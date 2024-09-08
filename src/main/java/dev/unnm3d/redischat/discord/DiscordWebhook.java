package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import net.kyori.adventure.text.minimessage.MiniMessage;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook implements IDiscordHook {
    private final RedisChat plugin;


    public DiscordWebhook(RedisChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendDiscordMessage(ChatMessage message) {
        if (message.getReceiver().isChannel()) {
            final Optional<Channel> channel = plugin.getChannelManager().getChannel(message.getReceiver().getName(), null);
            if(channel.isEmpty()) {
                plugin.getLogger().warning("Channel not found: " + message.getReceiver().getName());
                return;
            }

            if (channel.get().getDiscordWebhook() == null || channel.get().getDiscordWebhook().isEmpty() || message.getSender().isDiscord())
                return;

            CompletableFuture.runAsync(() -> {
                try {
                    final HttpsURLConnection connection = (HttpsURLConnection) new URL(channel.get().getDiscordWebhook()).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11");
                    connection.setDoOutput(true);
                    try (final OutputStream outputStream = connection.getOutputStream()) {
                        final String avatarUrl = !message.getSender().isServer() ?
                                plugin.getServer().getOnlinePlayers().stream()
                                        .filter(p -> p.getName().equals(message.getSender().getName()))
                                        .findFirst()
                                        .map(p -> "https://minotar.net/helm/" + p.getUniqueId().toString().replace("-", "") + ".png")
                                        .orElse("") :
                                "";

                        outputStream.write((String.format("""
                                        {"username": "%s",
                                        "avatar_url": "%s",
                                        "content": "%s",
                                        "embeds": []
                                        }
                                        """,
                                message.getSender().isServer() ? "Server" : message.getSender().getName(),
                                avatarUrl,
                                MiniMessage.miniMessage().stripTags(message.getContent())
                        ).getBytes(StandardCharsets.UTF_8)));
                    }
                    connection.getInputStream();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }, plugin.getExecutorService()).exceptionally((e) -> {
                plugin.getLogger().warning("Unable to send message to Discord channel " + channel.get().getName() + ": " + e.getMessage());
                return null;
            });
        }
    }
}
