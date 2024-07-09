package dev.unnm3d.redischat.chat.objects;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class Channel extends ChannelAudience {

    @Setter
    private String format;
    private final int rateLimit;
    private final int rateLimitPeriod;
    @Setter
    private String discordWebhook;
    private final boolean filtered;
    private final String notificationSound;

    @Builder(
            builderClassName = "NewChannelBuilder",
            builderMethodName = "channelBuilder"
    )
    public Channel(String name, String format, int proximityDistance, int rateLimit, int rateLimitPeriod, String discordWebhook, boolean filtered, String notificationSound, @Singular Set<String> permissions) {
        super(name, AudienceType.CHANNEL, proximityDistance, permissions);
        this.format = format;
        this.rateLimit = rateLimit;
        this.rateLimitPeriod = rateLimitPeriod;
        this.discordWebhook = discordWebhook;
        this.filtered = filtered;
        this.notificationSound = notificationSound;
    }

    public static NewChannelBuilder channelBuilder(String name) {
        return new NewChannelBuilder()
                .name(name)
                .rateLimit(5)
                .rateLimitPeriod(3)
                .discordWebhook("")
                .filtered(true)
                .notificationSound(null);
    }

    public static Channel deserialize(String serializedChannel) {
        String[] parts = serializedChannel.split("§;");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid channel serialization");
        }

        return Channel.channelBuilder(parts[1])
                .proximityDistance(Integer.parseInt(parts[2]))
                .permissions(new HashSet<>(Set.of(parts[3].split(","))))
                .format(parts[4])
                .rateLimit(Integer.parseInt(parts[5]))
                .rateLimitPeriod(Integer.parseInt(parts[6]))
                .discordWebhook(parts[7])
                .filtered(Boolean.parseBoolean(parts[8]))
                .notificationSound(parts[9])
                .build();
    }

    @Override
    public String serialize() {
        return super.serialize() + "§;" + format + "§;" + rateLimit + "§;" + rateLimitPeriod + "§;" + discordWebhook + "§;" + filtered + "§;" + notificationSound;
    }
}
