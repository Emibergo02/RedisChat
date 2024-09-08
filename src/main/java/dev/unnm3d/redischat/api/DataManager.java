package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public interface DataManager {


    void registerChannel(@NotNull Channel channel);

    void unregisterChannel(@NotNull String channelName);

    CompletionStage<String> getActivePlayerChannel(@NotNull String playerName);

    void setActivePlayerChannel(@NotNull String playerName, @Nullable String channelName);

    CompletionStage<List<Channel>> getChannels();

    CompletionStage<Optional<String>> getReplyName(@NotNull String requesterName);

    void setReplyName(@NotNull String nameReceiver, @NotNull String requesterName);

    CompletionStage<Map<String, String>> getPlayerPlaceholders(@NotNull String playerName);

    void setPlayerPlaceholders(@NotNull String playerName, @NotNull Map<String, String> placeholders);

    boolean isRateLimited(@NotNull String playerName, @NotNull Channel channel);

    CompletionStage<Boolean> isSpying(@NotNull String playerName);

    void setSpying(@NotNull String playerName, boolean spy);

    void addInventory(@NotNull String name, ItemStack[] inv);

    void addItem(@NotNull String name, ItemStack item);

    void addEnderchest(@NotNull String name, ItemStack[] inv);

    void clearInvShareCache();

    CompletionStage<ItemStack> getPlayerItem(@NotNull String playerName);

    CompletionStage<ItemStack[]> getPlayerInventory(@NotNull String playerName);

    CompletionStage<ItemStack[]> getPlayerEnderchest(@NotNull String playerName);

    CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName);

    CompletionStage<Boolean> setPlayerPrivateMail(@NotNull Mail mail);

    CompletionStage<Boolean> setPublicMail(@NotNull Mail mail);

    CompletableFuture<List<Mail>> getPublicMails(@NotNull String playerName);

    CompletionStage<Boolean> setMailRead(@NotNull String playerName, @NotNull Mail mail);

    CompletionStage<Boolean> deleteMail(@NotNull Mail mail);

    void setMutedEntities(@NotNull String playerName, @NotNull Set<String> mutedChannels);

    CompletionStage<Map<String, Set<String>>> getAllMutedEntities();

    CompletionStage<Set<String>> getWhitelistEnabledPlayers();

    void setWhitelistEnabledPlayer(@NotNull String playerName, boolean enabled);

    void sendChatMessage(@NotNull ChatMessage chatMessage);

    void publishPlayerList(@NotNull List<String> playerNames);

    void sendRejoin(@NotNull String playerName);

    void close();


    default String serialize(ItemStack... items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception exception) {
            exception.printStackTrace();
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
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ItemStack[0];
        }
    }

    default String serializePlayerPlaceholders(Map<String, String> placeholders) {
        return placeholders.entrySet().stream()
                .map(entry -> entry.getKey() + "§§§" + entry.getValue())
                .collect(Collectors.joining("§;"));
    }

    default Map<String, String> deserializePlayerPlaceholders(String source) {
        if (source == null || source.isEmpty()) return new HashMap<>();
        return Arrays.stream(source.split("§;"))
                .map(s -> s.split("§§§"))
                .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));
    }


}
