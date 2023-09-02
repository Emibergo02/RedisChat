package dev.unnm3d.redischat.api;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public interface VanishIntegration {

    /**
     * Check if a player is vanished
     *
     * @param player The player to check
     * @return true if the player is vanished, false otherwise
     */
    default boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

}
