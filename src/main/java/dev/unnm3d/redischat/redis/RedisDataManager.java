package dev.unnm3d.redischat.redis;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.mail.Mail;
import dev.unnm3d.redischat.redis.redistools.RedisAbstract;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static dev.unnm3d.redischat.redis.redistools.RedisKeys.*;

public class RedisDataManager extends RedisAbstract {
    private final RedisChat plugin;
    public static int pubSubIndex = 0;

    public RedisDataManager(RedisClient redisClient, RedisChat redisChat) {
        super(redisClient);
        this.plugin = redisChat;
    }

    public Optional<String> getReplyName(String requesterName) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        String replyName = connection.sync().hget(REPLY.toString(), requesterName);
        connection.close();
        return Optional.ofNullable(replyName);

    }

    public void setReplyName(String nameReceiver, String requesterName) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        connection.sync().hset(REPLY.toString(), nameReceiver, requesterName);
        connection.close();
    }

    public boolean isRateLimited(String playerName) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        String result = connection.sync().get(RATE_LIMIT_PREFIX + playerName);
        connection.close();

        int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
        return nowMessages >= plugin.config.rate_limit;//messages higher than limit
    }

    public void setRateLimit(String playerName, int seconds) {
        getConnectionPipeline(connection -> {
            connection.incr(RATE_LIMIT_PREFIX + playerName);
            connection.expire(RATE_LIMIT_PREFIX + playerName, seconds);
            return null;
        });
    }

    public CompletionStage<Boolean> isSpying(String playerName) {
        return getConnectionAsync(connection ->
                connection.sismember(SPYING_LIST.toString(), playerName)
                        .thenApply(result -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("isSpying " + playerName + " " + result);
                            }
                            return result;
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting spy list from redis");
                            return null;
                        })
        );
    }

    public void setSpying(String playerName, boolean spy) {
        getConnectionAsync(connection -> {
                    if (spy) {
                        return connection.sadd(SPYING_LIST.toString(), playerName)
                                .thenApply(result -> {
                                    if (plugin.config.debug) {
                                        plugin.getLogger().info("setSpying " + playerName + " " + result);
                                    }
                                    return result;
                                })
                                .exceptionally(throwable -> {
                                    throwable.printStackTrace();
                                    plugin.getLogger().warning("Error getting spy list from redis");
                                    return null;
                                });
                    } else {
                        return connection.srem(SPYING_LIST.toString(), playerName)
                                .thenApply(result -> {
                                    if (plugin.config.debug) {
                                        plugin.getLogger().info("setSpying " + playerName + " " + result);
                                    }
                                    return result;
                                })
                                .exceptionally(throwable -> {
                                    throwable.printStackTrace();
                                    plugin.getLogger().warning("Error getting spy list from redis");
                                    return null;
                                });
                    }
                }
        );
    }

    public CompletionStage<Boolean> toggleIgnoring(String playerName, String ignoringName) {
        return getConnectionAsync(connection ->
                connection.sadd(IGNORE_PREFIX + playerName, ignoringName)
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("02 Toggling ignoring " + ignoringName + " for " + playerName);
                            }
                            if (response == 0) {
                                getConnectionAsync(connection3 -> connection3.srem(IGNORE_PREFIX + playerName, ignoringName));
                                return false;
                            }
                            getConnectionAsync(connection3 -> connection3.expire(IGNORE_PREFIX + playerName, 60 * 60 * 24 * 7));
                            return true;

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
                            if (plugin.config.debug) {
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
                            if (plugin.config.debug) {
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
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_INVENTORY.toString(), name, serialize(inv))
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("05 Added inventory for " + name);
                            }
                            scheduleConnection(scheduled -> {
                                scheduled.async().hdel(INVSHARE_INVENTORY.toString(), name).thenAccept(response2 -> {
                                    if (plugin.config.debug) {
                                        plugin.getLogger().warning("06 Removing inv");
                                    }
                                });
                                return null;
                            }, 120, TimeUnit.SECONDS);
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
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_ITEM.toString(), name, serialize(item))
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("08 Added item for " + name);
                            }
                            scheduleConnection(scheduled -> {
                                scheduled.async().hdel(INVSHARE_ITEM.toString(), name).thenAccept(response2 -> {
                                    if (plugin.config.debug) {
                                        plugin.getLogger().warning("09 Removing item");
                                    }
                                });
                                return null;
                            }, 120, TimeUnit.SECONDS);
                            return response;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding item");
                            return null;
                        })
        );
    }

    public void addEnderchest(String name, ItemStack[] inv) {
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_ENDERCHEST.toString(), name, serialize(inv))
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("10 Added enderchest for " + name);
                            }
                            scheduleConnection(scheduled -> {
                                scheduled.async().hdel(INVSHARE_ENDERCHEST.toString(), name).thenAccept(response2 -> {
                                    if (plugin.config.debug) {
                                        plugin.getLogger().warning("11 Removing enderchest");
                                    }
                                });
                                return null;
                            }, 120, TimeUnit.SECONDS);
                            return response;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding enderchest");
                            return null;
                        })
        );
    }

    public CompletionStage<ItemStack> getPlayerItem(String playerName) {
        return getConnectionAsync(connection ->
                connection.hget(INVSHARE_ITEM.toString(), playerName)
                        .thenApply(serializedInv -> {
                            ItemStack[] itemStacks = deserialize(serializedInv == null ? "" : serializedInv);
                            if (plugin.config.debug) {
                                plugin.getLogger().info("04 Got item for " + playerName + " is " + (itemStacks.length != 0 ? itemStacks[0].toString() : "null"));
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
        return getConnectionAsync(connection ->
                connection.hget(INVSHARE_INVENTORY.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (plugin.config.debug) {
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
        return getConnectionAsync(connection ->
                connection.hget(INVSHARE_ENDERCHEST.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("13 Got enderchest for " + playerName + " is " + (serializedInv == null ? "null" : serializedInv));
                            }
                            return deserialize(serializedInv == null ? "" : serializedInv);
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting ec");
                            return null;
                        }));
    }

    public CompletionStage<List<Mail>> getPlayerPrivateMail(String playerName) {
        return getConnectionAsync(connection ->
                connection.hgetall(PRIVATE_MAIL_PREFIX + playerName)
                        .thenApply(this::deserializeMails)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting private mails");
                            return null;
                        }));
    }

    public CompletionStage<Boolean> setPlayerPrivateMail(Mail mail) {
        return getConnectionPipeline(connection -> {
            connection.hset(PRIVATE_MAIL_PREFIX + mail.getReceiver(), String.valueOf(mail.getId()), mail.toString());
            mail.setCategory(Mail.MailCategory.SENT);
            return connection.hset(PRIVATE_MAIL_PREFIX + mail.getSender(), String.valueOf(mail.getId()), mail.toString()).exceptionally(throwable -> {
                throwable.printStackTrace();
                plugin.getLogger().warning("Error setting private mail");
                return null;
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            plugin.getLogger().warning("Error setting private mail");
            return null;
        });
    }

    public CompletionStage<Boolean> setPublicMail(Mail mail) {
        return getConnectionAsync(connection ->
                connection.hset(PUBLIC_MAIL.toString(), String.valueOf(mail.getId()), mail.toString()).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error setting public mail");
                    return null;
                }));
    }

    public CompletionStage<List<Mail>> getPublicMails() {
        return getConnectionAsync(connection ->
                connection.hgetall(PUBLIC_MAIL.toString())
                        .thenApply(this::deserializeMails).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting public mails");
                            return null;
                        }));
    }

    private List<Mail> deserializeMails(Map<String, String> timestampMail) {
        List<Mail> mailList = new ArrayList<>();
        for (Map.Entry<String, String> timestampMailEntry : timestampMail.entrySet()) {
            mailList.add(new Mail(Double.parseDouble(timestampMailEntry.getKey()), timestampMailEntry.getValue()));
        }
        return mailList;
    }

    public void listenChatPackets() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = getPubSubConnection();
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                //plugin.getLogger().info("Received message #"+pubsubindex+" on channel " + channel + ": " + message);
                ChatMessageInfo chatMessageInfo = new ChatMessageInfo(message);

                if (chatMessageInfo.isPrivate()) {
                    long init = System.currentTimeMillis();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (plugin.getSpyManager().isSpying(player.getName())) {//Spychat
                            plugin.getComponentProvider().sendSpyChat(chatMessageInfo.getReceiverName(), chatMessageInfo.getSenderName(), player, chatMessageInfo.getMessage());
                        }
                        if (player.getName().equals(chatMessageInfo.getReceiverName())) {//Private message
                            plugin.getRedisDataManager().isIgnoring(chatMessageInfo.getReceiverName(), chatMessageInfo.getSenderName())
                                    .thenAccept(ignored -> {
                                        if (!ignored)
                                            plugin.getComponentProvider().sendPrivateChat(chatMessageInfo);
                                        if (plugin.config.debug) {
                                            plugin.getLogger().info("Private message sent to " + chatMessageInfo.getReceiverName() + " with ignore: "+ignored+" in " + (System.currentTimeMillis() - init) + "ms");
                                        }
                                    });
                        }
                    }
                    return;
                }

                plugin.getComponentProvider().sendGenericChat(chatMessageInfo);
            }

            @Override
            public void message(String pattern, String channel, String message) {
            }

            @Override
            public void subscribed(String channel, long count) {
            }

            @Override
            public void psubscribed(String pattern, long count) {
            }

            @Override
            public void unsubscribed(String channel, long count) {
            }

            @Override
            public void punsubscribed(String pattern, long count) {
            }
        });

        pubSubConnection.async().subscribe(CHAT_CHANNEL.toString())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error subscribing to chat channel");
                    return null;
                })
                .thenAccept(subscription -> plugin.getLogger().info("Subscribed to channel: " + CHAT_CHANNEL));


    }

    public void sendObjectPacket(ChatMessageInfo packet) {
        getConnectionAsync(conn ->
                conn.publish(CHAT_CHANNEL.toString(), packet.serialize())
                        .thenApply(integer -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().warning("#" + (++pubSubIndex) + "received by " + integer + " servers");
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

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception ignored) {
            return "";
        }
    }

    private ItemStack[] deserialize(String source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(source));
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
        super.close();
    }

}
