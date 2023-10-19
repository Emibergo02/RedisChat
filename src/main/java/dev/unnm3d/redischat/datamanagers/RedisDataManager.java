package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.channels.PlayerChannel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.datamanagers.redistools.RedisAbstract;
import dev.unnm3d.redischat.mail.Mail;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static dev.unnm3d.redischat.datamanagers.DataKeys.*;

public class RedisDataManager extends RedisAbstract implements DataManager {
    private final RedisChat plugin;
    public static int pubSubIndex = 0;

    public RedisDataManager(RedisClient redisClient, RedisChat redisChat) {
        super(redisClient, redisChat.config.redis.poolSize() <= 0 ? 1 : redisChat.config.redis.poolSize());
        this.plugin = redisChat;
        listenSub();
    }

    public static RedisDataManager startup(RedisChat redisChat) {
        RedisURI.Builder redisURIBuilder = RedisURI.builder()
                .withHost(redisChat.config.redis.host())
                .withPort(redisChat.config.redis.port())
                .withDatabase(redisChat.config.redis.database())
                .withTimeout(Duration.of(redisChat.config.redis.timeout(), TimeUnit.MILLISECONDS.toChronoUnit()))
                .withClientName(redisChat.config.redis.clientName());
        if (redisChat.config.redis.user().equals("changecredentials"))
            redisChat.getServer().getLogger().warning("You are using default redis credentials. Please change them in the config.yml file!");
        //Authentication params
        redisURIBuilder = redisChat.config.redis.password().equals("") ?
                redisURIBuilder :
                redisChat.config.redis.user().equals("") ?
                        redisURIBuilder.withPassword(redisChat.config.redis.password().toCharArray()) :
                        redisURIBuilder.withAuthentication(redisChat.config.redis.user(), redisChat.config.redis.password());
        return new RedisDataManager(RedisClient.create(redisURIBuilder.build()), redisChat);
    }

