package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.configs.Config;
import dev.unnm3d.redischat.redis.ChatPacket;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

@AllArgsConstructor
public class ChatListener implements Listener {
    private final RedisChat plugin;

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(true);

        long init = System.currentTimeMillis();
        int totalElapsed = 0;

        List<Config.ChatFormat> chatFormatList = plugin.config.getChatFormats(event.getPlayer());
        if (chatFormatList.isEmpty()) return;
        if (!event.getPlayer().hasPermission(Permission.REDIS_CHAT_BYPASS_RATE_LIMIT.getPermission()))
            if (plugin.getRedisDataManager().isRateLimited(event.getPlayer().getName())) {
                plugin.messages.sendMessage(event.getPlayer(), plugin.messages.rate_limited);
                return;
            }

        totalElapsed += debug("Rate limit timing: %time%ms", init);
        init = System.currentTimeMillis();

        Component formatted = plugin.getComponentProvider().parse(event.getPlayer(), chatFormatList.get(0).format());//Parse format without %message%
        //Check for minimessage tags permission
        String message = event.getMessage();
        boolean parsePlaceholders = true;
        if (!event.getPlayer().hasPermission(Permission.REDIS_CHAT_USE_FORMATTING.getPermission())) {
            message = plugin.getComponentProvider().purgeTags(message);//Remove all minimessage tags
            parsePlaceholders = false;
        }
        if (message.trim().equals("")) return;//Check if message is empty after purging tags
        if (plugin.getComponentProvider().antiCaps(message)) {
            plugin.messages.sendMessage(event.getPlayer(), plugin.messages.caps);
            message = message.toLowerCase();
        }
        message = plugin.getComponentProvider().sanitize(message);

        totalElapsed += debug("Format timing: %time%ms", init);
        init = System.currentTimeMillis();

        //Check inv update
        if (message.contains("<inv>")) {
            plugin.getRedisDataManager().addInventory(event.getPlayer().getName(), event.getPlayer().getInventory().getContents());
        }
        if (message.contains("<item>")) {
            plugin.getRedisDataManager().addItem(event.getPlayer().getName(), event.getPlayer().getInventory().getItemInMainHand());
        }
        if (message.contains("<ec>")) {
            plugin.getRedisDataManager().addEnderchest(event.getPlayer().getName(), event.getPlayer().getEnderChest().getContents());
        }
        totalElapsed += debug("Inv upload timing: %time%ms", init);
        init = System.currentTimeMillis();

        //Parse into minimessage (placeholders, tags and mentions)
        Component toBeReplaced = plugin.getComponentProvider().parse(event.getPlayer(), message, parsePlaceholders, plugin.getComponentProvider().getCustomTagResolver(event.getPlayer(), chatFormatList.get(0)));

        //Put message into format
        formatted = formatted.replaceText(
                builder -> builder.match("%message%").replacement(toBeReplaced)
        );
        totalElapsed += debug("Message parsing timing: %time%ms", init);

        // Send to other servers
        plugin.getRedisDataManager().sendObjectPacket(new ChatPacket(event.getPlayer().getName(), MiniMessage.miniMessage().serialize(formatted)));
        plugin.getRedisDataManager().setRateLimit(event.getPlayer().getName(), plugin.config.rate_limit_time_seconds);

        if (plugin.config.debug) {
            plugin.getLogger().info("Total chat event timing: " + totalElapsed + "ms");
        }
    }

    private int debug(String debugmsg, long init) {
        int time = (int) (System.currentTimeMillis() - init);
        if (plugin.config.debug)
            plugin.getLogger().info(debugmsg.replace("%time%", String.valueOf(time)));
        return time;
    }


    public void onSenderPrivateChat(CommandSender sender, Component formatted) {
        plugin.getComponentProvider().sendMessage(sender, formatted);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getSpyManager().onJoin(event.getPlayer());
    }


}
