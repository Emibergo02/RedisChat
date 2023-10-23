package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook implements IDiscordHook {
    private final RedisChat plugin;


    public DiscordWebhook(RedisChat plugin){
        this.plugin = plugin;
    }

    @Override
    public void sendDiscordMessage(Channel channel, ChatMessageInfo message) {
        if (channel.getDiscordWebhook() == null || channel.getDiscordWebhook().isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                final HttpURLConnection webhookConnection = (HttpURLConnection) new URL(channel.getDiscordWebhook()).openConnection();
                webhookConnection.setRequestMethod("POST");
                webhookConnection.setDoOutput(true);

                final byte[] jsonMessage = getDiscordMessageJson(plugin, channel, message).getBytes(StandardCharsets.UTF_8);
                final int messageLength = jsonMessage.length;
                webhookConnection.setFixedLengthStreamingMode(messageLength);
                webhookConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                webhookConnection.connect();
                try (OutputStream messageOutputStream = webhookConnection.getOutputStream()) {
                    messageOutputStream.write(jsonMessage);
                }
            } catch (Throwable e) {
                plugin.getLogger().warning("Unable to send message to Discord webhook: " + e.getMessage());
            }
        });
    }
}
