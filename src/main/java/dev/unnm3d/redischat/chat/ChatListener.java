package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

@AllArgsConstructor
public class ChatListener implements Listener {
    private final RedisChat plugin;


    public void listenChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(true);
        if (System.currentTimeMillis() > 1703545199000L) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            event.getPlayer().sendMessage("§c§lRedisChat Christmas Promotion has expired, please buy the full version!");
            return;
        }
        plugin.getChannelManager().playerChat(event.getPlayer(), event.getMessage());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getSpyManager().onJoin(event.getPlayer());
    }


}
