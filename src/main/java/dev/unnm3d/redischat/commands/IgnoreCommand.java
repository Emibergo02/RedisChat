package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

@AllArgsConstructor
public class IgnoreCommand implements CommandExecutor {
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
            plugin.getRedisDataManager().ignoringList(sender.getName())
                    .thenAccept(list -> {
                        if (list == null) return;
                        list.forEach(ignoreList::add);
                        plugin.messages.sendMessage(sender, plugin.getComponentProvider().parse(plugin.messages.ignoring_list.replace("%list%", ignoreList.toString())));
                    });

        }
        plugin.getRedisDataManager().toggleIgnoring(sender.getName(), args[0])
                .thenAccept(ignored -> {
                    if (ignored)
                        plugin.messages.sendMessage(sender, plugin.getComponentProvider().parse(plugin.messages.ignoring_player.replace("%player%", args[0])));
                    else
                        plugin.messages.sendMessage(sender, plugin.getComponentProvider().parse(plugin.messages.not_ignoring_player.replace("%player%", args[0])));
                });

        return true;
    }
}
