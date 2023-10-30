package dev.unnm3d.redischat.channels;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class Channel {
    @NotNull
    private final String name;
    private String format;
    private final int rateLimit;
    private final int rateLimitPeriod;
    private final int proximityDistance;
    @Setter
    private String discordWebhook;
    private final boolean filtered;
    private final Sound notificationSound;

    public Channel(@NotNull String name, String format, int rateLimit, int rateLimitPeriod, int proximityDistance, @NotNull String discordWebhook, boolean filtered, @Nullable Sound notificationSound) {
        this.name = name;
        this.format = format;
        this.rateLimit = rateLimit;
        this.rateLimitPeriod = rateLimitPeriod;
        this.proximityDistance = proximityDistance;
        this.discordWebhook = discordWebhook;
        this.filtered = filtered;
        this.notificationSound = notificationSound;
    }

    public Channel(String name, String format) {
        this(name, format, 5, 3, -1, "", false, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    public @NotNull String getName() {
        return name;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String serialize() {
        return name + "§§§" + format + "§§§" + rateLimit + "§§§" + rateLimitPeriod + "§§§" + proximityDistance + "§§§" + discordWebhook + "§§§" + (filtered ? "0" : "1") + (notificationSound == null ? "" : "§§§" + notificationSound);
    }

    public static Channel deserialize(String serialized) {
        String[] split = serialized.split("§§§");
        return new Channel(split[0], split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]), split[5], split[6].equals("0"), split.length == 8 ? Sound.valueOf(split[7]) : null);
    }

}
