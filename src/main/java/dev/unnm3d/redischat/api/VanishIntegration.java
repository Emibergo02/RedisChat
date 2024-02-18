package dev.unnm3d.redischat.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface VanishIntegration {

    /**
     * Check if a player can see another player
     *
     * @param playerName The playerName
     * @param viewer     The player who is viewing the possible vanished player
     * @return true if the viewer can see the player
     */
    boolean canSee(CommandSender viewer, String playerName);

    /**
     * Check if a player is vanished
     * Default implementation checks for the PremiumVanish metadata
     *
     * @param player The player
     * @return true if the player is vanished
     */
    boolean isVanished(Player player);

}
