package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.objects.Channel;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public interface IDiscordHook {

    default void sendDiscordMessage(Channel channel, ChatMessage message) {

    }

    @NotNull
    default String getMessageJsonString(@NotNull RedisChat plugin) {
        try {
            return new String(plugin.getResource("webhook_embed.json").readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
