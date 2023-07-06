package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static dev.unnm3d.redischat.redis.redistools.RedisKeys.PLAYERLIST;

public class PlayerListManager {
    private final BukkitTask task;
    @Getter
    private final Set<String> playerList;
    private final RedisChat plugin;

    public PlayerListManager(RedisChat plugin) {
        this.plugin = plugin;
        this.playerList = ConcurrentHashMap.newKeySet();
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                playerList.clear();
                plugin.getServer().getOnlinePlayers().stream()
                        .map(HumanEntity::getName)
                        .filter(s -> !s.isEmpty())
                        .forEach(playerList::add);
                plugin.getRedisDataManager().getConnectionAsync(connection ->
                        connection.publish(PLAYERLIST.toString(),
                                String.join("§", playerList))
                );
            }
        }.runTaskTimerAsynchronously(plugin, 0, 200);
        listenChatPackets();
    }

    public void listenChatPackets() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = plugin.getRedisDataManager().getPubSubConnection();
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                Arrays.asList(message.split("§")).forEach(s -> {
                    if (s != null && !s.isEmpty())
                        playerList.add(s);
                });

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
        pubSubConnection.async().subscribe(PLAYERLIST.toString())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getLogger().warning("Error subscribing to playerlist channel");
                    return null;
                })
                .thenAccept(subscription -> plugin.getLogger().info("Subscribed to channel: " + PLAYERLIST));
    }

    public void stop() {
        task.cancel();
    }

}
