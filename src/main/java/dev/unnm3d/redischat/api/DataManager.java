package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.chat.ChatMessageInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface DataManager {

    Optional<String> getReplyName(@NotNull String requesterName);

    void setReplyName(@NotNull String nameReceiver, @NotNull String requesterName);

    boolean isRateLimited(@NotNull String playerName);

    void setRateLimit(@NotNull String playerName, int seconds);

    CompletionStage<Boolean> toggleIgnoring(@NotNull String playerName, @NotNull String ignoringName);

    CompletionStage<Boolean> isIgnoring(@NotNull String playerName, @NotNull String ignoringName);

    CompletionStage<List<String>> ignoringList(@NotNull String playerName);

    void addInventory(@NotNull String name, ItemStack[] inv);

    void addItem(@NotNull String name, ItemStack item);

    void addEnderchest(@NotNull String name, ItemStack[] inv);

    CompletionStage<ItemStack[]> getPlayerInventory(@NotNull String playerName);


    void sendChatMessage(@NotNull ChatMessageInfo chatMessage);

    void publishPlayerList(@NotNull List<String> playerNames);

    void close();


    default String serialize(ItemStack... items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception ignored) {
            return "";
        }
    }

    default ItemStack[] deserialize(String source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++)
                items[i] = (ItemStack) dataInput.readObject();

            return items;
        } catch (Exception ignored) {
            return new ItemStack[0];
        }
    }


}
