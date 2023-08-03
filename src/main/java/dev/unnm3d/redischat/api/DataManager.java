package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.mail.Mail;
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

    CompletionStage<Boolean> isSpying(@NotNull String playerName);

    void setSpying(@NotNull String playerName, boolean spy);

    CompletionStage<Boolean> toggleIgnoring(@NotNull String playerName, @NotNull String ignoringName);

    CompletionStage<Boolean> isIgnoring(@NotNull String playerName, @NotNull String ignoringName);

    CompletionStage<List<String>> ignoringList(@NotNull String playerName);

    void addInventory(@NotNull String name, ItemStack[] inv);

    void addItem(@NotNull String name, ItemStack item);

    void addEnderchest(@NotNull String name, ItemStack[] inv);

    CompletionStage<ItemStack> getPlayerItem(@NotNull String playerName);

    CompletionStage<ItemStack[]> getPlayerInventory(@NotNull String playerName);

    CompletionStage<ItemStack[]> getPlayerEnderchest(@NotNull String playerName);

    CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName);

    CompletionStage<Boolean> setPlayerPrivateMail(@NotNull Mail mail);

    CompletionStage<Boolean> setPublicMail(@NotNull Mail mail);

    CompletionStage<List<Mail>> getPublicMails();

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

    default List<Mail> deserializeMails(Map<String, String> timestampMail) {
        return timestampMail.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(Double.parseDouble(entry.getKey()), entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Mail(entry.getKey(), entry.getValue())).toList();
    }


}
