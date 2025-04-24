package dev.unnm3d.redischat.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class InvShareCommand implements CommandExecutor {

    private final RedisChat plugin;

    @Override
    public boolean onCommand(final @NotNull CommandSender sender,
                             final @NotNull Command command,
                             final @NotNull String label,
                             final @NotNull String[] args) {

        if (!(sender instanceof final Player p)) return true;
        if (args.length == 0) return true;

        final String[] split = args[0].split("-");
        if (split.length != 2) return true;

        final String targetName = split[0];
        final InventoryType type;
        try {
            type = InventoryType.valueOf(split[1].toUpperCase());
        } catch (final IllegalArgumentException e) {
            return true;
        }

        switch (type) {
            case ITEM -> plugin.getDataManager()
                    .getPlayerItem(targetName)
                    .thenAccept(item -> {
                        ItemStack[] guiItems = new ItemStack[27];
                        guiItems[13] = item;
                        RedisChat.getScheduler().runTask(() ->
                                openRawGUI(
                                        p,
                                        plugin.config.item_title.replace("%player%", targetName),
                                        guiItems
                                )
                        );
                    });
            case INVENTORY -> plugin.getDataManager()
                    .getPlayerInventory(targetName)
                    .thenAccept(combinedArray ->
                            RedisChat.getScheduler().runTask(() ->
                                    openRawGUI(
                                            p,
                                            plugin.config.inv_title.replace("%player%", targetName),
                                            combinedArray
                                    )
                            )
                    );
            case ENDERCHEST -> plugin.getDataManager()
                    .getPlayerEnderchest(targetName)
                    .thenAccept(fetched -> {
                        RedisChat.getScheduler().runTask(() ->
                                openRawGUI(
                                        p,
                                        plugin.config.ec_title.replace("%player%", targetName),
                                        fetched
                                )
                        );
                    });
        }

        return true;
    }

    private void openRawGUI(final Player viewer,
                            final String title,
                            final ItemStack[] contents) {

        int rows = contents.length / 9 + (contents.length % 9 == 0 ? 0 : 1);
        rows = Math.max(1, Math.min(6, rows));

        final ItemStack[] gui = Arrays.copyOf(contents, rows * 9);
        for (int i = 0; i < gui.length; i++) {
            if (gui[i] == null) gui[i] = new ItemStack(Material.AIR);
        }

        final Gui inv = Gui.empty(9, rows);
        final List<SimpleItem> items = Arrays.stream(gui)
                .map(ItemBuilder::new)
                .map(SimpleItem::new)
                .toList();
        inv.addItems(items.toArray(SimpleItem[]::new));

        Window.single()
                .setTitle(title)
                .setGui(inv)
                .setCloseHandlers(List.of(() ->
                        new UniversalRunnable() {
                            @Override
                            public void run() {
                                viewer.updateInventory();
                            }
                        }.runTaskLater(plugin, 1)
                ))
                .open(viewer);
    }

    public enum InventoryType { INVENTORY, ENDERCHEST, ITEM }
}
