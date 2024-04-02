package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.ComponentProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("unused")
public abstract class RedisChatAPI {
    protected static RedisChatAPI INSTANCE = null;

    /**
     * Get API instance
     *
     * @return the API instance
     */
    public static @Nullable RedisChatAPI getAPI() {
        return INSTANCE;
    }

    /**
     * Get the component provider
     *
     * @return The component provider
     */
    public abstract ComponentProvider getComponentProvider();

    /**
     * Register a channel
     *
     * @param channel The channel to register
     */
    public abstract void registerChannel(Channel channel);

    /**
     * Unregister a channel
     *
     * @param channelName The name of the channel to unregister
     */
    public abstract void unregisterChannel(String channelName);

    /**
     * Open the channels GUI to a player
     *
     * @param player The player to open the GUI to
     */
    public abstract void openChannelsGUI(Player player);

    /**
     * Check if a player is rate-limited in a channel
     * If the data medium is not REDIS, the rate-limit register will be local
     *
     * @param player  The player
     * @param channel The channel
     * @return true if the player is rate-limited, false otherwise
     */
    protected abstract boolean isRateLimited(CommandSender player, Channel channel);

    /**
     * Sends a message into the RedisChat system (cross-server)
     *
     * @param player  The player who sent the message
     * @param channel The channel the message was sent to
     * @param message The message content
     */
    public abstract void playerChannelMessage(CommandSender player, Channel channel, @NotNull String message);

    /**
     * Sends a private or public message inside the current server
     * Registers the message inside keep chat feature (1.20.2+ Check docs for more info)
     *
     * @param chatMessageInfo The ChatMessageInfo to send
     */
    public abstract void sendAndKeepLocal(ChatMessageInfo chatMessageInfo);

    /**
     * Send a generic (non-private) ChatMessageInfo to all local players
     * Checks multicast permissions and mentions
     * (It calls sendComponentOrCache at the end)
     *
     * @param chatMessageInfo The chat message to send
     * @param recipient       The receiver of the message, null if it's a global/broadcasted message
     */
    public abstract void sendGenericChat(@NotNull ChatMessageInfo chatMessageInfo, @Nullable Player recipient);


    /**
     * Sends a private message to the receiver inside the chatMessageInfo
     * It is the final step of the private message process
     *
     * @param chatMessageInfo The chat packet to send
     */
    public abstract void sendPrivateChat(@NotNull ChatMessageInfo chatMessageInfo);


    /**
     * Pauses the chat for a player and caches all messages sent to them
     *
     * @param player The player to pause the chat for
     */
    public abstract void pauseChat(@NotNull Player player);

    public abstract boolean isPaused(@NotNull Player player);

    /**
     * Unpauses the chat for a player and sends all cached messages to them
     *
     * @param player The player to unpause the chat for
     */
    public abstract void unpauseChat(@NotNull Player player);

    /**
     * Get a channel by name null if not found
     *
     * @param channelName The name of the channel
     * @return The channel
     */
    public abstract Optional<Channel> getChannel(@Nullable String channelName);

    /**
     * Get the public channel
     * The format of the channel is defined by the player permissions
     *
     * @param player The player
     * @return The public channel
     */
    public abstract Channel getPublicChannel(CommandSender player);

    /**
     * Get the staff chat channel
     *
     * @return The staff chat channel
     */
    public abstract Channel getStaffChatChannel();

    /**
     * Add vanish "canSee" integration
     *
     * @param vanishIntegration The vanish integration
     */
    public abstract void addVanishIntegration(VanishIntegration vanishIntegration);

    /**
     * Remove vanish "canSee" integration
     *
     * @param vanishIntegration The vanish integration
     */
    public abstract void removeVanishIntegration(VanishIntegration vanishIntegration);

}
