package dev.unnm3d.kalyachat.redis;

import dev.unnm3d.kalyachat.KalyaChat;

public enum Channel {
    CHAT("g_chat_"+ KalyaChat.config.redis.database());

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
