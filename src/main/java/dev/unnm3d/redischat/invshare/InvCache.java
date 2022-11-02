package dev.unnm3d.redischat.invshare;

import dev.unnm3d.ezredislib.EzRedisMessenger;
import dev.unnm3d.redischat.RedisChat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class InvCache {
    private final EzRedisMessenger ezRedisMessenger;

    public InvCache(EzRedisMessenger ezRedisMessenger) {
        this.ezRedisMessenger = ezRedisMessenger;
    }

    public void addInventory(String name, ItemStack[] inv) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.hset("invshare_inventories", name, serialize(inv));
            return jedis;
        });
        Bukkit.getScheduler().runTaskLaterAsynchronously(RedisChat.getInstance(),() ->
                ezRedisMessenger.jedisResourceFuture(jedis ->
                        jedis.hdel("invshare_inventories", name)
                )
        ,20L * 60L * 5L);
    }

    public ItemStack[] getInventory(String name) {
        return deserialize(ezRedisMessenger.jedisResourceFuture(jedis -> {
            String serializedInv=jedis.hget("invshare_inventories", name);
            return serializedInv==null?"":serializedInv;
        }).join());

    }

    public void addEnderchest(String name, ItemStack[] inv) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.hset("invshare_enderchests", name, serialize(inv));
            return jedis;
        });
        Bukkit.getScheduler().runTaskLaterAsynchronously(RedisChat.getInstance(),() ->
                        ezRedisMessenger.jedisResourceFuture(jedis ->
                                jedis.hdel("invshare_enderchests", name)
                        )
                ,20L * 60L * 5L);

    }

    public ItemStack[] getEnderchest(String name) {
        return deserialize(ezRedisMessenger.jedisResourceFuture(jedis -> {
            String serializedInv=jedis.hget("invshare_enderchests", name);
            return serializedInv==null?"":serializedInv;
        }).join());
    }

    public void addItem(String name, ItemStack item) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.hset("invshare_item", name, serialize(item));
            return jedis;
        });
        Bukkit.getScheduler().runTaskLaterAsynchronously(RedisChat.getInstance(),() ->
                        ezRedisMessenger.jedisResourceFuture(jedis ->
                                jedis.hdel("invshare_item", name)
                        )
                ,20L * 60L * 5L);

    }

    public ItemStack getItem(String name) {
        return ezRedisMessenger.jedisResourceFuture(jedis -> {
            String serializedInv=jedis.hget("invshare_item", name);
            ItemStack[] a = deserialize(serializedInv==null?"":serializedInv);
            if (a.length == 0) return new ItemStack(Material.AIR);
            return a[0];
        }).join();
    }
    private String serialize(ItemStack... items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            return Base64Coder.encodeLines(outputStream.toByteArray());

        } catch (Exception ignored) {
            return "";
        }
    }

    private ItemStack[] deserialize(String source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++)
                items[i] = (ItemStack) dataInput.readObject();

            return items;
        } catch (Exception ignored) {
            return new ItemStack[0];
        }
    }

}
