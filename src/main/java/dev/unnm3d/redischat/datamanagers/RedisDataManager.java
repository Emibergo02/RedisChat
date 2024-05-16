package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.api.RedisChatAPI;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.channels.PlayerChannel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.datamanagers.redistools.RedisAbstract;
import dev.unnm3d.redischat.mail.Mail;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class RedisDataManager extends RedisAbstract implements DataManager {
    private final RedisChat plugin;
    public static int pubSubIndex = 0;

    public RedisDataManager(RedisClient redisClient, RedisChat redisChat) {
        super(redisClient, redisChat.config.redis.poolSize() <= 0 ? 1 : redisChat.config.redis.poolSize());
        this.plugin = redisChat;
        registerSub(DataKey.CHAT_CHANNEL.toString(),
                DataKey.GLOBAL_CHANNEL.withoutCluster(),
                DataKey.PLAYERLIST.toString(),
                DataKey.REJOIN_CHANNEL.toString(),
                DataKey.CHANNEL_UPDATE.toString(),
                DataKey.MUTED_UPDATE.toString(),
                DataKey.PLAYER_PLACEHOLDERS.toString(),
                DataKey.WHITELIST_ENABLED_UPDATE.toString(),
                DataKey.MAIL_UPDATE_CHANNEL.toString());
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
        redisURIBuilder = redisChat.config.redis.password().isEmpty() ?
                redisURIBuilder :
                redisChat.config.redis.user().isEmpty() ?
                        redisURIBuilder.withPassword(redisChat.config.redis.password().toCharArray()) :
                        redisURIBuilder.withAuthentication(redisChat.config.redis.user(), redisChat.config.redis.password());
        return new RedisDataManager(RedisClient.create(redisURIBuilder.build()), redisChat);
    }

    @Override
    public void receiveMessage(String channel, String message) {
        if (channel.equals(DataKey.CHAT_CHANNEL.toString())) {
            if (plugin.config.debug) {
                plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
            }
            plugin.getChannelManager().sendAndKeepLocal(ChatMessageInfo.deserialize(message));

        } else if (channel.equals(DataKey.GLOBAL_CHANNEL.withoutCluster())) {
            if (plugin.config.debug) {
                plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
            }
            plugin.getChannelManager().sendAndKeepLocal(ChatMessageInfo.deserialize(message));

        } else if (channel.equals(DataKey.PLAYERLIST.toString())) {
            if (plugin.getPlayerListManager() != null)
                plugin.getPlayerListManager().updatePlayerList(Arrays.asList(message.split("§")));

        } else if (channel.equals(DataKey.REJOIN_CHANNEL.toString())) {
            if (plugin.getJoinQuitManager() != null)
                plugin.getJoinQuitManager().rejoinRequest(message);

        } else if (channel.equals(DataKey.CHANNEL_UPDATE.toString())) {
            if (message.startsWith("delete§")) {
                plugin.getChannelManager().updateChannel(message.substring(7), null);
            } else {
                Channel ch = Channel.deserialize(message);
                plugin.getChannelManager().updateChannel(ch.getName(), ch);
            }

        } else if (channel.equals(DataKey.MAIL_UPDATE_CHANNEL.toString())) {
            plugin.getMailGUIManager().receiveMailUpdate(message);
        } else if (channel.equals(DataKey.MUTED_UPDATE.toString())) {
            plugin.getChannelManager().getMuteManager().serializedUpdate(message);
        } else if (channel.equals(DataKey.PLAYER_PLACEHOLDERS.toString())) {
            plugin.getPlaceholderManager().updatePlayerPlaceholders(message);
        } else if (channel.equals(DataKey.WHITELIST_ENABLED_UPDATE.toString())) {
            if (message.startsWith("D§")) {
                plugin.getChannelManager().getMuteManager().whitelistEnabledUpdate(message.substring(2), false);
            } else {
                plugin.getChannelManager().getMuteManager().whitelistEnabledUpdate(message, true);
            }
        }
    }

    @Override
    public CompletionStage<Optional<String>> getReplyName(@NotNull String requesterName) {
        if (plugin.config.debug) {
            plugin.getLogger().info("Getting reply name for " + requesterName);
        }
        return getConnectionAsync(connection -> connection.hget(DataKey.REPLY.toString(), requesterName))
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
            conn.hset(DataKey.REPLY.toString(), nameReceiver, requesterName)
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error when setting reply name");
                        return null;
                    });
            conn.hset(DataKey.REPLY.toString(), requesterName, nameReceiver)
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error when setting reply name");
                        return null;
                    });
            return null;
        });
    }

    @Override
    public CompletionStage<Map<String, String>> getPlayerPlaceholders(@NotNull String playerName) {
        return getConnectionAsync(connection ->
                connection.hget(DataKey.PLAYER_PLACEHOLDERS.toString(), playerName)
                        .thenApply(serializedPlaceholders -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("Getting placeholders for " + playerName + " is " + serializedPlaceholders);
                            }
                            return serializedPlaceholders == null ? null : deserializePlayerPlaceholders(serializedPlaceholders);
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting placeholders from redis");
                            return null;
                        }));
    }

    @Override
    public void setPlayerPlaceholders(@NotNull String playerName, @NotNull Map<String, String> placeholders) {
        getConnectionAsync(connection ->
                connection.hset(DataKey.PLAYER_PLACEHOLDERS.toString(), playerName, serializePlayerPlaceholders(placeholders))
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error setting placeholders to redis");
                            return null;
                        }));
    }

    @Override
    public boolean isRateLimited(@NotNull String playerName, @NotNull Channel channel) {
        String result = null;
        try {
            result = getConnectionAsync(conn -> conn.get(DataKey.RATE_LIMIT_PREFIX + playerName + channel.getName()))
                    .toCompletableFuture().get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.getLogger().warning("Error getting rate limit from redis: " + e.getMessage());
        }

        int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
        return nowMessages >= channel.getRateLimit();//messages higher than limit
    }


    @Override
    public CompletionStage<Boolean> isSpying(@NotNull String playerName) {
        return getConnectionAsync(connection ->
                connection.sismember(DataKey.SPYING_LIST.toString(), playerName)
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
                        return connection.sadd(DataKey.SPYING_LIST.toString(), playerName)
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
                        return connection.srem(DataKey.SPYING_LIST.toString(), playerName)
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
    public void addInventory(@NotNull String name, ItemStack[] inv) {
        getConnectionAsync(connection ->
                connection.hset(DataKey.INVSHARE_INVENTORY.toString(), name, serialize(inv))
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
                connection.hset(DataKey.INVSHARE_ITEM.toString(), name, serialize(item))
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
                connection.hset(DataKey.INVSHARE_ENDERCHEST.toString(), name, serialize(inv))
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
            connection.del(DataKey.INVSHARE_INVENTORY.toString());
            connection.del(DataKey.INVSHARE_ENDERCHEST.toString());
            connection.del(DataKey.INVSHARE_ITEM.toString());
            return null;
        });
    }

    @Override
    public CompletionStage<ItemStack> getPlayerItem(@NotNull String playerName) {
        return getConnectionAsync(connection ->
                connection.hget(DataKey.INVSHARE_ITEM.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("04 Got item for " + playerName + " is " + serializedInv);
                            }
                            ItemStack[] itemStacks = serializedInv == null || serializedInv.isEmpty() ? new ItemStack[0] : deserialize(serializedInv);
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
                connection.hget(DataKey.INVSHARE_INVENTORY.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("12 Got inventory for " + playerName + " is " + (serializedInv == null ? "null" : serializedInv));
                            }
                            return serializedInv == null || serializedInv.isEmpty() ? new ItemStack[0] : deserialize(serializedInv);
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
                connection.hget(DataKey.INVSHARE_ENDERCHEST.toString(), playerName)
                        .thenApply(serializedInv -> {
                            if (plugin.config.debug) {
                                plugin.getLogger().info("13 Got enderchest for " + playerName + " is " + (serializedInv == null ? "null" : serializedInv));
                            }
                            return serializedInv == null || serializedInv.isEmpty() ? new ItemStack[0] : deserialize(serializedInv);
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting ec");
                            return new ItemStack[0];
                        }));
    }

    @Override
    public CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> executeTransaction(commands -> {
                    commands.hgetall(DataKey.PRIVATE_MAIL_PREFIX + playerName);
                    commands.smembers(DataKey.READ_MAIL_MAP + playerName);
                }).map(result -> {
                    final List<Mail> mails = deserializeMails((Map<String, String>) result.get(0));
                    final Set<Double> readMailIds = ((Set<String>) result.get(1)).stream()
                            .map(Double::parseDouble)
                            .collect(Collectors.toSet());
                    mails.forEach(mail -> mail.setRead(readMailIds.contains(mail.getId())));
                    return mails;
                }).orElse(List.of()),
                plugin.getExecutorService());
    }

    @Override
    public CompletionStage<Boolean> setPlayerPrivateMail(@NotNull Mail mail) {
        return getConnectionPipeline(connection -> {
            connection.publish(DataKey.MAIL_UPDATE_CHANNEL.toString(), mail.serializeWithId());
            connection.hset(DataKey.PRIVATE_MAIL_PREFIX + mail.getReceiver(), String.valueOf(mail.getId()), mail.serialize());
            mail.setCategory(Mail.MailCategory.SENT);
            return connection.hset(DataKey.PRIVATE_MAIL_PREFIX + mail.getSender(), String.valueOf(mail.getId()), mail.serialize()).exceptionally(throwable -> {
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
        return getConnectionPipeline(connection -> {
            connection.publish(DataKey.MAIL_UPDATE_CHANNEL.toString(), mail.serializeWithId());
            return connection.hset(DataKey.PUBLIC_MAIL.toString(), String.valueOf(mail.getId()), mail.serialize()).exceptionally(throwable -> {
                throwable.printStackTrace();
                plugin.getLogger().warning("Error setting public mail");
                return null;
            });
        });
    }

    @Override
    public CompletableFuture<List<Mail>> getPublicMails(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> executeTransaction(commands -> {
                    commands.hgetall(DataKey.PUBLIC_MAIL.toString());
                    commands.smembers(DataKey.READ_MAIL_MAP + playerName);
                }).map(result -> {
                    final List<Mail> mails = deserializeMails((Map<String, String>) result.get(0));
                    final Set<Double> readMailIds = ((Set<String>) result.get(1)).stream()
                            .map(Double::parseDouble)
                            .collect(Collectors.toSet());
                    mails.forEach(mail -> mail.setRead(readMailIds.contains(mail.getId())));
                    return mails;
                }).orElse(List.of()),
                plugin.getExecutorService());
    }

    @Override
    public void setMailRead(@NotNull String playerName, @NotNull Mail mail) {
        getConnectionAsync(connection -> mail.isRead() ? connection.sadd(DataKey.READ_MAIL_MAP + playerName, String.valueOf(mail.getId()))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error setting mail read");
                    return null;
                }) : connection.srem(DataKey.READ_MAIL_MAP + playerName, String.valueOf(mail.getId()))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error setting mail read");
                    return null;
                }));
    }

    @Override
    public void deleteMail(@NotNull Mail mail) {
        getConnectionPipeline(connection -> {
            if (mail.getCategory() == Mail.MailCategory.PUBLIC) {
                return connection.hdel(DataKey.PUBLIC_MAIL.toString(), String.valueOf(mail.getId()));
            } else {
                connection.hdel(DataKey.PRIVATE_MAIL_PREFIX + mail.getSender(), String.valueOf(mail.getId()));
                return connection.hdel(DataKey.PRIVATE_MAIL_PREFIX + mail.getReceiver(), String.valueOf(mail.getId()));
            }
        });
    }

    @Override
    public void registerChannel(@NotNull Channel channel) {
        getConnectionPipeline(connection -> {
            connection.hset(DataKey.CHANNELS.toString(), channel.getName(), channel.serialize()).exceptionally(throwable -> {
                throwable.printStackTrace();
                plugin.getLogger().warning("Error registering custom channel");
                return null;
            });
            connection.publish(DataKey.CHANNEL_UPDATE.toString(), channel.serialize());
            return null;
        });
    }


    @Override
    public void unregisterChannel(@NotNull String channelName) {
        getConnectionPipeline(connection -> {
            connection.hdel(DataKey.CHANNELS.toString(), channelName).exceptionally(throwable -> {
                throwable.printStackTrace();
                plugin.getLogger().warning("Error registering custom channel");
                return null;
            });
            connection.publish(DataKey.CHANNEL_UPDATE.toString(), "delete§" + channelName);
            return null;
        });
    }

    @Override
    public CompletionStage<List<Channel>> getChannels() {
        return getConnectionAsync(connection ->
                connection.hgetall(DataKey.CHANNELS.toString())
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
                conn.hgetall(DataKey.PLAYER_CHANNELS_PREFIX + playerName)
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
                connection.hgetall(DataKey.PLAYER_CHANNELS_PREFIX + playerName)
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
                connection.hmset(DataKey.PLAYER_CHANNELS_PREFIX + playerName, channelStatuses)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }

    @Override
    public void removePlayerChannelStatus(@NotNull String playerName, @NotNull String channelName) {
        getConnectionAsync(connection ->
                connection.hdel(DataKey.PLAYER_CHANNELS_PREFIX + playerName, channelName)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error registering custom channel");
                            return null;
                        }));
    }

    @Override
    public void setMutedEntities(@NotNull String keyEntity, @NotNull Set<String> valueEntities) {
        getConnectionAsync(connection -> {
            if (valueEntities.isEmpty()) {
                connection.hdel(DataKey.MUTED_ENTITIES.toString(), keyEntity);
                return connection.publish(DataKey.MUTED_UPDATE.toString(), keyEntity + ";");
            }

            plugin.getLogger().info("Setting muted channels for " + keyEntity + " to " + valueEntities);

            final String serializedUpdate = String.join(",", valueEntities);
            connection.hset(DataKey.MUTED_ENTITIES.toString(), Map.of(keyEntity, serializedUpdate));
            return connection.publish(DataKey.MUTED_UPDATE.toString(), keyEntity + ";" + serializedUpdate);
        });
    }

    @Override
    public CompletionStage<Map<String, Set<String>>> getAllMutedEntities() {
        return getConnectionAsync(connection ->
                connection.hgetall(DataKey.MUTED_ENTITIES.toString())
                        .thenApply(serializedUpdate -> {
                            if (serializedUpdate == null) return new HashMap<String, Set<String>>();
                            final Map<String, Set<String>> map = new HashMap<>();
                            serializedUpdate.entrySet().stream()
                                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                                            Arrays.stream(entry.getValue().split(","))
                                                    .collect(Collectors.toCollection(HashSet::new))))
                                    .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
                            return map;
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting muted channels");
                            return null;
                        }));
    }

    @Override
    public CompletionStage<Set<String>> getWhitelistEnabledPlayers() {
        return getConnectionAsync(connection ->
                connection.smembers(DataKey.WHITELIST_ENABLED_PLAYERS.toString())
                        .thenApply(serializedSet -> {
                            if (serializedSet == null) return new HashSet<String>();
                            return serializedSet;
                        })
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error getting whitelist enabled players");
                            return null;
                        }));
    }

    @Override
    public void setWhitelistEnabledPlayer(@NotNull String playerName, boolean enabled) {
        getConnectionAsync(connection -> {
            if (enabled) {
                connection.sadd(DataKey.WHITELIST_ENABLED_PLAYERS.toString(), playerName)
                        .exceptionally(throwable -> {
                            throwable.printStackTrace();
                            plugin.getLogger().warning("Error setting whitelist enabled player");
                            return null;
                        });
                return connection.publish(DataKey.WHITELIST_ENABLED_UPDATE.toString(), playerName);
            }
            connection.srem(DataKey.WHITELIST_ENABLED_PLAYERS.toString(), playerName)
                    .exceptionally(throwable -> {
                        throwable.printStackTrace();
                        plugin.getLogger().warning("Error setting whitelist enabled player");
                        return null;
                    });
            return connection.publish(DataKey.WHITELIST_ENABLED_UPDATE.toString(), "D§" + playerName);

        });
    }

    @Override
    public void sendChatMessage(@NotNull ChatMessageInfo packet) {
        getConnectionPipeline(conn -> {
            String publishChannel = DataKey.CHAT_CHANNEL.toString();
            if (packet.getReceiver().isChannel()) {//If it's a channel message we need to increment the rate limit
                final String chName = packet.getReceiver().getName();

                if (chName.equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString()))
                    publishChannel = DataKey.GLOBAL_CHANNEL.withoutCluster();//Exception for staffchat: it's a global channel

                conn.incr(DataKey.RATE_LIMIT_PREFIX + packet.getSender().getName() + chName)
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error sending object packet");
                            return 0L;
                        });
                conn.expire(DataKey.RATE_LIMIT_PREFIX + packet.getSender().getName() + chName,
                        plugin.getChannelManager().getChannel(chName)
                                .orElse(plugin.getChannelManager().getPublicChannel(null))
                                .getRateLimitPeriod());
            }

            conn.publish(publishChannel, packet.serialize())
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
            return null;
        });

    }

    @Override
    public void sendRejoin(@NotNull String playerName) {
        getConnectionAsync(connection ->
                connection.publish(DataKey.REJOIN_CHANNEL.toString(), playerName)
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
                connection.publish(DataKey.PLAYERLIST.toString(),
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

    private List<Mail> deserializeMails(Map<String, String> timestampMail) {
        return Optional.ofNullable(RedisChatAPI.getAPI())
                .map(RedisChatAPI::getMailManager)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(mailGUIManager -> timestampMail.entrySet().stream()
                        // From string to Mail and sort by timestamp
                        .map(entry -> new AbstractMap.SimpleEntry<>(Double.parseDouble(entry.getKey()), entry.getValue()))
                        .sorted(Map.Entry.comparingByKey(
                                Comparator.reverseOrder()
                        ))
                        .map(entry -> new Mail(mailGUIManager, entry.getKey(), entry.getValue()))
                        .toList())
                .orElse(new ArrayList<>());

    }

}
