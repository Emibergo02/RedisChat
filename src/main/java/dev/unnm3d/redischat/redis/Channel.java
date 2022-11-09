package dev.unnm3d.redischat.redis;

import dev.unnm3d.redischat.RedisChat;

public enum Channel {
    CHAT("g_chat_" + RedisChat.config.redis.database());

    private final String channelName;

    Channel(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public String toString() {
        return this.channelName;
    }
}
