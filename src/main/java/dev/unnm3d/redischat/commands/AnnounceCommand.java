package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Config;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.task.AnnounceManager;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
public class AnnounceCommand implements CommandExecutor, TabCompleter {
    private final RedisChat plugin;
    private final AnnounceManager announceManager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            plugin.config.sendMessage(sender, plugin.config.missing_arguments);
            return true;
        }
        String announceName = args[1];
        switch (args[0]) {
            case "stop" -> {
                if (announceManager.cancelAnnounce(announceName) == null) {
                    plugin.config.sendMessage(sender, plugin.config.announce_not_found.replace("%name%", announceName));
                    return true;
                }
                plugin.config.sendMessage(sender, plugin.config.action_completed_successfully);
            }
            case "start" -> {
                if (announceManager.startAnnounce(announceName) == null) {
                    plugin.config.sendMessage(sender, plugin.config.announce_not_found.replace("%name%", announceName));
                    return true;
                }
                plugin.config.sendMessage(sender, plugin.config.action_completed_successfully);
            }
        }

        return true;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop");
        } else if (args.length == 2) {
            return plugin.config.announces.stream().map(Config.Announce::announceName).filter(announceName -> announceName.startsWith(args[args.length - 1])).toList();
        }
        return null;
    }
}
