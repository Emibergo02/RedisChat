package dev.unnm3d.redischat.redis.redistools;

import dev.unnm3d.redischat.RedisChat;

public enum RedisKeys {

    CHAT("g_chat_" + RedisChat.getInstance().getRedisDataManager().getRedisDatabase());

    private final String keyName;

    /**
     * @param keyName the name of the key
     */
    RedisKeys(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return keyName;
    }

}
