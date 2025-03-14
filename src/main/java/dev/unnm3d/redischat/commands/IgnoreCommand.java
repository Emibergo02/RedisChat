package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class IgnoreCommand implements CommandExecutor, TabCompleter {
    private final RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        if (args.length == 0) {
            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
            return true;
        }

        if (label.equalsIgnoreCase("ignore") && plugin.getChannelManager().getMuteManager()
                .isWhitelistEnabledPlayer(sender.getName())) {
            plugin.messages.sendMessage(sender, plugin.messages.ignore_whitelist_enabled);
            return true;
        } else if (label.equalsIgnoreCase("allowmsg") && !plugin.getChannelManager().getMuteManager()
                .isWhitelistEnabledPlayer(sender.getName())) {
            plugin.messages.sendMessage(sender, plugin.messages.ignore_whitelist_disabled);
            return true;
        }

        if (args[0].equalsIgnoreCase(sender.getName())) {
            plugin.getComponentProvider().sendMessage(sender, plugin.getComponentProvider().parse(sender, plugin.messages.cannot_ignore_yourself,
                    true, false, false));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            final String stringList = String.join(", ", plugin.getChannelManager().getMuteManager().getIgnoreList(sender.getName()));
            plugin.getComponentProvider().sendMessage(sender,
                    plugin.getComponentProvider().parse(null,
                            plugin.messages.ignoring_list.replace("%list%", stringList), true, false, false));

            return true;
        }

        //Apply ignore
        final String message = plugin.getChannelManager().getMuteManager().toggleIgnorePlayer(sender.getName(), args[0]) ?
                plugin.messages.ignoring_player.replace("%player%", args[0]) :
                plugin.messages.not_ignoring_player.replace("%player%", args[0]);

        plugin.getComponentProvider().sendMessage(sender, plugin.getComponentProvider().parse(null, message, true, false, false));

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.IGNORE.getPermission())) return List.of();
        List<String> temp = new ArrayList<>();
        temp.add("list");
        if (!plugin.config.allPlayersString.isEmpty()) temp.add(plugin.config.allPlayersString);
        temp.addAll(
                plugin.getPlayerListManager().getPlayerList(sender)
                        .stream().filter(s ->
                                s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())
                        ).toList());
        return temp;
    }
}
