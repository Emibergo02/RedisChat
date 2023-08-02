package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.chat.ChatMessageInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface DataManager {

    Optional<String> getReplyName(String requesterName);

    void setReplyName(String nameReceiver, String requesterName);

    boolean isRateLimited(String playerName);

    void setRateLimit(String playerName, int seconds);

    CompletionStage<Boolean> toggleIgnoring(String playerName, String ignoringName);

    CompletionStage<Boolean> isIgnoring(String playerName, String ignoringName);

    CompletionStage<List<String>> ignoringList(String playerName);

    void addInventory(String name, ItemStack[] inv);

    void addItem(String name, ItemStack item);

    void addEnderchest(String name, ItemStack[] inv);

    CompletionStage<ItemStack[]> getPlayerInventory(String playerName);


    void sendChatMessage(ChatMessageInfo chatMessage);

    void publishPlayerList(List<String> playerNames);

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
