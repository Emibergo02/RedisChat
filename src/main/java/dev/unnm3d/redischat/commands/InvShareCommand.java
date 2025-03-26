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
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player p)) return true;

        if (strings.length == 0) return true;
        final String[] split = strings[0].split("-");
        if (split.length == 1) return true;

        try {
            final String playerName = split[0];
            InventoryType type = InventoryType.valueOf(split[1].toUpperCase());
            switch (type) {
                case ITEM -> plugin.getDataManager().getPlayerItem(playerName)
                        .thenAccept(item ->
                                RedisChat.getScheduler().runTask(() -> {
                                            if (plugin.config.debugItemShare) {
                                                plugin.getLogger().info("Itemshare openGUI for player " + p.getName() + ": " + item.getType());
                                            }
                                            if (item.getType().toString().endsWith("SHULKER_BOX")) {
                                                if (item.getItemMeta() instanceof BlockStateMeta bsm)
                                                    if (bsm.getBlockState() instanceof Container shulkerBox) {
                                                        openInvShareGui(p,
                                                                plugin.config.shulker_title.replace("%player%", playerName),
                                                                3,
                                                                shulkerBox.getSnapshotInventory().getContents()
                                                        );
                                                    }
                                            } else {
                                                openInvShareGuiItem(p,
                                                        plugin.config.item_title.replace("%player%", playerName),
                                                        item
                                                );
                                            }
                                        }
                                ));


                case INVENTORY -> plugin.getDataManager().getPlayerInventory(playerName)
                        .thenAccept(inventoryContents ->
                                RedisChat.getScheduler().runTask(() -> {
                                    if (plugin.config.debugItemShare) {
                                        plugin.getLogger().info("Invshare openGUI for player " + p.getName() + ": " + Arrays.toString(inventoryContents));
                                    }
                                    openInvShareGui(p,
                                            plugin.config.inv_title.replace("%player%", playerName),
                                            5,
                                            inventoryContents
                                    );
                                }));
                case ENDERCHEST -> plugin.getDataManager().getPlayerEnderchest(playerName)
                        .thenAccept(ecContents ->
                                RedisChat.getScheduler().runTask(() -> {
                                    if (plugin.config.debugItemShare) {
                                        plugin.getLogger().info("ECshare openGUI for player " + p.getName() + ": " + Arrays.toString(ecContents));
                                    }
                                    openInvShareGui(p,
                                            plugin.config.ec_title.replace("%player%", playerName),
                                            3,
                                            ecContents
                                    );
                                }));
            }
        } catch (IllegalArgumentException exception) {
            plugin.messages.sendMessage(p, plugin.messages.missing_arguments);
        }
        return true;
    }

    private void openInvShareGui(Player player, String title, int size, ItemStack[] items) {
        final Gui gui = Gui.empty(9, size);
        gui.addItems(Arrays.stream(items)
                .map(itemStack -> {
                    if (itemStack == null) return new ItemBuilder(Material.AIR);
                    return new ItemBuilder(itemStack);
                })
                .map(SimpleItem::new)
                .toArray(Item[]::new)
        );
        Window.single().setTitle(title).setGui(gui).setCloseHandlers(List.of(() -> new UniversalRunnable() {
            @Override
            public void run() {
                player.updateInventory();
            }
        }.runTaskLater(plugin, 1))).open(player);
    }

    private void openInvShareGuiItem(Player player, String title, ItemStack item) {
        Gui gui = Gui.empty(9, 3);
        gui.setItem(13, new SimpleItem(item));
        Window.single().setTitle(title).setGui(gui).setCloseHandlers(List.of(() -> new UniversalRunnable() {
            @Override
            public void run() {
                player.updateInventory();
            }
        }.runTaskLater(plugin, 1))).open(player);
    }

    public enum InventoryType {
        INVENTORY,
        ENDERCHEST,
        ITEM
    }

}
