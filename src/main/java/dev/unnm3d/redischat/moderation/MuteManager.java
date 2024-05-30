package dev.unnm3d.redischat.moderation;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MuteManager {

    private final RedisChat plugin;

    /**
     * Key: Player name
     * Value: Set of muted channel names
     */
    private final ConcurrentHashMap<String, Set<String>> channelMutedForPlayers;

    /**
     * Key: Player name
     * Value: Set of muted channel names
     */
    private final Set<String> whitelistEnabledPlayers;

    /**
     * Key: Player name
     * Value: Set of muted player names
     */
    private final ConcurrentHashMap<String, Set<String>> playersMutedForPlayers;

    public MuteManager(RedisChat plugin) {
        this.plugin = plugin;
        this.channelMutedForPlayers = new ConcurrentHashMap<>();
        this.whitelistEnabledPlayers = ConcurrentHashMap.newKeySet();
        this.playersMutedForPlayers = new ConcurrentHashMap<>();
        reload();
    }

    public void reload() {
        channelMutedForPlayers.clear();
        playersMutedForPlayers.clear();
        plugin.getDataManager().getAllMutedEntities().thenAccept(stringSetMap -> {
            if (stringSetMap == null) return;

            for (Map.Entry<String, Set<String>> stringSetEntry : stringSetMap.entrySet()) {
                if (stringSetEntry.getKey().startsWith(KnownChatEntities.CHANNEL_PREFIX.toString())) {
                    channelMutedForPlayers.put(stringSetEntry.getKey().substring(1), stringSetEntry.getValue());
                } else {
                    playersMutedForPlayers.put(stringSetEntry.getKey(), stringSetEntry.getValue());
                }
            }
        });
        plugin.getDataManager().getWhitelistEnabledPlayers().thenAccept(whitelistEnabledPlayers::addAll);
    }


    /**
     * Mute/Unmute a player in a channel
     *
     * @param playerName Player to mute
     * @param channel    Channel to mute
     * @param muted      true to mute, false to unmute
     */
    public void toggleMuteOnChannel(@NotNull String playerName, @NotNull String channel, boolean muted) {
        localChannelMute(playerName, channel, muted);
        this.plugin.getDataManager().setMutedEntities(playerName, channelMutedForPlayers.getOrDefault(playerName, new HashSet<>()));

    }

    /**
     * Mute/Unmute a player in a channel
     * But only locally, not in the database
     *
     * @param playerName Player to mute
     * @param channel    Channel to mute
     * @param muted      true to mute, false to unmute
     */
    public void localChannelMute(@NotNull String playerName, @NotNull String channel, boolean muted) {
        if (!muted) {
            channelMutedForPlayers.computeIfPresent(channel, (s, strings) -> {
                strings.remove(playerName);
                return strings.isEmpty() ? null : strings;
            });
            return;
        }

        if (channelMutedForPlayers.containsKey(channel)) {
            channelMutedForPlayers.get(channel).add(playerName);
        } else {
            setMutedChannels(channel, playerName);
        }
    }

    /**
     * Ignore/Unignore a player
     *
     * @param ignorer       Player that is ignoring
     * @param ignoredPlayer Player to ignore
     * @return true if the player is ignored
     */
    public boolean toggleIgnorePlayer(@NotNull String ignorer, @NotNull String ignoredPlayer) {
        if (playersMutedForPlayers.containsKey(ignorer) && playersMutedForPlayers.get(ignorer).contains(ignoredPlayer)) {
            localPlayerIgnore(ignorer, ignoredPlayer, false);
            this.plugin.getDataManager().setMutedEntities(ignorer, playersMutedForPlayers.getOrDefault(ignorer, new HashSet<>()));
            return false;
        }
        localPlayerIgnore(ignorer, ignoredPlayer, true);
        this.plugin.getDataManager().setMutedEntities(ignorer, playersMutedForPlayers.getOrDefault(ignorer, new HashSet<>()));
        return true;
    }

    public void localPlayerIgnore(@NotNull String ignorer, @NotNull String ignoredPlayer, boolean ignore) {
        if (!ignore) {
            playersMutedForPlayers.computeIfPresent(ignorer, (s, strings) -> {
                strings.remove(ignoredPlayer);
                return strings.isEmpty() ? null : strings;
            });
            return;
        }

        if (playersMutedForPlayers.containsKey(ignorer)) {
            playersMutedForPlayers.get(ignorer).add(ignoredPlayer);
        } else {
            setPlayersMutedForPlayers(ignorer, ignoredPlayer);
        }
    }

    /**
     * Set the muted channels of a player
     *
     * @param channel Channel to mute
     * @param players Players to mute
     */
    private void setMutedChannels(String channel, String... players) {
        if (players.length == 0 || players[0].isEmpty()) {
            channelMutedForPlayers.remove(channel);
            return;
        }
        channelMutedForPlayers.put(channel, Arrays.stream(players).collect(Collectors.toCollection(HashSet::new)));
    }

    /**
     * Mute/Unmute a player for another player
     *
     * @param ignoringPlayer Player to mute for
     * @param ignoredPlayer  Players to mute
     */
    private void setPlayersMutedForPlayers(String ignoringPlayer, String... ignoredPlayer) {
        if (ignoredPlayer.length == 0 || ignoredPlayer[0].isEmpty()) {
            playersMutedForPlayers.remove(ignoringPlayer);
            return;
        }
        playersMutedForPlayers.put(ignoringPlayer, Arrays.stream(ignoredPlayer).collect(Collectors.toCollection(HashSet::new)));
    }

    /**
     * Check if a player is muted in a channel
     *
     * @param playerName Player to check
     * @param channel    Channel to check
     * @return true if the player is muted in the channel
     */
    public boolean isMutedOnChannel(String playerName, String channel) {
        final Set<String> mutedPlayers = channelMutedForPlayers.get(channel);
        if (mutedPlayers == null) return false;
        return mutedPlayers.contains(playerName) || mutedPlayers.contains(KnownChatEntities.ALL_PLAYERS.toString());
    }

    /**
     * Check if a player is muted for another player
     *
     * @param ignorer Who is ignoring
     * @param ignored Who is being ignored
     * @return true if the "ignorer" is ignoring the "ignored"
     */
    public boolean isPlayerIgnored(String ignorer, String ignored) {
        final Set<String> mutedPlayers = playersMutedForPlayers.get(ignorer);
        boolean isIgnored = mutedPlayers != null && (
                mutedPlayers.contains(ignored) || mutedPlayers.contains(KnownChatEntities.ALL_PLAYERS.toString())
        );
        // If the player is in the whitelist, the result is inverted
        return isWhitelistEnabledPlayer(ignorer) != isIgnored;
    }

    public boolean isWhitelistEnabledPlayer(String playerName) {
        return whitelistEnabledPlayers.contains(playerName);
    }

    public Set<String> getIgnoreList(String playerName) {
        return playersMutedForPlayers.getOrDefault(playerName, new HashSet<>());
    }

    public void serializedUpdate(String serializedUpdate) {
        final String[] split = serializedUpdate.split(";");
        final String keyEntity = split[0];

        if (keyEntity.startsWith(KnownChatEntities.CHANNEL_PREFIX.toString())) {
            if (split.length == 1) {
                setMutedChannels(keyEntity.substring(1));
                return;
            }
            setMutedChannels(keyEntity.substring(1), split[1].split(","));
            return;
        }

        if (split.length == 1) {
            setPlayersMutedForPlayers(keyEntity);
            return;
        }
        setPlayersMutedForPlayers(keyEntity, split[1].split(","));
    }

    public void setWhitelistEnabledPlayer(String playerName, boolean enabled) {
        plugin.getDataManager().setWhitelistEnabledPlayer(playerName, enabled);
        whitelistEnabledUpdate(playerName, enabled);
    }

    public void whitelistEnabledUpdate(String playerName, boolean enabled) {
        if (enabled) {
            whitelistEnabledPlayers.add(playerName);
        } else {
            whitelistEnabledPlayers.remove(playerName);
        }
    }


}
