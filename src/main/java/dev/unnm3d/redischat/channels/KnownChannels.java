package dev.unnm3d.redischat.channels;

public enum KnownChannels {
    BROADCAST("public"),
    STAFFCHAT("!"),
    PRIVATE_MESSAGE_PREFIX("$"),
    ;

    private final String keyName;

    /**
     * @param keyName the name of the key
     */
    KnownChannels(final String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String toString() {
        return keyName;
    }
}
