package dev.unnm3d.redischat.redis;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.redis.redistools.RedisAbstract;
import dev.unnm3d.redischat.redis.redistools.RedisKeys;
import dev.unnm3d.redischat.redis.redistools.RedisPubSub;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public class RedisDataManager extends RedisAbstract {
    private final RedisChat plugin;

    public RedisDataManager(RedisClient redisClient, RedisChat redisChat) {
        super(redisClient, 3000);
        this.plugin = redisChat;
    }

    public Optional<String> getReplyName(String requesterName) {
        return getConnection(redisCommands ->
                Optional.ofNullable(redisCommands.sync().hget("redischat_reply", requesterName)));
    }

    public void setReplyName(String nameReceiver, String requesterName) {
        getConnection(redisCommands ->
                redisCommands.sync().hset("redischat_reply", nameReceiver, requesterName));
    }

    public boolean isRateLimited(String playerName) {
        return getConnection(redisCommands -> {
            String result = redisCommands.sync().get("redischat_ratelimit_" + playerName);
            int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
            return nowMessages >= RedisChat.config.rate_limit;//messages higher than limit
        });

    }

    public void setRateLimit(String playerName, int seconds) {
        getConnection(redisCommands -> {
            redisCommands.setAutoFlushCommands(false);
            RedisAsyncCommands<String, String> rac = redisCommands.async();

            rac.incr("redischat_ratelimit_" + playerName);
            rac.expire("redischat_ratelimit_" + playerName, seconds);
            redisCommands.flushCommands();
            return null;
        });
    }

    public void addPlayerName(String playerName) {
        getConnectionAsync(redisCommands -> {
            redisCommands.sadd("redischat_playerlist", playerName);
            return null;
        });
    }

    public Set<String> getPlayerList() {
        return getConnection(redisCommands -> redisCommands.sync().smembers("redischat_playerlist"));
    }

    public void removePlayerName(String playerName) {
        getConnectionAsync(redisCommands -> {
            redisCommands.srem("redischat_playerlist", playerName);
            return null;
        });
    }

    public void toggleIgnoring(String playerName, String ignoringName) {
        getConnectionAsync(redisCommands -> {
            long response = redisCommands.sadd("redischat_ignore_" + playerName, ignoringName);
            if (response == 0) {
                redisCommands.srem("redischat_ignore_" + playerName, ignoringName);
            } else {
                redisCommands.expire("redischat_ignore_" + playerName, 60 * 60 * 24 * 7);
            }
            return null;
        });
    }

    public boolean isIgnoring(String playerName, String ignoringName) {
        Set<String> ignored = ignoringList(playerName);
        return ignored.contains(ignoringName) || ignored.contains("*") || ignored.contains("all");

    }

    public Set<String> ignoringList(String playerName) {
        return getConnection(redisCommands -> redisCommands.sync().smembers("redischat_ignore_" + playerName));
    }

    public void listenChatPackets() {
        getBinaryPubSubConnection(callback -> {
            callback.addListener(new RedisPubSub<>() {
                @Override
                public void message(String channel, Object message) {
                    ChatPacket chatPacket = (ChatPacket) message;
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
                        if (!plugin.getRedisDataManager().isIgnoring(chatPacket.getReceiverName(), chatPacket.getSenderName()))//Check ignoring
                            plugin.getChatListener().onPrivateChat(chatPacket.getSenderName(), chatPacket.getReceiverName(), chatPacket.getMessage());
                    } else {
                        plugin.getChatListener().onPublicChat(chatPacket.getMessage());
                    }
                }
            });
            callback.async().subscribe(RedisKeys.CHAT.toString());
        });
    }

    public void sendObjectPacket(Object object) {
        getBinaryConnectionAsync(callback -> callback.publish(RedisKeys.CHAT.toString(), object));
    }

    public String getRedisDatabase() {
        return new File(System.getProperty("user.dir")).getName();
    }

}
