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

    /**
     * Event that is called before the channel GUI is populated
     * @param player The player that is opening the GUI
     * @param channelItems The list of channels that will be displayed in the GUI
     */
    public ChannelGuiPopulateEvent(Player player, List<PlayerChannel> channelItems) {
        super(true);
        this.player = player;
        this.channelItems = channelItems;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
