package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.channels.Channel;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RedisChatMessageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final CommandSender sender;
    private Channel channel;
    private final String format;
    private final String message;
    private boolean cancelled;

    /**
     * Creates a RedisChatMessageEvent
     * @param sender The sender of the message
     * @param channel The channel of the message
     * @param format The format of the message
     * @param message The message content
     */
    public RedisChatMessageEvent(CommandSender sender, Channel channel, String format, String message) {
        this.sender = sender;
        this.channel = channel;
        this.format = format;
        this.message = message;
        this.cancelled = false;
    }

    public CommandSender getSender() {
        return sender;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getFormat() {
        return format;
    }

    public String getMessage() {
        return message;
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
