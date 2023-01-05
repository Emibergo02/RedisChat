package dev.unnm3d.redischat.redis.redistools;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
public abstract class RedisAbstract {

    private RedisClient lettuceRedisClient;
    private int forcedTimeout;

    public <T> T getConnection(RedisCallBack<T> redisCallBack) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        try {
            return redisCallBack.useConnection(connection);
        } finally {
            CompletableFuture.delayedExecutor(forcedTimeout, java.util.concurrent.TimeUnit.MILLISECONDS).execute(connection::closeAsync);
        }
    }

    public <R> R getBinaryConnection(RedisCallBack.Binary<R> redisCallBack) {
        StatefulRedisConnection<String, Object> connection = lettuceRedisClient.connect(new SerializedObjectCodec());
        try {
            return redisCallBack.useBinaryConnection(connection);
        } finally {
            CompletableFuture.delayedExecutor(forcedTimeout, java.util.concurrent.TimeUnit.MILLISECONDS).execute(connection::closeAsync);
        }
    }

    public <T> CompletableFuture<T> getConnectionAsync(Function<RedisCommands<String, String>, T> redisCallBack) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        connection.setTimeout(Duration.of(forcedTimeout, ChronoUnit.MILLIS));
        return CompletableFuture.supplyAsync(() -> {
            try (connection) {
                return redisCallBack.apply(connection.sync());
            }
        });
    }

    public <R> CompletableFuture<R> getBinaryConnectionAsync(Function<RedisCommands<String, Object>, R> redisCallBack) {
        StatefulRedisConnection<String, Object> connection = lettuceRedisClient.connect(new SerializedObjectCodec());
        connection.setTimeout(Duration.of(forcedTimeout, ChronoUnit.MILLIS));
        return CompletableFuture.supplyAsync(() -> {
            try (connection) {
                return redisCallBack.apply(connection.sync());
            }
        });

    }

    //Get pubsub
    public StatefulRedisPubSubConnection<String, Object> getBinaryPubSubConnection() {
        return lettuceRedisClient.connectPubSub(new SerializedObjectCodec());
    }

    public void getBinaryPubSubConnection(RedisCallBack.PubSub.Binary redisCallBack) {
        redisCallBack.useConnection(lettuceRedisClient.connectPubSub(new SerializedObjectCodec()));
    }

    public void getPubSubConnection(RedisCallBack.PubSub redisCallBack) {
        try (StatefulRedisPubSubConnection<String, String> connection = lettuceRedisClient.connectPubSub()) {
            redisCallBack.useConnection(connection);
        }
    }

    public StatefulRedisConnection<String, String> getUnclosedConnection() {
        return lettuceRedisClient.connect();
    }

    public boolean isConnected() {
        return getConnection(connection -> connection.sync().ping().equals("PONG"));
    }

    public void close() {
        lettuceRedisClient.shutdown();
    }

}
