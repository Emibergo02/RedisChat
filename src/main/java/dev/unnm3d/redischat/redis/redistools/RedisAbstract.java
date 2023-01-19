package dev.unnm3d.redischat.redis.redistools;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

@AllArgsConstructor
public abstract class RedisAbstract {
    protected static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    protected RedisClient lettuceRedisClient;


    public <T> T getConnectionSync(RedisCallBack<T> redisCallBack) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        T returned = redisCallBack.useConnection(connection);
        connection.closeAsync();
        return returned;
    }

    public <T> ScheduledFuture<T> scheduleConnection(Function<StatefulRedisConnection<String, String>, T> function, int timeout, TimeUnit timeUnit) {
        return executorService.schedule(() -> {
            try (StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect()) {
                return function.apply(connection);
            }
        }, timeout, timeUnit);
    }

    public <T> CompletionStage<T> getConnectionAsync(Function<RedisAsyncCommands<String, String>, CompletionStage<T>> redisCallBack) {
        StatefulRedisConnection<String, String> connection = lettuceRedisClient.connect();
        CompletionStage<T> returnable = redisCallBack.apply(connection.async());
        return returnable.thenApply(t -> {
            connection.close();
            return t;
        });
    }


    public StatefulRedisConnection<String, String> getUnclosedConnection() {
        return lettuceRedisClient.connect();
    }


    public void close() {
        lettuceRedisClient.shutdown(Duration.ofSeconds(3), Duration.ZERO);
        executorService.shutdown();
    }

}
