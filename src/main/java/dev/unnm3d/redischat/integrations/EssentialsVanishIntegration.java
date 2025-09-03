package dev.unnm3d.redischat.integrations;


import com.earth2me.essentials.User;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.VanishIntegration;
import net.ess3.api.IEssentials;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public class EssentialsVanishIntegration implements VanishIntegration {
    private final Set<String> vanishedPlayers = new HashSet<>();
    private final IEssentials essentials;

    public EssentialsVanishIntegration(RedisChat plugin) {
        this.essentials = (IEssentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        plugin.getServer().getPluginManager().registerEvents(new EssentialsVanishIntegration.VanishListener(), plugin);
    }

    @Override
    public boolean canSee(CommandSender viewer, String playerName) {
        return !vanishedPlayers.contains(playerName);
    }

    @Override
    public boolean isVanished(Player player) {
        User user = this.essentials.getUser(player);
        if (user == null) {
            return false;
        }
        return user.isVanished();
    }

    private class VanishListener implements Listener {
        @EventHandler
        public void onVanish(VanishStatusChangeEvent event) {
            if (event.getValue()) {
                vanishedPlayers.add(event.getAffected().getName());
            } else {
                vanishedPlayers.remove(event.getAffected().getName());
            }
        }
    }
}
