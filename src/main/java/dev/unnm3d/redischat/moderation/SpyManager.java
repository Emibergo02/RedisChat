package dev.unnm3d.redischat.moderation;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpyManager {
    private final RedisChat plugin;
    private final Set<String> isSpyingNames;

    public SpyManager(RedisChat plugin) {
        this.plugin = plugin;
        this.isSpyingNames = ConcurrentHashMap.newKeySet();
    }

    public void onJoin(Player player) {
        plugin.getDataManager().isSpying(player.getName()).thenAccept(isSpying -> {
            if (isSpying) {
                this.isSpyingNames.add(player.getName());
            } else {
                this.isSpyingNames.remove(player.getName());
            }
        });
    }

    public boolean toggleSpying(String playerName) {
        if (isSpyingNames.contains(playerName)) {
            isSpyingNames.remove(playerName);
            plugin.getDataManager().setSpying(playerName, false);
            return false;
        } else {
            isSpyingNames.add(playerName);
            plugin.getDataManager().setSpying(playerName, true);
            return true;
        }
    }

    public boolean isSpying(String playerName) {
        return isSpyingNames.contains(playerName);
    }
}
