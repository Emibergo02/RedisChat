package dev.unnm3d.redischat.integrations;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.api.vanish.VanishAPI;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.VanishIntegration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public class PremiumVanishIntegration implements VanishIntegration {
    protected final Set<String> vanishedPlayers = new HashSet<>();

    public PremiumVanishIntegration(RedisChat plugin) {
        plugin.getServer().getPluginManager().registerEvents(new VanishListener(), plugin);
    }

    @Override
    public boolean canSee(CommandSender viewer, String playerName) {
        if (viewer.hasPermission("pv.see")) return true;
        return !vanishedPlayers.contains(playerName);
    }

    @Override
    public boolean isVanished(Player player) {
        return VanishAPI.isInvisible(player);
    }

    private class VanishListener implements Listener {
        @EventHandler
        public void onVanish(PlayerVanishStateChangeEvent event) {
            if (event.isVanishing()) {
                vanishedPlayers.add(event.getName());
            } else {
                vanishedPlayers.remove(event.getName());
            }
        }
    }
}
