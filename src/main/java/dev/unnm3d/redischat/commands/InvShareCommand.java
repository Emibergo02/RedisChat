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

    private final RedisChat plugin;  // schon final

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
                        // item ist effektiv final
                        RedisChat.getScheduler().runTask(() ->
                                openRawGUI(
                                        p,
                                        plugin.config.item_title.replace("%player%", targetName),
                                        new ItemStack[]{ item }
                                )
                        );
                    });

            case INVENTORY -> plugin.getDataManager()
                    .getPlayerInventory(targetName)
                    .thenAccept(rawInv -> {
                        final ItemStack[] guiItems = new ItemStack[45];
                        for (int i = 0; i < 9; i++) {
                            int armorIdx = 36 + i;
                            ItemStack armorOrAir = (armorIdx < rawInv.length && rawInv[armorIdx] != null)
                                    ? rawInv[armorIdx]
                                    : new ItemStack(Material.AIR);
                            guiItems[i] = armorOrAir;
                        }
                        for (int slot = 9; slot < 36; slot++) {
                            ItemStack content = (slot < rawInv.length && rawInv[slot] != null)
                                    ? rawInv[slot]
                                    : new ItemStack(Material.AIR);
                            guiItems[slot] = content;
                        }
                        for (int i = 0; i < 9; i++) {
                            ItemStack hotbarOrAir = (i < rawInv.length && rawInv[i] != null)
                                    ? rawInv[i]
                                    : new ItemStack(Material.AIR);
                            guiItems[36 + i] = hotbarOrAir;
                        }
                        RedisChat.getScheduler().runTask(() ->
                                openRawGUI(
                                        p,
                                        plugin.config.inv_title.replace("%player%", targetName),
                                        guiItems
                                )
                        );
                    });



            case ENDERCHEST -> plugin.getDataManager()
                    .getPlayerEnderchest(targetName)
                    .thenAccept(fetched -> {
                        // fetched ist effektiv final
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

        // Inhalte auf die GUI‑Größe trimmen/padden
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
