package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;


@AllArgsConstructor
public class IgnoreWhitelistCommand {
    private final RedisChat plugin;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("ignorewhitelist")
                .withPermission(Permissions.IGNORE_WHITELIST.getPermission())
                .withOptionalArguments(new PlayerArgument("player"))
                .executesPlayer((sender, args) -> {
                    //Get the target player name or sender name
                    final String targetName = args.getOptional("player")
                            .map(o -> ((Player) o).getName())
                            .orElseGet(sender::getName);
                    if (!sender.hasPermission(Permissions.IGNORE_WHITELIST.getPermission() + ".other") &&
                            !sender.getName().equals(targetName)) {
                        plugin.messages.sendMessage(sender, plugin.messages.noPermission);
                        return;
                    }
                    if (plugin.getChannelManager().getMuteManager().isWhitelistEnabledPlayer(targetName)) {
                        plugin.getChannelManager().getMuteManager().setWhitelistEnabledPlayer(targetName, false);
                        plugin.messages.sendMessage(sender, plugin.messages.ignore_whitelist_disabled);
                    } else {
                        plugin.getChannelManager().getMuteManager().setWhitelistEnabledPlayer(targetName, true);
                        plugin.messages.sendMessage(sender, plugin.messages.ignore_whitelist_enabled);
                    }
                });
    }
}
