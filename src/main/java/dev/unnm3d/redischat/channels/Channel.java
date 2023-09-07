package dev.unnm3d.redischat.channels;

import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Channel {
    @NotNull
    private final String name;
    private String format;
    private final int rateLimit;
    private final int rateLimitPeriod;
    private final int proximityDistance;
    private final String discordWebhook;
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format){
        this.format=format;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public int getRateLimitPeriod() {
        return rateLimitPeriod;
    }

    public int getProximityDistance() {
        return proximityDistance;
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public Sound getNotificationSound() {
        return notificationSound;
    }

    public String serialize() {
        return name + "§§§" + format + "§§§" + rateLimit + "§§§" + rateLimitPeriod + "§§§" + proximityDistance + "§§§" + discordWebhook + "§§§" + (filtered ? "0" : "1") + "§§§" + notificationSound;
    }

    public static Channel deserialize(String serialized) {
        String[] split = serialized.split("§§§");
        return new Channel(split[0], split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]), split[5], split[6].equals("0"), Sound.valueOf(split[7]));
    }
}
