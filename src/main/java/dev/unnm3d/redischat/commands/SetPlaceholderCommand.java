package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
public class SetPlaceholderCommand implements CommandExecutor, TabCompleter {
    private final RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        if (args.length < 3) {
            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
            return true;
        }
        if (args[2].equals("delete")) {
            plugin.getPlaceholderManager().removePlayerPlaceholder(args[0], args[1]);
            plugin.messages.sendMessage(sender, plugin.messages.placeholder_deleted
                    .replace("%placeholder%", args[1])
                    .replace("%player%", args[0]));
            return true;
        }

        plugin.getPlaceholderManager().addPlayerPlaceholder(args[0], args[1], args[2]);

        plugin.messages.sendMessage(sender, plugin.messages.placeholder_set
                .replace("%player%", args[0])
                .replace("%placeholder%", args[1])
                .replace("%value%", args[2].replace("<","\\<"))
        );
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return plugin.getPlayerListManager().getPlayerList(sender)
                    .stream().filter(s ->
                            s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())
                    ).toList();
        } else if (args.length == 2 && args[args.length - 1].isEmpty()) {
            return List.of("chat_color", "placeholder");
        } else if (args.length == 3 && args[args.length - 1].isEmpty()) {
            return List.of("value");
        }
        return List.of();
    }
}
