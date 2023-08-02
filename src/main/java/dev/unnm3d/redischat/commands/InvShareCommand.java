package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class InvShareCommand implements CommandExecutor {

    private final RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player p)) return true;

        if (strings.length == 0) return true;
        String[] splitted = strings[0].split("-");
        if (splitted.length == 1) return true;


        String playername = splitted[0];
        InventoryType type = InventoryType.valueOf(splitted[1].toUpperCase());
        if (type == InventoryType.INVENTORY) {
            plugin.getDataManager().getPlayerInventory(playername)
                    .thenAccept(inventoryContents ->
                            plugin.getServer().getScheduler().runTask(plugin, () ->
                                    openInvShareGui(p,
                                            plugin.config.inv_title.replace("%player%", playername),
                                            inventoryContents
                                    )
                            ));
        }
        return true;
    }

    private void openInvShareGui(Player player, String title, ItemStack[] items) {
        Gui gui = Gui.empty(9, 5);
        gui.addItems(
                Arrays.stream(items)
                        .map(itemStack -> {
                            if (itemStack == null) return new ItemBuilder(Material.AIR);
                            return new ItemBuilder(itemStack);
                        })
                        .map(SimpleItem::new)
                        .toArray(Item[]::new)
        );
        Window.single().setTitle(title).setGui(gui).setCloseHandlers(List.of(() -> new BukkitRunnable() {
            @Override
            public void run() {
                player.updateInventory();
            }
        }.runTaskLater(plugin, 1))).open(player);
    }

    public enum InventoryType {
        INVENTORY,
    }

}
