package dev.unnm3d.redischat.datamanagers;

public enum DataKeys {

    CHAT_CHANNEL("redischat:g_chat"),
    PLAYERLIST("redischat:playerlist"),
    IGNORE_PREFIX("redischat:ignore_"),
    RATE_LIMIT_PREFIX("redischat:ratelimit_"),
    REPLY("redischat:reply"),
    INVSHARE_ITEM("redischat:item"),
    INVSHARE_INVENTORY("redischat:inventory"),
    INVSHARE_ENDERCHEST("redischat:enderchest"),
    SPYING_LIST("redischat:spying"),
    PRIVATE_MAIL_PREFIX("redismail:"),
    PUBLIC_MAIL("redismail:public"),
    ;

    private final String keyName;

    /**
     * @param keyName the name of the key
     */
    DataKeys(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return keyName;
    }

}
