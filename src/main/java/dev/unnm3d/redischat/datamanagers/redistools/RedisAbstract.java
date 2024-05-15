package dev.unnm3d.redischat.datamanagers.redistools;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;


public abstract class RedisAbstract {
    private final RoundRobinConnectionPool<String, String> roundRobinConnectionPool;
    private final ConcurrentHashMap<String[], StatefulRedisPubSubConnection<String, String>> pubSubConnections;
    protected RedisClient lettuceRedisClient;

    public RedisAbstract(RedisClient lettuceRedisClient, int poolSize) {
        this.lettuceRedisClient = lettuceRedisClient;
        this.roundRobinConnectionPool = new RoundRobinConnectionPool<>(lettuceRedisClient::connect, poolSize);
        this.pubSubConnections = new ConcurrentHashMap<>();
    }

    public abstract void receiveMessage(String channel, String message);

    protected void registerSub(String... listenedChannels) {
        if (listenedChannels.length == 0) {
            return;
        }
        final StatefulRedisPubSubConnection<String, String> pubSubConnection = lettuceRedisClient.connectPubSub();
        pubSubConnections.put(listenedChannels, pubSubConnection);
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                receiveMessage(channel, message);
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
        pubSubConnection.async().subscribe(listenedChannels)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    public <T> CompletionStage<T> getConnectionAsync(Function<RedisAsyncCommands<String, String>, CompletionStage<T>> redisCallBack) {
        return redisCallBack.apply(roundRobinConnectionPool.get().async());
    }

    public <T> CompletionStage<T> getConnectionPipeline(Function<RedisAsyncCommands<String, String>, CompletionStage<T>> redisCallBack) {
        StatefulRedisConnection<String, String> connection = roundRobinConnectionPool.get();
        connection.setAutoFlushCommands(false);
        CompletionStage<T> completionStage = redisCallBack.apply(connection.async());
        connection.flushCommands();
        connection.setAutoFlushCommands(true);
        return completionStage;
    }

    public Optional<List<Object>> executeTransaction(Consumer<RedisCommands<String, String>> redisCommandsConsumer) {
        final RedisCommands<String, String> syncCommands = roundRobinConnectionPool.get().sync();
        syncCommands.multi();
        redisCommandsConsumer.accept(syncCommands);
        final TransactionResult transactionResult = syncCommands.exec();
        return Optional.ofNullable(transactionResult.wasDiscarded() ? null : transactionResult.stream().toList());
    }

    public void close() {
        pubSubConnections.values().forEach(StatefulRedisPubSubConnection::close);
        Bukkit.getLogger().info("Closing pubsub connection");
        lettuceRedisClient.shutdown();
        Bukkit.getLogger().info("Lettuce shutdown connection");
    }


}
