package dev.unnm3d.redischat.datamanagers;

import dev.unnm3d.redischat.RedisChat;

public enum DataKey {
    CHAT_CHANNEL("rchat:chat"),
    GLOBAL_CHANNEL("rchat:g_chat"),
    CHANNEL_UPDATE("rchat:ch_update"),
    REJOIN_CHANNEL("rchat:rejoin"),
    PLAYERLIST("rchat:playerlist"),
    MAIL_UPDATE_CHANNEL("rchat:mail_update"),
    PLAYER_ACTIVE_CHANNELS("rchat:pactch"),
    CHANNELS("rchat:ch"),
    PLAYER_CHANNELS_PREFIX("rchat:pch_"),
    ACTIVE_CHANNEL_ID("!activech"),
    IGNORE_PREFIX("rchat:ignore_"),
    MUTED_ENTITIES("rchat:muted"),
    RATE_LIMIT_PREFIX("rchat:ratelimit_"),
    REPLY("rchat:reply"),
    PLAYER_PLACEHOLDERS("rchat:p_ph"),
    PLAYER_PLACEHOLDERS_UPDATE("rchat:p_ph_update"),
    INVSHARE_ITEM("rchat:item"),
    INVSHARE_INVENTORY("rchat:inventory"),
    INVSHARE_ENDERCHEST("rchat:enderchest"),
    SPYING_LIST("rchat:spying"),
    PRIVATE_MAIL_PREFIX("rmail:"),
    PUBLIC_MAIL("rmail:public"),
    MUTED_UPDATE("rchat:m_update"),
    WHITELIST_ENABLED_PLAYERS("rchat:wl_enabled"),
    WHITELIST_ENABLED_UPDATE("rchat:wl_enabled_update"),
    READ_MAIL_MAP("rchat:read_mails:"),
    ;

    private final String keyName;


    /**
     * @param keyName the name of the key
     */
    DataKey(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return RedisChat.getInstance().config.clusterId + keyName;
    }

    public String withoutCluster() {
        return keyName;
    }

}
