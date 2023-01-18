package dev.unnm3d.redischat.redis;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.redis.redistools.RedisAbstract;
import dev.unnm3d.redischat.redis.redistools.RedisPubSub;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static dev.unnm3d.redischat.redis.redistools.RedisKeys.*;

public class RedisDataManager extends RedisAbstract {
    private final RedisChat plugin;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    public static int pubsubindex = 0;

    public RedisDataManager(RedisClient redisClient, RedisChat redisChat) {
        super(redisClient, 3000);
        this.plugin = redisChat;
    }

    public Optional<String> getReplyName(String requesterName) {
        if (RedisChat.config.debug) {
            Bukkit.getLogger().info("getReplyName: " + requesterName);
        }
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        String replyName = connection.sync().hget(REPLY.toString(), requesterName);
        connection.close();
        return Optional.ofNullable(replyName);

    }

    public void setReplyName(String nameReceiver, String requesterName) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("setReplyName: " + nameReceiver + " " + requesterName);
        }
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        connection.sync().hset(REPLY.toString(), nameReceiver, requesterName);
        connection.close();

    }

    public boolean isRateLimited(String playerName) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("isRateLimited: " + playerName);
        }
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        String result = connection.sync().get(RATE_LIMIT_PREFIX + playerName);
        connection.close();

        int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
        return nowMessages >= RedisChat.config.rate_limit;//messages higher than limit
    }

    public void setRateLimit(String playerName, int seconds) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("setRateLimit: " + playerName + " " + seconds);
        }
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        connection.setAutoFlushCommands(false);
        RedisAsyncCommands<String, String> rac = connection.async();

        rac.incr(RATE_LIMIT_PREFIX + playerName);
        rac.expire(RATE_LIMIT_PREFIX + playerName, seconds);
        connection.flushCommands();
        connection.close();
    }

    public void addPlayerName(String playerName) {
        getConnectionAsync(connection->
                connection.sadd(PLAYERLIST.toString(), playerName)
                .thenApply(result -> {
                    if (RedisChat.config.debug) {
                        plugin.getLogger().info("00 Added player " + playerName + " to the playerlist");
                    }
                    return result;
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error adding player name to redis");
                    return null;
                }));

    }

    public CompletionStage<Set<String>> getPlayerList() {
        return getConnectionAsync(connection->
                connection.smembers(PLAYERLIST.toString())
                .thenApply(result -> {
                    if (RedisChat.config.debug) {
                        plugin.getLogger().info("repeated00 get playerlist " + result);
                    }
                    return result;
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error getting player list from redis");
                    return null;
                })
        );
    }

    public void removePlayerName(String playerName) {
        removePlayerNames(new String[]{playerName});
    }

    public void removePlayerNames(String[] playerNames) {
        getConnectionAsync(connection->
                connection.srem(PLAYERLIST.toString(), playerNames)
                .thenApply(result -> {
                    if (RedisChat.config.debug) {
                        plugin.getLogger().info("01 Removed players " + Arrays.toString(playerNames) + " from the playerlist");
                    }
                    return result;
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error removing player name to redis");
                    return null;
                })
        );

    }

    public void toggleIgnoring(String playerName, String ignoringName) {
        getConnectionAsync(connection ->
                connection.sadd(IGNORE_PREFIX + playerName, ignoringName)
                        .thenApply(response -> {

                            StatefulRedisConnection<String, String> connection2 = lettuceRedisClient.connect();
                            if(response == 0)
                                connection2.async().srem(IGNORE_PREFIX + playerName, ignoringName)
                                        .thenAccept(response2 -> connection2.close());
                            else
                                connection2.async().expire(IGNORE_PREFIX + playerName, 60 * 60 * 24 * 7)
                                        .thenAccept(response2 -> connection2.close());
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("02 Toggled ignoring " + ignoringName + " for " + playerName);
                            }
                            return response;
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error toggle ignore player name to redis");
                            return null;
                        })
        );

    }

    public CompletionStage<Boolean> isIgnoring(String playerName, String ignoringName) {
        return getConnectionAsync(connection ->
                connection.smembers(IGNORE_PREFIX + playerName)
                        .thenApply(result -> {
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("03 Ignoring list for " + playerName + " is " + result);
                            }
                            return result.contains(ignoringName) || result.contains("*") || result.contains("all");
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting ignore list from redis");
                            return null;
                        }));
    }

    public CompletionStage<Set<String>> ignoringList(String playerName) {
        return getConnectionAsync(connection ->
                connection.smembers(IGNORE_PREFIX + playerName)
                        .thenApply(result -> {
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("03 Ignoring list for " + playerName + " is " + result);
                            }
                            return result;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting ignore list from redis");
                            return null;
                        }));

    }

    public void addInventory(String name, ItemStack[] inv) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("addInventory: " + name);
        }
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_INVENTORY.toString(), name, serialize(inv))
                        .thenApply(response -> {
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("05 Added inventory for " + name);
                            }
                            scheduleConnection(scheduled -> {
                                scheduled.sync().hdel(INVSHARE_INVENTORY.toString(), name);
                                plugin.getLogger().warning("06 Removing inv");
                                return null;
                            }, 60, TimeUnit.SECONDS);
                            return response;
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding inventory");
                            return null;
                        })
        );
    }

    public void addItem(String name, ItemStack item) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("addItem: " + name);
        }
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_ITEM.toString(), name, serialize(item))
                        .thenApply(response -> {
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("08 Added item for " + name);
                            }
                            scheduleConnection(scheduled -> {
                                scheduled.sync().hdel(INVSHARE_ITEM.toString(), name);
                                plugin.getLogger().warning("09 Removing item");
                                return null;
                            }, 60, TimeUnit.SECONDS);
                            return response;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding item");
                            return null;
                        })
        );
    }

    public void addEnderchest(String name, ItemStack[] inv) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("addEnderchest: " + name);
        }
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_ENDERCHEST.toString(), name, serialize(inv))
                        .thenApply(response -> {
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("10 Added enderchest for " + name);
                            }
                            scheduleConnection(scheduled -> {
                                scheduled.sync().hdel(INVSHARE_ENDERCHEST.toString(), name);
                                if (RedisChat.config.debug) {
                                    plugin.getLogger().info("11 Removing enderchest");
                                }
                                return null;
                            }, 60, TimeUnit.SECONDS);
                            return response;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding enderchest");
                            return null;
                        })
        );
    }

    public CompletionStage<ItemStack> getPlayerItem(String playerName) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("getPlayerItem: " + playerName);
        }
        return getConnectionAsync(connection->
                connection.hget(INVSHARE_ITEM.toString(), playerName)
                        .thenApply(serializedInv -> {
                            ItemStack[] itemStacks = deserialize(serializedInv == null ? "" : serializedInv);
                            if(RedisChat.config.debug) {
                                plugin.getLogger().info("04 Got item for " + playerName + " is " + (itemStacks.length!=0?itemStacks[0].toString():"null"));
                            }
                            if (itemStacks.length == 0) return new ItemStack(Material.AIR);
                            return itemStacks[0];
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting item");
                            return null;
                        })
        );
    }

    public CompletionStage<ItemStack[]> getPlayerInventory(String playerName) {
        if(RedisChat.config.debug){
            Bukkit.getLogger().info("getPlayerInventory: " + playerName);
        }
        return getConnectionAsync(connection ->
                connection.hget(INVSHARE_INVENTORY.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (RedisChat.config.debug) {
                                plugin.getLogger().info("12 Got inventory for " + playerName + " is " + (serializedInv == null ? "null" : serializedInv));
                            }
                            return deserialize(serializedInv == null ? "" : serializedInv);
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting inv");
                            return null;
                        }));

    }

    public CompletionStage<ItemStack[]> getPlayerEnderchest(String playerName) {
        if (RedisChat.config.debug) {
            Bukkit.getLogger().info("getPlayerEnderchest: " + playerName);
        }
        return getConnectionAsync(connection ->
                connection.hget(INVSHARE_ENDERCHEST.toString(), playerName)
                        .thenApply(serializedInv -> {
                                    if (RedisChat.config.debug) {
                                        plugin.getLogger().info("13 Got enderchest for " + playerName + " is " + (serializedInv == null ? "null" : serializedInv));
                                    }
                                    return deserialize(serializedInv == null ? "" : serializedInv);
                                }
                        ).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting ec");
                            return null;
                        }));
    }

    public void listenChatPackets() {
        this.pubSubConnection = lettuceRedisClient.connectPubSub();
        this.pubSubConnection.addListener(new RedisPubSub<>() {
            @Override
            public void message(String channel, String message) {
                //plugin.getLogger().info("Received message #"+pubsubindex+" on channel " + channel + ": " + message);
                ChatPacket chatPacket = new ChatPacket(message);
                //Check if receiver is online and send priv message to spychat
                if (chatPacket.getReceiverName() != null) {
                    boolean realReceiver = false;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().equals(chatPacket.getReceiverName())) {
                            realReceiver = true;
                        } else if (player.hasPermission(Permission.REDIS_CHAT_SPYCHAT.getPermission())) {
                            plugin.getChatListener().onSpyPrivateChat(chatPacket.getReceiverName(), chatPacket.getSenderName(), player, chatPacket.getMessage());
                        }
                    }
                    if (!realReceiver) return;
                }

                if (chatPacket.isPrivate()) {
                    CompletableFuture.runAsync(() ->
                            plugin.getRedisDataManager().isIgnoring(chatPacket.getReceiverName(), chatPacket.getSenderName())
                            .thenAccept(ignored -> {
                                if (!ignored)
                                    plugin.getChatListener().onPrivateChat(chatPacket.getSenderName(), chatPacket.getReceiverName(), chatPacket.getMessage());
                            }));

                } else {
                    plugin.getChatListener().onPublicChat(chatPacket.getMessage());
                }
            }
        });
        this.pubSubConnection.async().subscribe(CHAT_CHANNEL.toString()).exceptionally(throwable -> {
            throwable.printStackTrace();
            plugin.getLogger().warning("Error subscribing to chat channel");
            return null;
        }).thenAccept(subscription -> plugin.getLogger().info("Subscribed to channel: " + CHAT_CHANNEL));


    }

    public void sendObjectPacket(ChatPacket packet) {
        getConnectionAsync(conn->
                conn.publish(CHAT_CHANNEL.toString(), packet.serialize())
                .thenApply(integer -> {
                    if(RedisChat.config.debug) {
                        plugin.getLogger().warning("#"+ (++pubsubindex) +"received by " + integer + " servers");
                    }
                    return integer;
                })
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    plugin.getLogger().warning("Error sending object packet");
                    return 0L;
                })
        );

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

    @Override
    public void close() {
        this.pubSubConnection.close();
        super.close();
    }

}
