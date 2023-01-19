package dev.unnm3d.redischat.invshare;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InvShare implements CommandExecutor {
    private final RedisChat plugin;

    public InvShare(RedisChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player p)) return true;

        if (strings.length == 0) return true;
        String[] splitted = strings[0].split("-");
        if (splitted.length == 1) return true;

        Bukkit.getScheduler().runTaskAsynchronously(RedisChat.getInstance(), () -> {
            String playername = splitted[0];
            InventoryType type = InventoryType.valueOf(splitted[1].toUpperCase());
            switch (type) {
                case ITEM -> plugin.getRedisDataManager().getPlayerItem(playername)
                        .thenAccept(item ->
                                new InvGUI(p,
                                        plugin.config.item_title.replace("%player%", playername),
                                        item));

                case INVENTORY -> plugin.getRedisDataManager().getPlayerInventory(playername)
                        .thenAccept(inventoryContents ->
                                new InvGUI(p,
                                        plugin.config.inv_title.replace("%player%", playername),
                                        45,
                                        inventoryContents));
                case ENDERCHEST -> plugin.getRedisDataManager().getPlayerEnderchest(playername)
                        .thenAccept(ecContents ->
                                new InvGUI(p,
                                        plugin.config.ec_title.replace("%player%", playername),
                                        27,
                                        ecContents));
            }
        });
        return true;
    }

    public enum InventoryType {
        INVENTORY,
        ENDERCHEST,
        ITEM
    }

}
