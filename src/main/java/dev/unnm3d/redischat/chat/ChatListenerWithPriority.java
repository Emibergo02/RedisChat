package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@Getter
public enum ChatListenerWithPriority {
    HIGH(new ChatListener(RedisChat.getInstance()) {
        @EventHandler(priority = EventPriority.HIGH)
        public void onChat(AsyncPlayerChatEvent event) {
            listenChat(event);
        }
    }),
    HIGHEST(new ChatListener(RedisChat.getInstance()) {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onChat(AsyncPlayerChatEvent event) {
            listenChat(event);
        }
    }),
    LOW(new ChatListener(RedisChat.getInstance()) {
        @EventHandler(priority = EventPriority.LOW)
        public void onChat(AsyncPlayerChatEvent event) {
            listenChat(event);
        }
    }),
    LOWEST(new ChatListener(RedisChat.getInstance()) {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onChat(AsyncPlayerChatEvent event) {
            listenChat(event);
        }
    }),
    MONITOR(new ChatListener(RedisChat.getInstance()) {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onChat(AsyncPlayerChatEvent event) {
            listenChat(event);
        }
    }),
    NORMAL(new ChatListener(RedisChat.getInstance()) {
        @EventHandler(priority = EventPriority.NORMAL)
        public void onChat(AsyncPlayerChatEvent event) {
            listenChat(event);
        }
    });


    private final Listener listener;

    ChatListenerWithPriority(Listener listener) {
        this.listener = listener;
    }
}
