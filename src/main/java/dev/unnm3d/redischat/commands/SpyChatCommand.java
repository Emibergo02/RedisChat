package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Permission;
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

        if (!(sender instanceof Player)) return false;

        net.milkbowl.vault.permission.Permission provider = RedisChat.getInstance().getPermissionProvider();
        if (provider == null) {
            plugin.config.sendMessage(sender, "<red>Vault not found, feature is not available");
            return false;
        }
        if (!sender.hasPermission(Permission.REDIS_CHAT_SPYCHAT.getPermission())) {
            provider.playerAdd(null, (Player) sender, Permission.REDIS_CHAT_SPYCHAT.getPermission());
            plugin.config.sendMessage(sender, plugin.getComponentProvider().parse(plugin.config.spychat_enabled));
        } else {
            provider.playerRemove(null, (Player) sender, Permission.REDIS_CHAT_SPYCHAT.getPermission());
            plugin.config.sendMessage(sender, plugin.getComponentProvider().parse(plugin.config.spychat_disabled));
        }

        return false;
    }
}
