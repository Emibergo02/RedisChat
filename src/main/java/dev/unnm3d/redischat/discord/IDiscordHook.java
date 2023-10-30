package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public interface IDiscordHook {

    default void sendDiscordMessage(Channel channel, ChatMessageInfo message) {

    }

    default String getDiscordMessageJson(@NotNull RedisChat plugin, @NotNull Channel channel, @NotNull ChatMessageInfo message) {
        return getMessageJsonString(plugin)
                .replace("{SENDER_CHANNEL}", channel.getName())
                .replace("{CURRENT_TIMESTAMP}", ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .replace("{SENDER_USERNAME}", message.getSenderName())
                .replace("{CHAT_MESSAGE}", MiniMessage.miniMessage().stripTags(message.getFormatting())
                        .replace("%message%", MiniMessage.miniMessage().stripTags(message.getMessage())));
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
