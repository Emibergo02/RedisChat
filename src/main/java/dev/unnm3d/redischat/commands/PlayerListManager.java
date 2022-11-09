package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
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
    private final BukkitTask task;
    private static Set<String> playerList;

    public PlayerListManager() {
        RedisChat kc = RedisChat.getInstance();
        this.task = new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    Set<String> redisList = kc.getRedisDataManager().getPlayerList();
                    if (redisList != null) playerList = redisList;
                } catch (Exception ignored) {
                }
            }
        }.runTaskTimerAsynchronously(kc, 0, 40);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return null;

        if (command.getName().equalsIgnoreCase("msg") || args.length != 1) {
            return null;
        }
        if (command.getName().equalsIgnoreCase("ignore")) {
            List<String> temp = new ArrayList<>(List.of("list", "all"));
            temp.addAll(playerList.stream().filter(s -> s.startsWith(args[args.length - 1])).toList());
            return temp;
        }
        return playerList.stream().filter(s -> s.startsWith(args[args.length - 1])).toList();
    }

    public static Set<String> getPlayerList() {
        if (playerList == null) {
            return Set.of();
        }
        return playerList;
    }
}
