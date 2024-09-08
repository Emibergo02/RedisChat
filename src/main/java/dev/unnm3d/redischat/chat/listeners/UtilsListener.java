package dev.unnm3d.redischat.chat.listeners;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AllArgsConstructor
public class UtilsListener implements Listener {
    private final RedisChat plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinSpy(PlayerJoinEvent event) {
        plugin.getSpyManager().onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinActiveChannel(PlayerJoinEvent event) {
        plugin.getDataManager().getActivePlayerChannel(event.getPlayer().getName())
                .thenAccept(channelName ->
                        plugin.getChannelManager().updateActiveChannel(event.getPlayer().getName(), channelName));
    }
}
