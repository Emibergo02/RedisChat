package dev.unnm3d.redischat.chat;

import lombok.Getter;

public class ChatActor {

    @Getter
    private final String name;
    private final ActorType type;

    /**
     * Creates a ChatActor from a serialized string
     *
     * @param serializedString The serialized string
     */
    ChatActor(String serializedString) {
        if (serializedString.startsWith(KnownChatEntities.CHANNEL_PREFIX.toString())) {
            this.name = serializedString.substring(KnownChatEntities.CHANNEL_PREFIX.toString().length());
            this.type = ActorType.CHANNEL;
        } else if (serializedString.startsWith(KnownChatEntities.DISCORD_PREFIX.toString())) {
            this.name = serializedString.substring(KnownChatEntities.DISCORD_PREFIX.toString().length());
            this.type = ActorType.DISCORD;
        } else if (serializedString.startsWith(KnownChatEntities.PERMISSION_PREFIX.toString())) {
            this.name = serializedString.substring(KnownChatEntities.PERMISSION_PREFIX.toString().length());
            this.type = ActorType.PERMISSION;
        } else if (serializedString.equals(KnownChatEntities.SERVER_SENDER.toString())) {
            this.name = serializedString;
            this.type = ActorType.SERVER;
        } else {
            this.name = serializedString;
            this.type = ActorType.PLAYER;
        }
    }

    /**
     * Creates a ChatActor from a name and a type
     *
     * @param name The name of the actor
     * @param type The type of the actor
     */
    public ChatActor(String name, ActorType type) {
        this.name = type == ActorType.SERVER ?
                KnownChatEntities.SERVER_SENDER.toString() :
                name;
        this.type = type;
    }

    /**
     * Creates a "server" ChatActor
     */
    public ChatActor() {
        this.name = KnownChatEntities.SERVER_SENDER.toString();
        this.type = ActorType.SERVER;
    }


    public boolean isPlayer() {
        return !(isChannel() || isDiscord() || needPermission());
    }

    public boolean isChannel() {
        return type == ActorType.CHANNEL;
    }

    public boolean needPermission() {
        return type == ActorType.PERMISSION;
    }

    public boolean isDiscord() {
        return type == ActorType.DISCORD;
    }

    public boolean isServer() {
        return type == ActorType.SERVER;
    }

    /**
     * Serializes the ChatActor
     *
     * @return The serialized string
     */
    String serialize() {
        return switch (type) {
            case CHANNEL -> KnownChatEntities.CHANNEL_PREFIX + name;
            case DISCORD -> KnownChatEntities.DISCORD_PREFIX + name;
            case PERMISSION -> KnownChatEntities.PERMISSION_PREFIX + name;
            case PLAYER -> name;
            case SERVER -> KnownChatEntities.SERVER_SENDER.toString();
        };
    }

    public enum ActorType {
        PLAYER,
        CHANNEL,
        PERMISSION,
        DISCORD,
        SERVER
    }
}
