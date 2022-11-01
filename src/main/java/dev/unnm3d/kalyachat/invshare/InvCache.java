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
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.hset("invshare_inventories", name, SerializeInventory.write(inv));
            return jedis;
        });
        Bukkit.getScheduler().runTaskLaterAsynchronously(KalyaChat.getInstance(),() ->
                ezRedisMessenger.jedisResourceFuture(jedis ->
                        jedis.hdel("invshare_inventories", name)
                )
        ,20L * 60L * 5L);
    }

    public ItemStack[] getInventory(String name) {
        return SerializeInventory.read(ezRedisMessenger.jedisResourceFuture(jedis -> {
            String serializedInv=jedis.hget("invshare_inventories", name);
            return serializedInv==null?"":serializedInv;
        }).join());

    }

    public void addEnderchest(String name, ItemStack[] inv) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.hset("invshare_enderchests", name, SerializeInventory.write(inv));
            return jedis;
        });
        Bukkit.getScheduler().runTaskLaterAsynchronously(KalyaChat.getInstance(),() ->
                        ezRedisMessenger.jedisResourceFuture(jedis ->
                                jedis.hdel("invshare_enderchests", name)
                        )
                ,20L * 60L * 5L);

    }

    public ItemStack[] getEnderchest(String name) {
        return SerializeInventory.read(ezRedisMessenger.jedisResourceFuture(jedis -> {
            String serializedInv=jedis.hget("invshare_enderchests", name);
            return serializedInv==null?"":serializedInv;
        }).join());
    }

    public void addItem(String name, ItemStack item) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.hset("invshare_item", name, SerializeInventory.write(item));
            return jedis;
        });
        Bukkit.getScheduler().runTaskLaterAsynchronously(KalyaChat.getInstance(),() ->
                        ezRedisMessenger.jedisResourceFuture(jedis ->
                                jedis.hdel("invshare_item", name)
                        )
                ,20L * 60L * 5L);

    }

    public ItemStack getItem(String name) {
        return ezRedisMessenger.jedisResourceFuture(jedis -> {
            String serializedInv=jedis.hget("invshare_item", name);
            ItemStack[] a = SerializeInventory.read(serializedInv==null?"":serializedInv);
            if (a.length == 0) return new ItemStack(Material.AIR);
            return a[0];
        }).join();
    }

}
