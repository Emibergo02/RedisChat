package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static dev.unnm3d.redischat.redis.redistools.RedisKeys.PLAYERLIST;

public class PlayerListManager {
    private final BukkitTask task;
    @Getter
    private Set<String> playerList ;
    private final RedisChat plugin;

    public PlayerListManager(RedisChat plugin) {
        this.plugin = plugin;
        this.playerList = Collections.synchronizedSet(new HashSet<>());
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                playerList.clear();
                plugin.getRedisDataManager().getConnectionAsync(connection ->
                        connection.publish(PLAYERLIST.toString(),
                                String.join("§", plugin.getServer().getOnlinePlayers()
                                        .stream().map(HumanEntity::getName).toArray(String[]::new)))
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
                playerList.addAll(Arrays.asList(message.split("§")));
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
