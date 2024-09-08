package dev.unnm3d.redischat.moderation;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class SpyChatCommand implements CommandExecutor {
    private final RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0 && !(sender instanceof Player)) {
            plugin.messages.sendMessage(sender, plugin.messages.player_not_online.replace("%player%", "CONSOLE"));
            return true;
        }

        String playerName = args.length == 0 ? sender.getName() : args[0];
        if (args.length > 0 && !sender.hasPermission(Permissions.SPY_OTHERS.getPermission())) {
            plugin.messages.sendMessage(sender, plugin.messages.noPermission);
            return true;
        }

        if (plugin.getSpyManager().toggleSpying(playerName)) {
            plugin.getComponentProvider().sendMessage(sender, plugin.getComponentProvider().parse(null, plugin.messages.spychat_enabled.replace("%player%", playerName), true, false, false));
        } else {
            plugin.getComponentProvider().sendMessage(sender, plugin.getComponentProvider().parse(null, plugin.messages.spychat_disabled.replace("%player%", playerName), true, false, false));
        }

        return true;
    }
}
