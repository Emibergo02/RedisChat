package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.mail.Mail;
import dev.unnm3d.redischat.datamanagers.redistools.RedisAbstract;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static dev.unnm3d.redischat.datamanagers.DataKeys.*;

public class RedisDataManager extends RedisAbstract implements DataManager {
    private final RedisChat plugin;
    public static int pubSubIndex = 0;

    public RedisDataManager(RedisClient redisClient, RedisChat redisChat) {
        super(redisClient);
        this.plugin = redisChat;
        listenSub();
    }

    private void listenSub() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = getPubSubConnection();
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                if (channel.equals(CHAT_CHANNEL.toString()))
                    plugin.getChatListener().receiveChatMessage(new ChatMessageInfo(message));
                else if (channel.equals(PLAYERLIST.toString()))
                    if (plugin.getPlayerListManager() != null)
                        plugin.getPlayerListManager().updatePlayerList(Arrays.asList(message.split("§")));
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
        pubSubConnection.async().subscribe(CHAT_CHANNEL.toString(), PLAYERLIST.toString())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error subscribing to chat channel");
                    return null;
                })
                .thenAccept(subscription -> plugin.getLogger().info("Subscribed to channel: " + CHAT_CHANNEL));


    }

    @Override
    public Optional<String> getReplyName(String requesterName) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        String replyName = connection.sync().hget(REPLY.toString(), requesterName);
        connection.close();
        return Optional.ofNullable(replyName);

    }

    @Override
    public void setReplyName(String nameReceiver, String requesterName) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        connection.sync().hset(REPLY.toString(), nameReceiver, requesterName);
        connection.close();
    }

    @Override
    public boolean isRateLimited(String playerName) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        String result = connection.sync().get(RATE_LIMIT_PREFIX + playerName);
        connection.close();

        int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
        return nowMessages >= plugin.config.rate_limit;//messages higher than limit
    }

    @Override
    public void setRateLimit(String playerName, int seconds) {
        getConnectionPipeline(connection -> {
            connection.incr(RATE_LIMIT_PREFIX + playerName);
            connection.expire(RATE_LIMIT_PREFIX + playerName, seconds);
            return null;
        });
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public CompletionStage<List<String>> ignoringList(String playerName) {
        return getConnectionAsync(connection ->
                connection.smembers(IGNORE_PREFIX + playerName)
                        .thenApply(result -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("03 Ignoring list for " + playerName + " is " + result);
                            }
                            return List.copyOf(result);
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting ignore list from redis");
                            return null;
                        }));

    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public CompletionStage<Boolean> setPlayerPrivateMail(Mail mail) {
        return getConnectionPipeline(connection -> {
            connection.hset(PRIVATE_MAIL_PREFIX + mail.getReceiver(), String.valueOf(mail.getId()), mail.serialize());
            mail.setCategory(Mail.MailCategory.SENT);
            return connection.hset(PRIVATE_MAIL_PREFIX + mail.getSender(), String.valueOf(mail.getId()), mail.serialize()).exceptionally(throwable -> {
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

    @Override
    public CompletionStage<Boolean> setPublicMail(Mail mail) {
        return getConnectionAsync(connection ->
                connection.hset(PUBLIC_MAIL.toString(), String.valueOf(mail.getId()), mail.serialize()).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error setting public mail");
                    return null;
                }));
    }

    @Override
    public CompletionStage<List<Mail>> getPublicMails() {
        return getConnectionAsync(connection ->
                connection.hgetall(PUBLIC_MAIL.toString())
                        .thenApply(this::deserializeMails).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting public mails");
                            return null;
                        }));
    }

    @Override
    public void sendChatMessage(ChatMessageInfo packet) {
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

    @Override
    public void publishPlayerList(List<String> playerNames) {
        getConnectionAsync(connection ->
                connection.publish(PLAYERLIST.toString(),
                        String.join("§", playerNames))
        );
    }


    @Override
    public void close() {
        super.close();
    }

}
