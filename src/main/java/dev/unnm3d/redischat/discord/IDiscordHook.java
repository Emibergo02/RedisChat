package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.api.objects.ChatMessage;

public interface IDiscordHook {

    default void sendDiscordMessage(ChatMessage message) {

    }
}
