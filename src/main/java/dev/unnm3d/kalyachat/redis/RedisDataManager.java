package dev.unnm3d.kalyachat.redis;

import dev.unnm3d.ezredislib.EzRedisMessenger;
import dev.unnm3d.jedis.Pipeline;
import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;

public class RedisDataManager {
    private EzRedisMessenger ezRedisMessenger;

    public RedisDataManager(EzRedisMessenger ezRedisMessenger) {
        this.ezRedisMessenger = ezRedisMessenger;
    }

    public Optional<String> getReplyName(String requesterName) {
        return ezRedisMessenger.jedisResource(jedis ->
            Optional.ofNullable(jedis.hget("kalyachat_reply", requesterName))
                ,1000,true);
    }
    public void setReplyName(String nameReceiver,String requesterName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> jedis.hset("kalyachat_reply", nameReceiver, requesterName));
    }
    public boolean isRateLimited(String playerName) {
        return Boolean.TRUE.equals(ezRedisMessenger.jedisResource(jedis -> {
            String result = jedis.get("kalyachat_ratelimit_" + playerName);
            int nowMessages = result == null ? 0 : Integer.parseInt(result);//If null, then 0
            return nowMessages > KalyaChat.config.rate_limit;//messages higher than limit
        },1000,true));

    }
    public void setRateLimit(String playerName,int seconds) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            Pipeline p=jedis.pipelined();
            p.incr("kalyachat_ratelimit_"+playerName);
            p.expire("kalyachat_ratelimit_"+playerName, seconds);
            p.sync();
            return null;
        });
    }
    public void addPlayerName(String playerName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.sadd("kalyachat_playerlist", playerName);
            return null;
        });
    }
    public Set<String> getPlayerList() {
        return ezRedisMessenger.jedisResource(jedis -> jedis.smembers("kalyachat_playerlist") ,1000,true);
    }
    public void removePlayerName(String playerName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {
            jedis.srem("kalyachat_playerlist", playerName);
            return null;
        });
    }
    public void toggleIgnoring(String playerName, String ignoringName) {
        ezRedisMessenger.jedisResourceFuture(jedis -> {

            long response=jedis.sadd("kalyachat_ignore_"+playerName, ignoringName);
            if(response==0) {
                jedis.srem("kalyachat_ignore_" + playerName, ignoringName);
            }else {
                jedis.expire("kalyachat_ignore_" + playerName, 60 * 60 * 24 * 7);
            }
            return null;
        });
    }
    public boolean isIgnoring(String playerName,String ignoringName) {
        Set<String> ignored=ignoringList(playerName);
        return ignored.contains(ignoringName) || ignored.contains("*") || ignored.contains("all");

    }
    public Set<String> ignoringList(String playerName) {
        return ezRedisMessenger.jedisResource(jedis ->
                jedis.smembers("kalyachat_ignore_" + playerName)
        );

    }
    public void listenChatPackets(){
        ezRedisMessenger.registerChannelObjectListener(Channel.CHAT.getChannelName(), (packet) -> {

            ChatPacket chatPacket = (ChatPacket) packet;
            //Check if receiver is online and send priv message to spychat
            if(chatPacket.getReceiverName()!=null){
                boolean realReceiver=false;
                for(Player player:Bukkit.getOnlinePlayers()){
                    if (player.getName().equals(chatPacket.getReceiverName())) {
                        realReceiver=true;
                    }else if(player.hasPermission(Permission.KALYA_CHAT_SPYCHAT.getPermission())){
                        KalyaChat.getInstance().getChatListener().onSpyPrivateChat(chatPacket.getReceiverName(),chatPacket.getSenderName(),player, chatPacket.getMessage());
                    }
                }
                if(!realReceiver)return;
            }

            if (chatPacket.isPrivate()) {
                if(!KalyaChat.getInstance().getRedisDataManager().isIgnoring(chatPacket.getReceiverName(),chatPacket.getSenderName()))//Check ignoring
                    KalyaChat.getInstance().getChatListener().onPrivateChat(chatPacket.getSenderName(), chatPacket.getReceiverName(), chatPacket.getMessage());
            } else {
                KalyaChat.getInstance().getChatListener().onPublicChat(chatPacket.getMessage());
            }

        }, ChatPacket.class);
    }

}
