package dev.unnm3d.redischat.chat;

public enum KnownChatEntities {
    BROADCAST("*"),
    SERVER_SENDER("*Server*"),
    PERMISSION_MULTICAST("@"),
    ;

    private final String keyName;

    /**
     * @param keyName the name of the key
     */
    KnownChatEntities(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return keyName;
    }
}
