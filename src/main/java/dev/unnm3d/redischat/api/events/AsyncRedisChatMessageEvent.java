package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.chat.objects.NewChannel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AsyncRedisChatMessageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final CommandSender sender;
    @Setter
    @Getter
    private NewChannel channel;
    @Getter
    private final Component format;
    @Getter
    private final Component content;
    private boolean cancelled;

    /**
     * Creates a RedisChatMessageEvent
     *
     * @param sender  The sender of the message
     * @param channel The channel of the message
     * @param format  The format of the message
     * @param content The message content
     */
    public AsyncRedisChatMessageEvent(CommandSender sender, NewChannel channel, Component format, Component content) {
        super(true);
        this.sender = sender;
        this.channel = channel;
        this.format = format;
        this.content = content;
        this.cancelled = false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
