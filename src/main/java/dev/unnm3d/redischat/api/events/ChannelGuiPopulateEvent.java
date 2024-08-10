package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.channels.gui.PlayerChannel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class ChannelGuiPopulateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    @Setter
    private List<PlayerChannel> channelItems;

    public ChannelGuiPopulateEvent(Player player, List<PlayerChannel> channelItems) {
        super(true);
        this.player = player;
        this.channelItems = channelItems;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