    private void listenSub() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = getPubSubConnection();
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                if (channel.equals(CHAT_CHANNEL.toString())) {
                    if (plugin.config.debug) {
                        plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
                    }
                    plugin.getChannelManager().sendLocalChatMessage(new ChatMessageInfo(message));
                } else if (channel.equals(PLAYERLIST.toString())) {
                    if (plugin.getPlayerListManager() != null)
                        plugin.getPlayerListManager().updatePlayerList(Arrays.asList(message.split("§")));
                } else if (channel.equals(REJOIN_CHANNEL.toString())) {
                    if (plugin.getJoinQuitManager() != null)
                        plugin.getJoinQuitManager().rejoinRequest(message);
                }
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
        pubSubConnection.async().subscribe(CHAT_CHANNEL.toString(), PLAYERLIST.toString(), REJOIN_CHANNEL.toString())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error subscribing to chat channel");
                    return null;
                })
                .thenAccept(subscription -> plugin.getLogger().info("Subscribed to channel: " + CHAT_CHANNEL));
    }

    @Override
    public CompletionStage<Optional<String>> getReplyName(@NotNull String requesterName) {
        if (plugin.config.debug) {
            plugin.getLogger().info("Getting reply name for " + requesterName);
        }
        return getConnectionAsync(connection -> connection.hget(REPLY.toString(), requesterName))
                .thenApply(Optional::ofNullable)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error getting reply name from redis");
                    return Optional.empty();
                });

    }

    @Override
    public void setReplyName(@NotNull String nameReceiver, @NotNull String requesterName) {
        getConnectionPipeline(conn -> {
            conn.hset(REPLY.toString(), nameReceiver, requesterName)
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error when setting reply name");
                        return null;
                    });
            conn.hset(REPLY.toString(), requesterName, nameReceiver)
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error when setting reply name");
                        return null;
                    });
            return null;
        });
    }

    @Override
    public boolean isRateLimited(@NotNull String playerName, @NotNull Channel channel) {
        String result = null;
        try {
            result = getConnectionAsync(conn -> conn.get(RATE_LIMIT_PREFIX + playerName + channel.getName()))
                    .toCompletableFuture().get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
        return nowMessages >= channel.getRateLimit();//messages higher than limit
    }


    @Override
    public CompletionStage<Boolean> isSpying(@NotNull String playerName) {
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
    public void setSpying(@NotNull String playerName, boolean spy) {
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
    public CompletionStage<Boolean> toggleIgnoring(@NotNull String playerName, @NotNull String ignoringName) {
        return getConnectionAsync(connection ->
                connection.sadd(IGNORE_PREFIX + playerName, ignoringName)
                        .thenApplyAsync(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("02 Toggling ignoring " + ignoringName + " for " + playerName);
                            }
                            if (response == 0) {
                                getConnectionAsync(connection3 -> connection3.srem(IGNORE_PREFIX + playerName, ignoringName))
                                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                                        .exceptionally(exception -> {
                                            exception.printStackTrace();
                                            plugin.getLogger().warning("Error removing ignore");
                                            return null;
                                        });
                                return false;
                            }
                            getConnectionAsync(connection3 -> connection3.expire(IGNORE_PREFIX + playerName, 60 * 60 * 24 * 7))
                                    .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                                    .exceptionally(exception -> {
                                        exception.printStackTrace();
                                        plugin.getLogger().warning("Error when setting ignore expiration");
                                        return null;
                                    });
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
    public CompletionStage<Boolean> isIgnoring(@NotNull String playerName, @NotNull String ignoringName) {
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
    public CompletionStage<List<String>> ignoringList(@NotNull String playerName) {
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
    public void addInventory(@NotNull String name, ItemStack[] inv) {
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_INVENTORY.toString(), name, serialize(inv))
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("05 Added inventory for " + name);
                            }
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
    public void addItem(@NotNull String name, ItemStack item) {
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_ITEM.toString(), name, serialize(item))
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("08 Added item for " + name);
                            }
                            return response;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding item");
                            return null;
                        })
        );
    }

    @Override
    public void addEnderchest(@NotNull String name, ItemStack[] inv) {
        getConnectionAsync(connection ->
                connection.hset(INVSHARE_ENDERCHEST.toString(), name, serialize(inv))
                        .thenApply(response -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("10 Added enderchest for " + name);
                            }
                            return response;
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error adding enderchest");
                            return null;
                        })
        );
    }

    @Override
    public void clearInvShareCache() {
        getConnectionPipeline(connection -> {
            connection.del(INVSHARE_INVENTORY.toString());
            connection.del(INVSHARE_ENDERCHEST.toString());
            connection.del(INVSHARE_ITEM.toString());
            return null;
        });
    }

    @Override
    public CompletionStage<ItemStack> getPlayerItem(@NotNull String playerName) {
        return getConnectionAsync(connection ->
                connection.hget(INVSHARE_ITEM.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("04 Got item for " + playerName + " is " + serializedInv);
                            }
                            ItemStack[] itemStacks = deserialize(serializedInv == null ? "" : serializedInv);
                            if (itemStacks.length == 0) return new ItemStack(Material.AIR);
                            return itemStacks[0];
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting item");
                            return new ItemStack(Material.AIR);
                        })
        );
    }

    @Override
    public CompletionStage<ItemStack[]> getPlayerInventory(@NotNull String playerName) {
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
                            return new ItemStack[0];
                        }));

    }

    @Override
    public CompletionStage<ItemStack[]> getPlayerEnderchest(@NotNull String playerName) {
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
                            return new ItemStack[0];
                        }));
    }

    @Override
    public CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName) {
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
    public CompletionStage<Boolean> setPlayerPrivateMail(@NotNull Mail mail) {
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
    public CompletionStage<Boolean> setPublicMail(@NotNull Mail mail) {
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
    public void registerChannel(@NotNull Channel channel) {
        getConnectionAsync(connection ->
                connection.hset(CHANNELS.toString(), channel.getName(), channel.serialize())
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }


    @Override
    public void unregisterChannel(@NotNull String channelName) {
        getConnectionAsync(connection ->
                connection.hdel(CHANNELS.toString(), channelName)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }

    @Override
    public CompletionStage<List<Channel>> getChannels() {
        return getConnectionAsync(connection ->
                connection.hgetall(CHANNELS.toString())
                        .thenApply(channelMap -> channelMap.values().stream()
                                .map(Channel::deserialize)
                                .toList())
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }

    @Override
    public CompletionStage<@Nullable String> getActivePlayerChannel(@NotNull String playerName, Map<String, Channel> registeredChannels) {
        return getConnectionAsync(conn ->
                conn.hgetall(PLAYER_CHANNELS_PREFIX + playerName)
                        .thenApply(result -> {
                            for (Map.Entry<String, String> channelStatus : result.entrySet()) {
                                if (channelStatus.getValue().equals("1"))
                                    return channelStatus.getKey();
                            }
                            return null;
                        }));
    }

    @Override
    public CompletionStage<List<PlayerChannel>> getPlayerChannelStatuses(@NotNull String playerName, Map<String, Channel> registeredChannels) {
        return getConnectionAsync(connection ->
                connection.hgetall(PLAYER_CHANNELS_PREFIX + playerName)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting player channel");
                            return null;
                        }))
                .thenApply(allPlayerCh -> {//Get all player channels
                    return allPlayerCh.entrySet().stream()//Get all player channels future
                            .filter(entry -> registeredChannels.containsKey(entry.getKey()))//Filter only registered channels
                            .map(entry -> {
                                Channel channel = registeredChannels.get(entry.getKey());
                                return new PlayerChannel(channel,
                                        Integer.parseInt(entry.getValue()));
                            }).toList();
                });
    }

    @Override
    public void setPlayerChannelStatuses(@NotNull String playerName, @NotNull Map<String, String> channelStatuses) {
        getConnectionAsync(connection ->
                connection.hmset(PLAYER_CHANNELS_PREFIX + playerName, channelStatuses)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }

    @Override
    public void removePlayerChannelStatus(@NotNull String playerName, @NotNull String channelName) {
        getConnectionAsync(connection ->
                connection.hdel(PLAYER_CHANNELS_PREFIX + playerName, channelName)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }

    @Override
    public void sendChatMessage(@NotNull ChatMessageInfo packet) {
        getConnectionPipeline(conn -> {
            conn.publish(CHAT_CHANNEL.toString(), packet.serialize())
                    .thenApply(integer -> {
                        if (plugin.config.debug) {
                            plugin.getLogger().warning("#" + (++pubSubIndex) + "received by " + integer + " servers");
                        }
                        return integer;
                    })
                    .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error sending object packet");
                        return 0L;
                    });
            if (packet.isChannel()) {
                String chName = packet.getReceiverName().substring(1);
                conn.incr(RATE_LIMIT_PREFIX + packet.getSenderName() + chName)
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error sending object packet");
                            return 0L;
                        });
                conn.expire(RATE_LIMIT_PREFIX + packet.getSenderName() + chName,
                        plugin.getChannelManager().getRegisteredChannels().containsKey(chName) ?
                                plugin.getChannelManager().getRegisteredChannels().get(chName).getRateLimitPeriod() :
                                5);
            }
            return null;
        });

    }

    @Override
    public void sendRejoin(@NotNull String playerName) {
        getConnectionAsync(connection ->
                connection.publish(REJOIN_CHANNEL.toString(), playerName)
                        .thenApply(integer -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().warning("#" + (++pubSubIndex) + "received by " + integer + " servers");
                            }
                            return integer;
                        })
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error sending rejoin packet");
                            return 0L;
                        })
        );
    }


    @Override
    public void publishPlayerList(@NotNull List<String> playerNames) {
        getConnectionAsync(connection ->
                connection.publish(PLAYERLIST.toString(),
                                String.join("§", playerNames))
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing player list");
                            return 0L;
                        })
        );
    }

    @Override
    public void close() {
        super.close();
    }

}
