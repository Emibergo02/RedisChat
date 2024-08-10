package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.channels.gui.PlayerChannel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Setter
@Getter
public class ChannelGuiPopulateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private List<PlayerChannel> channelItems;

    public ChannelGuiPopulateEvent(List<PlayerChannel> channelItems) {
        super(true);
        this.channelItems = channelItems;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
