package dev.unnm3d.redischat.api;

import org.bukkit.command.CommandSender;

public interface VanishIntegration {

    /**
     * Check if a player is vanished
     *
     * @param playerName The playerName
     * @param viewer     The player who is viewing the possible vanished player
     * @return true if the viewer can see the player
     */
    default boolean canSee(CommandSender viewer, String playerName) {

        return true;
    }

}
