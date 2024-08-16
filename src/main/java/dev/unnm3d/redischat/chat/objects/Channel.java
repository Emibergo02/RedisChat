package dev.unnm3d.redischat.chat.objects;

import lombok.*;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class Channel extends ChannelAudience {
    @Setter
    private String displayName;
    @Setter
    private String format;
    private final int rateLimit;
    private final int rateLimitPeriod;
    @Setter
    private String discordWebhook;
    private final boolean filtered;
    private final boolean shownByDefault;
    private final boolean permissionEnabled;
    private final String notificationSound;

    public Channel(String name, String displayName, String format,
                   int proximityDistance, int rateLimit, int rateLimitPeriod,
                   String discordWebhook, boolean filtered, boolean shownByDefault,
                   boolean permissionEnabled, String notificationSound, @Singular List<String> permissions) {
        super(AudienceType.CHANNEL, name, proximityDistance, permissions);
        this.displayName = displayName;
        this.format = format;
        this.rateLimit = rateLimit;
        this.rateLimitPeriod = rateLimitPeriod;
        this.discordWebhook = discordWebhook;
        this.filtered = filtered;
        this.shownByDefault = shownByDefault;
        this.permissionEnabled = permissionEnabled;
        this.notificationSound = notificationSound;
    }


    public static ChannelBuilder builder(String name) {
        return new ChannelBuilder(name);
    }

    public static class ChannelBuilder extends ChannelAudienceBuilder {
        private String displayName;
        private String format;
        private int rateLimit = 5;
        private int rateLimitPeriod = 3;
        private String discordWebhook = "";
        private boolean filtered = true;
        private boolean shownByDefault = true;
        private boolean permissionEnabled = true;
        private String notificationSound;

        /**
         * Constructs a new ChannelBuilder with the specified name.
         *
         * @param name The name of the channel.
         */
        public ChannelBuilder(String name) {
            super(name);
            this.displayName = name;
        }

        /**
         * Sets the display name of the channel.
         *
         * @param displayName The display name to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the format of the channel.
         *
         * @param format The format to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder format(String format) {
            this.format = format;
            return this;
        }

        /**
         * Sets the rate limit of the channel.
         *
         * @param rateLimit The rate limit to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder rateLimit(int rateLimit) {
            this.rateLimit = rateLimit;
            return this;
        }

        /**
         * Sets the rate limit period of the channel.
         *
         * @param rateLimitPeriod The rate limit period to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder rateLimitPeriod(int rateLimitPeriod) {
            this.rateLimitPeriod = rateLimitPeriod;
            return this;
        }

        /**
         * Sets the Discord webhook URL of the channel.
         *
         * @param discordWebhook The Discord webhook URL to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder discordWebhook(String discordWebhook) {
            this.discordWebhook = discordWebhook;
            return this;
        }

        /**
         * Sets whether the channel is filtered.
         *
         * @param filtered True if the channel is filtered, false otherwise.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder filtered(boolean filtered) {
            this.filtered = filtered;
            return this;
        }

        /**
         * Sets whether the channel is shown by default.
         *
         * @param shownByDefault True if the channel is shown by default, false otherwise.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder shownByDefault(boolean shownByDefault) {
            this.shownByDefault = shownByDefault;
            return this;
        }

        /**
         * Sets the proximity distance of the channel.
         *
         * @param proximityDistance The proximity distance to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder proximityDistance(int proximityDistance) {
            this.proximityDistance = proximityDistance;
            return this;
        }

        /**
         * Sets whether permissions are enabled for the channel.
         *
         * @param permissionEnabled True if permissions are enabled, false otherwise.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder permissionEnabled(boolean permissionEnabled) {
            this.permissionEnabled = permissionEnabled;
            return this;
        }

        /**
         * Adds permissions to the channel.
         *
         * @param permissions Varargs parameter representing the permissions to add.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder permission(String... permissions) {
            this.permissions.addAll(List.of(permissions));
            return this;
        }

        /**
         * Sets the permissions for the channel.
         *
         * @param permissions A list of permissions to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * Sets the notification sound of the channel.
         *
         * @param notificationSound The notification sound to set.
         * @return The current instance of ChannelBuilder.
         */
        public ChannelBuilder notificationSound(String notificationSound) {
            this.notificationSound = notificationSound;
            return this;
        }

        public Channel build() {
            return new Channel(
                    this.name,
                    this.displayName,
                    this.format,
                    this.proximityDistance,
                    this.rateLimit,
                    this.rateLimitPeriod,
                    this.discordWebhook,
                    this.filtered,
                    this.shownByDefault,
                    this.permissionEnabled,
                    this.notificationSound,
                    this.permissions
            );
        }

        @Override
        public String toString() {
            return "ChannelBuilder(name=" + this.name + ", displayName=" + this.displayName + ", format=" + this.format +
                    ", rateLimit=" + this.rateLimit + ", rateLimitPeriod=" + this.rateLimitPeriod +
                    ", discordWebhook=" + this.discordWebhook + ", filtered=" + this.filtered +
                    ", shownByDefault=" + this.shownByDefault + ", permissionEnabled=" + this.permissionEnabled +
                    ", notificationSound=" + this.notificationSound + ", permissions=" + this.permissions + ")";
        }
    }
}
