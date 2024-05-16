package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderManager implements Listener {

    private final RedisChat plugin;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> playerPlaceholders;

    public PlaceholderManager(RedisChat plugin) {
        this.plugin = plugin;
        this.playerPlaceholders = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        updatePlayerPlaceholderCache(event.getPlayer().getName());
    }

    public void updatePlayerPlaceholderCache(String playerName) {
        this.plugin.getDataManager().getPlayerPlaceholders(playerName)
                .thenAccept(redisPlaceholders -> {
                    if (redisPlaceholders == null) return;
                    playerPlaceholders.put(playerName, new ConcurrentHashMap<>(redisPlaceholders));
                });
    }

    public void updatePlayerPlaceholders(String serializedPlaceholders) {
        final String playerName = serializedPlaceholders.substring(0,serializedPlaceholders.indexOf("§;"));
        final String placeholders = serializedPlaceholders.substring(serializedPlaceholders.indexOf("§;") + 2);
        playerPlaceholders.put(playerName, new ConcurrentHashMap<>(plugin.getDataManager().deserializePlayerPlaceholders(placeholders)));
    }

    public void addPlayerPlaceholder(String playerName, String placeholder, String value) {
        playerPlaceholders.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>()).put(placeholder, value);
        this.plugin.getDataManager().setPlayerPlaceholders(playerName, playerPlaceholders.get(playerName));
    }

    public void removePlayerPlaceholder(String playerName, String placeholder) {
        if (!playerPlaceholders.containsKey(playerName)) return;
        if (playerPlaceholders.get(playerName).remove(placeholder) == null) return;
        this.plugin.getDataManager().setPlayerPlaceholders(playerName, playerPlaceholders.get(playerName));
    }

    public String getPlaceholder(@NotNull String playerName, @NotNull String placeholder) {
        return playerPlaceholders.getOrDefault(playerName, new ConcurrentHashMap<>()).getOrDefault(placeholder, "");
    }
}
