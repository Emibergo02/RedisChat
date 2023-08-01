package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletionStage;

public interface DataManager {

    Optional<String> getReplyName(String requesterName);

    void setReplyName(String nameReceiver, String requesterName);

    boolean isRateLimited(String playerName);

    void setRateLimit(String playerName, int seconds);

    CompletionStage<Boolean> isSpying(String playerName);

    void setSpying(String playerName, boolean spy);

    CompletionStage<Boolean> toggleIgnoring(String playerName, String ignoringName);

    CompletionStage<Boolean> isIgnoring(String playerName, String ignoringName);

    CompletionStage<List<String>> ignoringList(String playerName);

    void addInventory(String name, ItemStack[] inv);

    void addItem(String name, ItemStack item);

    void addEnderchest(String name, ItemStack[] inv);

    CompletionStage<ItemStack> getPlayerItem(String playerName);

    CompletionStage<ItemStack[]> getPlayerInventory(String playerName);

    CompletionStage<ItemStack[]> getPlayerEnderchest(String playerName);

    CompletionStage<List<Mail>> getPlayerPrivateMail(String playerName);

    CompletionStage<Boolean> setPlayerPrivateMail(Mail mail);

    CompletionStage<Boolean> setPublicMail(Mail mail);

    CompletionStage<List<Mail>> getPublicMails();

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

    default List<Mail> deserializeMails(Map<String, String> timestampMail) {
        return timestampMail.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(Double.parseDouble(entry.getKey()), entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Mail(entry.getKey(), entry.getValue())).toList();
    }


}
