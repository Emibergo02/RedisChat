package dev.unnm3d.kalyachat.commands;

import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.Permission;
import dev.unnm3d.kalyachat.chat.TextParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpyChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) return false;
        net.milkbowl.vault.permission.Permission provider = KalyaChat.getInstance().getPermissionProvider();
        if (provider == null) {
            sender.sendMessage("§cVault not found, feature is not available");
            return false;
        }
        if (!sender.hasPermission(Permission.KALYA_CHAT_SPYCHAT.getPermission())){
            KalyaChat.getInstance().getPermissionProvider().playerAdd(null, (Player) sender, Permission.KALYA_CHAT_SPYCHAT.getPermission());
            sender.sendMessage(TextParser.parse(KalyaChat.config.spychat_enabled));
        }else{
            KalyaChat.getInstance().getPermissionProvider().playerRemove(null,(Player) sender, Permission.KALYA_CHAT_SPYCHAT.getPermission());
            sender.sendMessage(TextParser.parse(KalyaChat.config.spychat_disabled));
        }

        return false;
    }
}
