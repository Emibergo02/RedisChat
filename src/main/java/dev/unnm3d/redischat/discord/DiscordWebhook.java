package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook implements IDiscordHook {
    private final RedisChat plugin;


    public DiscordWebhook(RedisChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendDiscordMessage(Channel channel, ChatMessageInfo message) {
        if (channel.getDiscordWebhook() == null || channel.getDiscordWebhook().isEmpty() || message.getSender().isDiscord())
            return;

        CompletableFuture.runAsync(() -> {
            try {
                final HttpsURLConnection connection = (HttpsURLConnection) new URL(channel.getDiscordWebhook()).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11");
                connection.setDoOutput(true);
                try (final OutputStream outputStream = connection.getOutputStream()) {
                    final UUID uuid = !message.getSender().isServer() ?
                            Bukkit.getOfflinePlayer(message.getSender().getName()).getUniqueId() :
                            null;
                    outputStream.write((String.format("""
                                    {"username": "%s",
                                    "avatar_url": "%s",
                                    "content": "%s",
                                    "embeds": []
                                    }
                                    """,
                            message.getSender().isServer() ? "Server" : message.getSender().getName(),
                            uuid == null ? "" : "https://crafatar.com/avatars/" + uuid + "?size=64&default=MHF_Steve&overlay",
                            MiniMessage.miniMessage().stripTags(message.getMessage())
                    ).getBytes(StandardCharsets.UTF_8)));
                }
                connection.getInputStream();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, plugin.getExecutorService()).exceptionally((e) -> {
            plugin.getLogger().warning("Unable to send message to Discord channel " + channel.getName() + ": " + e.getMessage());
            return null;
        });

    }
}
