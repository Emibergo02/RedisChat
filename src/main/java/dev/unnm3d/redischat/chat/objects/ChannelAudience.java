package dev.unnm3d.redischat.chat.objects;

import dev.unnm3d.redischat.chat.KnownChatEntities;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Builder(
        toBuilder = true,
        builderClassName = "ChannelAudienceBuilder",
        builderMethodName = "audienceBuilder"
)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
public class ChannelAudience implements DataSerializable {

    protected final String name;
    @Builder.Default
    protected AudienceType type = AudienceType.PLAYER;
    @Setter
    @Builder.Default
    protected int proximityDistance = -1;
    @Setter
    @Singular
    protected Set<String> permissions;


    /**
     * Constructor without 'local' field
     *
     * @param name        The name of the audience
     * @param type        The type of the audience, represented by the AudienceType enum
     * @param permissions A varargs parameter representing the permissions of the audience
     */
    public ChannelAudience(String name, AudienceType type, String... permissions) {
        this(name, type, -1, new HashSet<>(Set.of(permissions)));
    }

    /**
     * Constructor without 'permission' and 'local' fields
     *
     * @param channelName The name of the channel
     * @param permissions A varargs parameter representing the permissions of the audience
     */
    public ChannelAudience(String channelName, String... permissions) {
        this(channelName, AudienceType.CHANNEL, permissions);
    }

    /**
     * Default constructor
     * <p>
     * This constructor initializes the audience with the SERVER_SENDER name, SYSTEM type, and no permissions.
     */
    public ChannelAudience() {
        this(KnownChatEntities.SERVER_SENDER.toString(), AudienceType.PLAYER, -1, new HashSet<>());
    }

    /**
     * Constructor for building a player audience
     *
     * @param playerName The name of the player
     */
    public static ChannelAudience newPlayerAudience(String playerName) {
        return new ChannelAudience(playerName, AudienceType.PLAYER);
    }

    /**
     * Constructor for building a discord audience
     *
     * @param authorUsername The discord username of the audience
     */
    public static ChannelAudience newDiscordAudience(String authorUsername) {
        return new ChannelAudience(authorUsername, AudienceType.DISCORD);
    }

    /**
     * Constructor for building a public channel audience
     */
    public static ChannelAudience publicChannelAudience(String... permissions) {
        return new ChannelAudience(KnownChatEntities.PUBLIC_CHAT.toString(), AudienceType.CHANNEL, permissions);
    }

    public static ChannelAudience deserialize(String serialized) {
        String[] parts = serialized.split("§;");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid audience serialization");
        }
        return new ChannelAudience(
                parts[1],
                AudienceType.valueOf(parts[0]),
                Integer.parseInt(parts[2]),
                parts.length < 4 ? new HashSet<>() : new HashSet<>(Set.of(parts[3].split(","))));
    }

    /**
     * Checks if the audience is a player
     *
     * @return True if the audience is a player, false otherwise
     */
    public boolean isPlayer() {
        return type == AudienceType.PLAYER;
    }

    /**
     * Checks if the audience is the server
     *
     * @return True if the audience is the server, false otherwise
     */
    public boolean isServer() {
        return type == AudienceType.PLAYER && name.equals(KnownChatEntities.SERVER_SENDER.toString());
    }

    /**
     * Checks if the audience is a channel
     *
     * @return True if the audience is a channel, false otherwise
     */
    public boolean isChannel() {
        return type == AudienceType.CHANNEL;
    }

    /**
     * Checks if the audience is a discord channel
     *
     * @return True if the audience is a discord channel, false otherwise
     */
    public boolean isDiscord() {
        return type == AudienceType.DISCORD;
    }

    @Override
    public String serialize() {
        return type.name() + "§;" + name + "§;" + proximityDistance + "§;" + String.join(",", permissions);
    }

    public static ChannelAudienceBuilder audienceBuilder(String name) {
        return new ChannelAudienceBuilder().name(name);
    }
}
