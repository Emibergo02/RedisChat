package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.channels.Channel;
import lombok.Getter;
import lombok.Setter;
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
    private Channel channel;
    @Getter
    private final String format;
    @Getter
    private final String message;
    private boolean cancelled;

    /**
     * Creates a RedisChatMessageEvent
     *
     * @param sender  The sender of the message
     * @param channel The channel of the message
     * @param format  The format of the message
     * @param message The message content
     */
    public AsyncRedisChatMessageEvent(CommandSender sender, Channel channel, String format, String message) {
        super(true);
        this.sender = sender;
        this.channel = channel;
        this.format = format;
        this.message = message;
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
