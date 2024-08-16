package dev.unnm3d.redischat.chat.objects;

import dev.unnm3d.redischat.chat.KnownChatEntities;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
public class ChannelAudience {
    protected AudienceType type;
    protected final String name;
    @Setter
    protected int proximityDistance;
    @Setter
    protected List<String> permissions;


    /**
     * Constructor without 'local' field
     *
     * @param type        The type of the audience, represented by the AudienceType enum
     * @param name        The name of the audience
     * @param permissions A varargs parameter representing the permissions of the audience
     */
    public ChannelAudience(AudienceType type, String name, String... permissions) {
        this(type, name, -1, new ArrayList<>(List.of(permissions)));
    }

    /**
     * Constructor without 'permission' and 'local' fields
     *
     * @param channelName The name of the channel
     * @param permissions A varargs parameter representing the permissions of the audience
     */
    public ChannelAudience(String channelName, String... permissions) {
        this(AudienceType.CHANNEL, channelName, permissions);
    }

    /**
     * Default constructor
     * <p>
     * This constructor initializes the audience with the SERVER_SENDER name, SYSTEM type, and no permissions.
     */
    public ChannelAudience() {
        this(AudienceType.PLAYER, KnownChatEntities.SERVER_SENDER.toString(), -1, new ArrayList<>());
    }

    /**
     * Constructor for building a player audience
     *
     * @param playerName The name of the player
     */
    public static ChannelAudience newPlayerAudience(String playerName) {
        return new ChannelAudience(AudienceType.PLAYER, playerName);
    }

    /**
     * Constructor for building a discord audience
     *
     * @param authorUsername The discord username of the audience
     */
    public static ChannelAudience newDiscordAudience(String authorUsername) {
        return new ChannelAudience(AudienceType.DISCORD, authorUsername);
    }

    /**
     * Constructor for building a public channel audience
     */
    public static ChannelAudience publicChannelAudience(String... permissions) {
        return new ChannelAudience(AudienceType.CHANNEL, KnownChatEntities.GENERAL_CHANNEL.toString(), permissions);
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

    public static ChannelAudienceBuilder builder(String name) {
        return new ChannelAudienceBuilder(name);
    }


    public static class ChannelAudienceBuilder {
        protected AudienceType type = AudienceType.PLAYER;
        protected final String name;
        protected int proximityDistance = -1;
        protected List<String> permissions = new ArrayList<>();

        /**
         * Constructor for ChannelAudienceBuilder.
         *
         * @param name The name of the audience.
         */
        public ChannelAudienceBuilder(String name) {
            this.name = name;
        }

        /**
         * Sets the type of the audience.
         *
         * @param type The type of the audience.
         * @return The current instance of ChannelAudienceBuilder.
         */
        public ChannelAudienceBuilder type(AudienceType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the proximity distance for the audience.
         *
         * @param proximityDistance The proximity distance.
         * @return The current instance of ChannelAudienceBuilder.
         */
        public ChannelAudienceBuilder proximityDistance(int proximityDistance) {
            this.proximityDistance = proximityDistance;
            return this;
        }

        /**
         * Adds permissions to the audience.
         *
         * @param permissions Varargs parameter representing the permissions.
         * @return The current instance of ChannelAudienceBuilder.
         */
        public ChannelAudienceBuilder permission(String... permissions) {
            this.permissions.addAll(List.of(permissions));
            return this;
        }

        /**
         * Sets the permissions for the audience.
         *
         * @param permissions A list of permissions.
         * @return The current instance of ChannelAudienceBuilder.
         */
        public ChannelAudienceBuilder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public ChannelAudience build() {
            return new ChannelAudience(type, name, proximityDistance, permissions);
        }

        public String toString() {
            return "ChannelAudience.ChannelAudienceBuilder(type=" + this.type + ", name=" + this.name + ", proximityDistance=" + this.proximityDistance + ", permissions=" + this.permissions + ")";
        }
    }
}
