package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public interface Filter {

    String getName();

    Direction getDirection();

    FilterResult filter(Player receiver, @NotNull NewChatMessage message, NewChatMessage... previousMessages);

    enum Direction {
        INCOMING,
        OUTGOING
    }
}
