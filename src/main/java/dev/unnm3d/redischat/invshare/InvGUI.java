package dev.unnm3d.redischat.invshare;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class InvGUI implements Listener {
    private static final List<String> invTitles = new ArrayList<>();
    private final Inventory inv;

    public InvGUI(HumanEntity player, String title, int size, ItemStack[] items) {
        inv = Bukkit.createInventory(null, size, title);

        // Put the items into the inventory
        inv.setContents(items);
        Bukkit.getScheduler().runTask(RedisChat.getInstance(), () -> {
            player.openInventory(inv);
            invTitles.add(title);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getOpenInventory().getTopInventory() == inv || player.getOpenInventory().getBottomInventory() == inv) {
                        player.closeInventory();
                    }
                    invTitles.remove(title);
                }
            }.runTaskLater(RedisChat.getInstance(), 20 * 60 * 5);
        });
    }

    public InvGUI(HumanEntity player, String title, ItemStack item) {
        inv = Bukkit.createInventory(null, 27, title);

        // Put the items into the inventory
        inv.setItem(13, item);
        Bukkit.getScheduler().runTask(RedisChat.getInstance(), () -> {
            player.openInventory(inv);
            invTitles.add(title);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getOpenInventory().getTopInventory() == inv || player.getOpenInventory().getBottomInventory() == inv) {
                        player.closeInventory();
                    }
                    invTitles.remove(title);
                }
            }.runTaskLater(RedisChat.getInstance(), 20 * 60 * 5);
        });
    }

    public static class GuiListener implements Listener {
        @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
        public void onInventoryClick(InventoryClickEvent e) {
            if (invTitles.contains(e.getView().getTitle())) {
                e.setCancelled(true);
            }


        }

        @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
        public void onInventoryDrag(InventoryDragEvent e) {
            if (invTitles.contains(e.getView().getTitle())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
        public void onInventoryClose(InventoryCloseEvent e) {
            if (!invTitles.contains(e.getView().getTitle())) return;
            if (e.getPlayer() instanceof Player p) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.updateInventory();
                        invTitles.remove(e.getView().getTitle());
                    }
                }.runTaskLater(RedisChat.getInstance(), 2);
            }

        }
    }


}