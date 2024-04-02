package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class RedisChatPAPI extends PlaceholderExpansion {
    private final RedisChat plugin;

    @Override
    public @NotNull String getIdentifier() {
        return "redischat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Unnm3d";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }


    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (plugin.getServer().isPrimaryThread()) {
            plugin.getLogger().warning("RedisChat's placeholders are being called from the main thread, this is not recommended and may cause lag!");
        }

        if (params.equalsIgnoreCase("active_channel")) {
            if (player.getName() == null) return plugin.getChannelManager().getPublicChannel(null).getName();
            return plugin.getDataManager().getActivePlayerChannel(player.getName(), plugin.getChannelManager().getRegisteredChannels())
                    .thenApply(channel -> channel == null ?

                            plugin.getChannelManager().getPublicChannel(null).getName() :
                            channel
                    ).toCompletableFuture().join();
        }

        if (params.equalsIgnoreCase("ignoring_all")) {
            if (player.getName() == null) return "false";
            return plugin.getChannelManager().getMuteManager().getIgnoreList(player.getName()).contains("all") ? "true" : "false";
        }
        if (player != null && player.getName() != null) {
            return plugin.getPlaceholderManager().getPlaceholder(player.getName(), params);
        }
        return null; // Placeholder is unknown by the Expansion
    }
}
