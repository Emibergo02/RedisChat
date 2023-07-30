package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.integrations.VanishIntegration;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static dev.unnm3d.redischat.redis.redistools.RedisKeys.PLAYERLIST;

public class PlayerListManager {
    private final BukkitTask task;
    private final ConcurrentHashMap<String, Long> playerList;
    private final List<VanishIntegration> vanishIntegrations;
    private final RedisChat plugin;

    public PlayerListManager(RedisChat plugin) {
        this.plugin = plugin;
        this.playerList = new ConcurrentHashMap<>();
        this.vanishIntegrations = new ArrayList<>();
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                playerList.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 1000 * 6);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> vanishIntegrations.stream().noneMatch(integration -> integration.isVanished(player)))
                        .map(HumanEntity::getName)
                        .filter(s -> !s.isEmpty())
                        .forEach(name -> playerList.put(name, System.currentTimeMillis()));
                plugin.getRedisDataManager().getConnectionAsync(connection ->
                        connection.publish(PLAYERLIST.toString(),
                                String.join("§", playerList.keySet().stream().toList()))
                );
            }
        }.runTaskTimerAsynchronously(plugin, 0, 100);//5 seconds
        listenPlayerListUpdate();
    }

    public void listenPlayerListUpdate() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = plugin.getRedisDataManager().getPubSubConnection();
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                Arrays.asList(message.split("§")).forEach(s -> {
                    if (s != null && !s.isEmpty())
                        playerList.put(s, System.currentTimeMillis());
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

    public void addVanishIntegration(VanishIntegration vanishIntegration) {
        vanishIntegrations.add(vanishIntegration);
    }

    public Set<String> getPlayerList() {
        return playerList.keySet();
    }

    public void stop() {
        task.cancel();
    }

}
