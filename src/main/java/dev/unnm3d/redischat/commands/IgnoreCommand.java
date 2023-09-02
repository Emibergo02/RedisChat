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
import java.util.StringJoiner;

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

        if (args[0].equalsIgnoreCase("list")) {
            final StringJoiner ignoreList = new StringJoiner(", ");
            plugin.getDataManager().ignoringList(sender.getName())
                    .thenAccept(list -> {
                        if (list == null) return;
                        list.forEach(ignoreList::add);
                        plugin.messages.sendMessage(sender, plugin.getComponentProvider().parse(plugin.messages.ignoring_list.replace("%list%", ignoreList.toString())));
                    });
            return true;
        }
        plugin.getDataManager().toggleIgnoring(sender.getName(), args[0])
                .thenAccept(ignored -> {
                    if (ignored)
                        plugin.messages.sendMessage(sender, plugin.getComponentProvider().parse(plugin.messages.ignoring_player.replace("%player%", args[0])));
                    else
                        plugin.messages.sendMessage(sender, plugin.getComponentProvider().parse(plugin.messages.not_ignoring_player.replace("%player%", args[0])));
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.IGNORE.getPermission())) return List.of();
        List<String> temp = new ArrayList<>(List.of("list", "all"));
        temp.addAll(plugin.getPlayerListManager().getPlayerList().stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1])).toList());
        return temp;
    }
}
