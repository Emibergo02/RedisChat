package dev.unnm3d.redischat.moderation;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MuteManager {

    private final RedisChat plugin;
    private ConcurrentHashMap<String, Set<String>> mutedPlayers;

    public MuteManager(RedisChat plugin) {
        this.plugin = plugin;
        this.mutedPlayers = new ConcurrentHashMap<>();
        reload();
    }

    public void reload() {
        mutedPlayers.clear();
        plugin.getDataManager().getAllMutedChannels().thenAccept(stringSetMap -> {
            if (stringSetMap == null) return;
            this.mutedPlayers = new ConcurrentHashMap<>(stringSetMap);
        });
    }


    /**
     * Mute/Unmute a player in a channel
     *
     * @param playerName Player to mute
     * @param channel    Channel to mute
     * @param muted      true to mute, false to unmute
     */
    public void toggleMutePlayer(@NotNull String playerName, @NotNull String channel, boolean muted) {
        localMute(playerName, channel, muted);
        this.plugin.getDataManager().setMutedChannels(playerName, mutedPlayers.getOrDefault(playerName, new HashSet<>()));
    }

    /**
     * Mute/Unmute a player in a channel
     * But only locally, not in the database
     *
     * @param playerName Player to mute
     * @param channel    Channel to mute
     * @param muted      true to mute, false to unmute
     */
    public void localMute(@NotNull String playerName, @NotNull String channel, boolean muted) {
        if (!muted) {
            mutedPlayers.computeIfPresent(playerName, (s, strings) -> {
                strings.remove(channel);
                return strings.isEmpty() ? null : strings;
            });
            return;
        }
        plugin.getLogger().info("Muting " + playerName + " in " + channel);

        if (mutedPlayers.containsKey(playerName)) {
            mutedPlayers.get(playerName).add(channel);
        } else {
            setMutedPlayers(playerName, channel);
        }
    }

    /**
     * Set the muted channels of a player
     *
     * @param playerName Player to mute
     * @param channels   Channels to mute
     */
    private void setMutedPlayers(String playerName, String... channels) {
        if (channels.length == 0 || channels[0].isEmpty()) {
            mutedPlayers.remove(playerName);
            return;
        }
        mutedPlayers.put(playerName, Arrays.stream(channels).collect(Collectors.toCollection(HashSet::new)));
    }

    public void serializedUpdate(String serializedUpdate) {
        final String[] split = serializedUpdate.split(";");
        final String playerName = split[0];
        if (split.length == 1) {
            setMutedPlayers(playerName);
            return;
        }

        setMutedPlayers(playerName, split[1].split("§"));
    }

    public boolean isMuted(String playerName, String channel) {
        if (mutedPlayers.containsKey(KnownChatEntities.ALL_PLAYERS.toString()) && mutedPlayers.get(KnownChatEntities.ALL_PLAYERS.toString()).contains(channel))
            return true;

        return mutedPlayers.containsKey(playerName) && mutedPlayers.get(playerName).contains(channel);
    }


}
