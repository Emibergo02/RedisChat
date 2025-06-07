package dev.unnm3d.redischat.chat.listeners;

import dev.unnm3d.redischat.RedisChat;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@Getter
public enum ChatListenerWithPriority {
    LOWEST(new ChatListener() {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onChat(AsyncChatEvent event) {
            listenChat(event);
        }
    }),
    LOW(new ChatListener() {
        @EventHandler(priority = EventPriority.LOW)
        public void onChat(AsyncChatEvent event) {
            listenChat(event);
        }
    }),
    NORMAL(new ChatListener() {
        @EventHandler(priority = EventPriority.NORMAL)
        public void onChat(AsyncChatEvent event) {
            listenChat(event);
        }
    }),
    HIGH(new ChatListener() {
        @EventHandler(priority = EventPriority.HIGH)
        public void onChat(AsyncChatEvent event) {
            listenChat(event);
        }
    }),
    HIGHEST(new ChatListener() {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onChat(AsyncChatEvent event) {
            listenChat(event);
        }
    }),
    MONITOR(new ChatListener() {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onChat(AsyncChatEvent event) {
            listenChat(event);
        }
    });


    private final Listener listener;

    ChatListenerWithPriority(Listener listener) {
        this.listener = listener;
    }


    private abstract static class ChatListener implements Listener {
        private final RedisChat plugin = RedisChat.getInstance();

        public void listenChat(AsyncChatEvent event) {
            if (event.isCancelled()) return;
            event.setCancelled(true);
            plugin.getChannelManager().outgoingMessage(event.getPlayer(), MiniMessage.miniMessage().serialize(event.message())
                    .replace("\\<", "<"));
        }
    }
}
