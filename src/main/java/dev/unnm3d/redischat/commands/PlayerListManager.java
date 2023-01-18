package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlayerListManager implements TabCompleter {
    @Getter
    private final BukkitTask task;
    private static Set<String> playerList;

    public PlayerListManager(RedisChat plugin) {
        this.task = new BukkitRunnable() {

            @Override
            public void run() {
                plugin.getRedisDataManager().getPlayerList().thenAccept(redisList->{
                    if (redisList != null) playerList = redisList;
                });
            }
        }.runTaskTimerAsynchronously(plugin, 0, 160);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return null;

        if (command.getName().equalsIgnoreCase("msg") && args.length == 1) {
            return playerList.stream().filter(s -> s.startsWith(args[args.length - 1])).toList();
        }
        if (command.getName().equalsIgnoreCase("ignore")) {
            List<String> temp = new ArrayList<>(List.of("list", "all"));
            temp.addAll(playerList.stream().filter(s -> s.startsWith(args[args.length - 1])).toList());
            return temp;
        }
        return null;
    }

    public static Set<String> getPlayerList() {
        if (playerList == null) {
            return Set.of();
        }
        return playerList;
    }
}
