package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.chat.objects.ChatMessage;

public interface IDiscordHook {

    default void sendDiscordMessage(ChatMessage message) {

    }
}
