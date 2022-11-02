package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.chat.TextParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpyChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) return false;
        net.milkbowl.vault.permission.Permission provider = RedisChat.getInstance().getPermissionProvider();
        if (provider == null) {
            sender.sendMessage("§cVault not found, feature is not available");
            return false;
        }
        if (!sender.hasPermission(Permission.REDIS_CHAT_SPYCHAT.getPermission())){
            RedisChat.getInstance().getPermissionProvider().playerAdd(null, (Player) sender, Permission.REDIS_CHAT_SPYCHAT.getPermission());
            sender.sendMessage(TextParser.parse(RedisChat.config.spychat_enabled));
        }else{
            RedisChat.getInstance().getPermissionProvider().playerRemove(null,(Player) sender, Permission.REDIS_CHAT_SPYCHAT.getPermission());
            sender.sendMessage(TextParser.parse(RedisChat.config.spychat_disabled));
        }

        return false;
    }
}
