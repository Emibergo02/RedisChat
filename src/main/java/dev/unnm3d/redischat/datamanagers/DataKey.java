package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.RedisChat;

public enum DataKey {
    CHAT_CHANNEL("rchat:chat"),
    GLOBAL_CHANNEL("rchat:g_chat"),
    CHANNEL_UPDATE("rchat:ch_update"),
    REJOIN_CHANNEL("rchat:rejoin"),
    PLAYERLIST("rchat:playerlist"),
    PLAYER_ACTIVE_CHANNELS("rchat:pactch"),
    CHANNELS("rchat:ch"),
    PLAYER_CHANNELS_PREFIX("rchat:pch_"),
    ACTIVE_CHANNEL_ID("!activech"),
    IGNORE_PREFIX("rchat:ignore_"),
    RATE_LIMIT_PREFIX("rchat:ratelimit_"),
    REPLY("rchat:reply"),
    INVSHARE_ITEM("rchat:item"),
    INVSHARE_INVENTORY("rchat:inventory"),
    INVSHARE_ENDERCHEST("rchat:enderchest"),
    SPYING_LIST("rchat:spying"),
    PRIVATE_MAIL_PREFIX("rmail:"),
    PUBLIC_MAIL("rmail:public"),

    ;

    private final String keyName;
    private static final String CLUSTER_ID = RedisChat.getInstance().config.clusterId;


    /**
     * @param keyName the name of the key
     */
    DataKey(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return CLUSTER_ID + keyName;
    }

    public String withoutCluster() {
        return keyName;
    }

}
