package dev.unnm3d.redischat.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
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
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player p)) return true;
        if (args.length == 0) return true;

        String[] split = args[0].split("-");
        if (split.length != 2) return true;

        String playerName = split[0];
        InventoryType type;
        try {
            type = InventoryType.valueOf(split[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            return true;
        }

        switch (type) {
            case ITEM -> plugin.getDataManager().getPlayerItem(playerName)
                    .thenAccept(item ->
                            RedisChat.getScheduler().runTask(() -> {
                                if (plugin.config.debugItemShare) {
                                    plugin.getLogger().info("Itemshare openGUI for player " + p.getName() + ": " + item.getType());
                                }
                                if (item.getType().toString().endsWith("SHULKER_BOX") && item.getItemMeta() instanceof BlockStateMeta bsm
                                        && bsm.getBlockState() instanceof Container shulkerBox) {

                                    openInvShareGui(p,
                                            plugin.config.shulker_title.replace("%player%", playerName),
                                            3,
                                            shulkerBox.getSnapshotInventory().getContents()
                                    );
                                } else {
                                    openInvShareGuiItem(p,
                                            plugin.config.item_title.replace("%player%", playerName),
                                            item
                                    );
                                }
                            })
                    );

            case INVENTORY -> plugin.getDataManager().getPlayerInventory(playerName)
                    .thenAccept(rawInv ->
                            RedisChat.getScheduler().runTask(() -> {
                                // GUI dimensions: 9 columns × 5 rows = 45 slots
                                ItemStack[] guiItems = new ItemStack[45];

                                // Hotbar & Armor Slots reorder
                                for (int i = 0; i < 9; i++) {
                                    int rawIndex = 36 + i;
                                    ItemStack item = (rawInv.length > rawIndex && rawInv[rawIndex] != null)
                                            ? rawInv[rawIndex]
                                            : new ItemStack(Material.AIR);

                                    guiItems[i] = item;         // Top row 
                                    guiItems[36 + i] = item;    // Bottom row (hotbar)
                                }

                                // Main inventory
                                for (int slot = 9; slot < 36; slot++) {
                                    guiItems[slot] = (rawInv.length > slot && rawInv[slot] != null)
                                            ? rawInv[slot]
                                            : new ItemStack(Material.AIR);
                                }

                                if (plugin.config.debugItemShare) {
                                    plugin.getLogger().info("Invshare openGUI for player " + p.getName() + ": " + Arrays.toString(guiItems));
                                }

                                openInvShareGui(
                                        p,
                                        plugin.config.inv_title.replace("%player%", playerName),
                                        5, // number of rows
                                        guiItems
                                );
                            })
                    );

            case ENDERCHEST -> plugin.getDataManager().getPlayerEnderchest(playerName)
                    .thenAccept(ecContents ->
                            RedisChat.getScheduler().runTask(() -> {
                                if (plugin.config.debugItemShare) {
                                    plugin.getLogger().info("ECshare openGUI for player " + p.getName() + ": " + Arrays.toString(ecContents));
                                }

                                int totalSlots;
                                if (plugin.getCustomInventoryAPI().hasCustomEnderChest(p)) {
                                    ItemStack[] custom = plugin.getCustomInventoryAPI().getCustomEnderChest(p);
                                    totalSlots = custom.length;
                                } else {
                                    totalSlots = p.getEnderChest().getSize();
                                }
                                int rows = totalSlots / 9;

                                openInvShareGui(
                                        p,
                                        plugin.config.ec_title.replace("%player%", playerName),
                                        rows,
                                        ecContents
                                );
                            })
                    );
        }

        return true;
    }

    private void openInvShareGui(Player player, String title, int rows, ItemStack[] items) {
        Gui gui = Gui.empty(9, rows);
        gui.addItems(Arrays.stream(items)
                .map(itemStack -> itemStack == null ? new ItemBuilder(Material.AIR) : new ItemBuilder(itemStack))
                .map(SimpleItem::new)
                .toArray(Item[]::new)
        );

        Window.single()
                .setTitle(title)
                .setGui(gui)
                .setCloseHandlers(List.of(() -> new UniversalRunnable() {
                    @Override
                    public void run() {
                        player.updateInventory();
                    }
                }.runTaskLater(plugin, 1)))
                .open(player);
    }

    private void openInvShareGuiItem(Player player, String title, ItemStack item) {
        Gui gui = Gui.empty(9, 3);
        gui.setItem(13, new SimpleItem(item));

        Window.single()
                .setTitle(title)
                .setGui(gui)
                .setCloseHandlers(List.of(() -> new UniversalRunnable() {
                    @Override
                    public void run() {
                        player.updateInventory();
                    }
                }.runTaskLater(plugin, 1)))
                .open(player);
    }

    public enum InventoryType {
        INVENTORY,
        ENDERCHEST,
        ITEM
    }
}
