package dev.unnm3d.redischat.moderation;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpyManager {
    private final RedisChat plugin;
    private final ConcurrentMap<String, Optional<Void>> isSpyingNames;

    public SpyManager(RedisChat plugin) {
        this.plugin = plugin;
        this.isSpyingNames = new ConcurrentHashMap<>();
    }

    public void onJoin(Player player) {
        plugin.getRedisDataManager().isSpying(player.getName()).thenAccept(isSpying -> {
            if (isSpying) {
                this.isSpyingNames.put(player.getName(), Optional.empty());
            } else {
                this.isSpyingNames.remove(player.getName());
            }
        });
    }

    public boolean toggleSpying(String playerName) {
        if (isSpyingNames.containsKey(playerName)) {
            isSpyingNames.remove(playerName);
            plugin.getRedisDataManager().setSpying(playerName, false);
            return false;
        } else {
            isSpyingNames.put(playerName, Optional.empty());
            plugin.getRedisDataManager().setSpying(playerName, true);
            return true;
        }
    }

    public boolean isSpying(String playerName) {
        return isSpyingNames.containsKey(playerName);
    }
}
