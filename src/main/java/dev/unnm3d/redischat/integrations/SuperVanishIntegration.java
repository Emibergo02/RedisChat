package dev.unnm3d.redischat.integrations;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.command.CommandSender;

public class SuperVanishIntegration extends PremiumVanishIntegration {

    public SuperVanishIntegration(RedisChat plugin) {
        super(plugin);
    }

    @Override
    public boolean canSee(CommandSender viewer, String playerName) {
        if (viewer.hasPermission("sv.see")) return true;
        return !vanishedPlayers.contains(playerName);
    }
}
