package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.RedisChat;

public enum DataKeys {
    CHAT_CHANNEL("rchat:g_chat"),
    REJOIN_CHANNEL("rchat:rejoin"),
    PLAYERLIST("rchat:playerlist"),
    CHANNELS("rchat:ch"),
    //New player key
    PLAYER_CHANNELS_PREFIX("rchat:pch_"),
    PLAYER_ACTIVE_CHANNELS("rchat:pactch"),
    ACTIVE_CHANNEL_ID("!activech"),
    //New player key
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
    private static final int CLUSTER_ID = RedisChat.getInstance().config.clusterId;


    /**
     * @param keyName the name of the key
     */
    DataKeys(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return CLUSTER_ID + keyName;
    }

}
