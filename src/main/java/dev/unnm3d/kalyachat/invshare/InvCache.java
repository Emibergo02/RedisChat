package dev.unnm3d.kalyachat.invshare;

import dev.unnm3d.ezredislib.EzRedisMessenger;
import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.invshare.utils.SerializeInventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class InvCache {
    private final EzRedisMessenger ezRedisMessenger;

    public InvCache(EzRedisMessenger ezRedisMessenger) {
        this.ezRedisMessenger = ezRedisMessenger;
    }

    public void addInventory(String name, ItemStack[] inv) {
        ezRedisMessenger.getJedis().hset("invshare_inventories", name, SerializeInventory.write(inv));
        Bukkit.getScheduler().runTaskLaterAsynchronously(KalyaChat.getInstance(), () -> ezRedisMessenger.getJedis().hdel("invshare_inventories", name), 20L * 60L * 5L);
    }

    public ItemStack[] getInventory(String name) {
        return SerializeInventory.read(ezRedisMessenger.getJedis().hget("invshare_inventories", name) == null ? "" : ezRedisMessenger.getJedis().hget("invshare_inventories", name));
    }

    public void addEnderchest(String name, ItemStack[] inv) {
        ezRedisMessenger.getJedis().hset("invshare_enderchests", name, SerializeInventory.write(inv));
        Bukkit.getScheduler().runTaskLaterAsynchronously(KalyaChat.getInstance(), () -> ezRedisMessenger.getJedis().hdel("invshare_enderchests", name), 20L * 60L * 5L);

    }

    public ItemStack[] getEnderchest(String name) {
        return SerializeInventory.read(ezRedisMessenger.getJedis().hget("invshare_enderchests", name) == null ? "" : ezRedisMessenger.getJedis().hget("invshare_enderchests", name));
    }

    public void addItem(String name, ItemStack item) {
        ezRedisMessenger.getJedis().hset("invshare_item", name, SerializeInventory.write(item));
        Bukkit.getScheduler().runTaskLaterAsynchronously(KalyaChat.getInstance(), () -> ezRedisMessenger.getJedis().hdel("invshare_item", name), 20L * 60L * 5L);

    }

    public ItemStack getItem(String name) {
        String result = ezRedisMessenger.getJedis().hget("invshare_item", name);
        ItemStack[] a = SerializeInventory.read(result == null ? "" : result);
        if (a.length == 0) return new ItemStack(Material.AIR);
        return a[0];
    }

}
