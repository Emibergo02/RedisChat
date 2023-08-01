package dev.unnm3d.redischat.datamanagers.redistools;


import io.lettuce.core.api.StatefulRedisConnection;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class RoundRobinConnectionPool<K, V> {
    private final AtomicInteger next = new AtomicInteger(0);
    private final StatefulRedisConnection<K, V>[] elements;
    private final Supplier<StatefulRedisConnection<K, V>> statefulRedisConnectionSupplier;

    @SuppressWarnings("unchecked")
    public RoundRobinConnectionPool(Supplier<StatefulRedisConnection<K, V>> statefulRedisConnectionSupplier, int poolSize) {
        this.statefulRedisConnectionSupplier = statefulRedisConnectionSupplier;
        this.elements = new StatefulRedisConnection[poolSize];
        for (int i = 0; i < poolSize; i++) {
            elements[i] = statefulRedisConnectionSupplier.get();
        }
    }

    public StatefulRedisConnection<K, V> get() {
        int index = next.getAndIncrement() % elements.length;
        StatefulRedisConnection<K, V> connection = elements[index];
        if (connection != null)
            if (connection.isOpen())
                if (connection.isMulti()) {
                    return get();
                } else
                    return connection;

        connection = statefulRedisConnectionSupplier.get();
        elements[index] = connection;
        return connection;
    }

}