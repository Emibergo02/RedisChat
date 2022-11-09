package dev.unnm3d.redischat.redis;

import dev.unnm3d.ezredislib.EzRedisMessenger;
import dev.unnm3d.jedis.Pipeline;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;

public class RedisDataManager {
    private final EzRedisMessenger ezRedisMessenger;

    public RedisDataManager(EzRedisMessenger ezRedisMessenger) {
        this.ezRedisMessenger = ezRedisMessenger;
    }

    public Optional<String> getReplyName(String requesterName) {
        return Optional.ofNullable(ezRedisMessenger.jedisResource(jedis ->
                        jedis.hget("redischat_reply", requesterName)
                , 1000, true));
    }

    public void setReplyName(String nameReceiver, String requesterName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> jedis.hset("redischat_reply", nameReceiver, requesterName));
    }

    public boolean isRateLimited(String playerName) {
        return Boolean.TRUE.equals(ezRedisMessenger.jedisResource(jedis -> {
            String result = jedis.get("redischat_ratelimit_" + playerName);
            int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
            return nowMessages >= RedisChat.config.rate_limit;//messages higher than limit
        }, 100, true));

    }

    public void setRateLimit(String playerName, int seconds) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            Pipeline p = jedis.pipelined();
            p.incr("redischat_ratelimit_" + playerName);
            p.expire("redischat_ratelimit_" + playerName, seconds);
            p.sync();
            p.close();
            return null;
        });
    }

    public void addPlayerName(String playerName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.sadd("redischat_playerlist", playerName);
            return null;
        });
    }

    public Set<String> getPlayerList() {
        return ezRedisMessenger.jedisResource(jedis -> jedis.smembers("redischat_playerlist"), 1000, true);
    }

    public void removePlayerName(String playerName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.srem("redischat_playerlist", playerName);
            return null;
        });
    }

    public void toggleIgnoring(String playerName, String ignoringName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {

            long response = jedis.sadd("redischat_ignore_" + playerName, ignoringName);
            if (response == 0) {
                jedis.srem("redischat_ignore_" + playerName, ignoringName);
            } else {
                jedis.expire("redischat_ignore_" + playerName, 60 * 60 * 24 * 7);
            }
            return null;
        });
    }

    public boolean isIgnoring(String playerName, String ignoringName) {
        Set<String> ignored = ignoringList(playerName);
        return ignored.contains(ignoringName) || ignored.contains("*") || ignored.contains("all");

    }

    public Set<String> ignoringList(String playerName) {
        return ezRedisMessenger.jedisResource(jedis ->
                jedis.smembers("redischat_ignore_" + playerName)
        );

    }

    public void listenChatPackets() {
        ezRedisMessenger.registerChannelObjectListener(Channel.CHAT.getChannelName(), (packet) -> {

            ChatPacket chatPacket = (ChatPacket) packet;
            //Check if receiver is online and send priv message to spychat
            if (chatPacket.getReceiverName() != null) {
                boolean realReceiver = false;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().equals(chatPacket.getReceiverName())) {
                        realReceiver = true;
                    } else if (player.hasPermission(Permission.REDIS_CHAT_SPYCHAT.getPermission())) {
                        RedisChat.getInstance().getChatListener().onSpyPrivateChat(chatPacket.getReceiverName(), chatPacket.getSenderName(), player, chatPacket.getMessage());
                    }
                }
                if (!realReceiver) return;
            }

            if (chatPacket.isPrivate()) {
                if (!RedisChat.getInstance().getRedisDataManager().isIgnoring(chatPacket.getReceiverName(), chatPacket.getSenderName()))//Check ignoring
                    RedisChat.getInstance().getChatListener().onPrivateChat(chatPacket.getSenderName(), chatPacket.getReceiverName(), chatPacket.getMessage());
            } else {
                RedisChat.getInstance().getChatListener().onPublicChat(chatPacket.getMessage());
            }

        }, ChatPacket.class);
    }

}
