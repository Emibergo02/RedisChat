package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.chat.filters.FilterManager;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.mail.MailGUIManager;
import dev.unnm3d.redischat.moderation.MuteManager;
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
     * Get the mail manager
     *
     * @return Optional, the mail feature may be disabled
     */
    public abstract Optional<MailGUIManager> getMailManager();

    /**
     * Get the filter manager
     *
     * @return The filter manager
     */
    public abstract FilterManager getFilterManager();

    /**
     * Get the Mute Manager
     *
     * @return The mute manager
     */
    public abstract MuteManager getMuteManager();

    /**
     * Get the Data Manager
     *
     * @return The data manager
     */
    public abstract DataManager getDataManager();

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
     * Broadcast a message to a channel
     * @param channel The channel to broadcast to
     * @param message The message to broadcast, formatted strings will be parsed accordingly
     */
    public abstract void broadcastMessage(Channel channel, String message);

    /**
     * Sends a message into the RedisChat system (cross-server)
     *
     * @param player  The player who sent the message
     * @param message The message content
     */
    public abstract void outgoingMessage(CommandSender player, @NotNull String message);

    /**
     * Send a generic (non-private) ChatMessageInfo to all local players
     * Checks multicast permissions and mentions
     * (It calls sendComponentOrCache at the end)
     *
     * @param chatMessage The message to send
     */
    public abstract void sendGenericChat(@NotNull ChatMessage chatMessage);

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
     * Can be staffchat or public channel too
     *
     * @param channelName The name of the channel
     * @param player The player for parsing channel formats, can be null
     * @return The channel
     */
    public abstract Optional<Channel> getChannel(@Nullable String channelName, @Nullable CommandSender player);

    /**
     * Get a registered channel by name
     *
     * @param channelName The name of the channel
     * @return The channel
     */
    public abstract Optional<Channel> getRegisteredChannel(@Nullable String channelName);

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
     * Set the active channel for a player
     * @param playerName The player name
     * @param channelName The channel name
     */
    public abstract void setActiveChannel(String playerName, String channelName);

    /**
     * Get the active channel for a player stored in the local cache.
     * The cache is updated whenever a remote change is made to the active channel
     * Or when a player joins the server.
     * To get a precise, global active channel (you probably don't need that),
     * use DataManager#getActiveChannel
     *
     * @param playerName The player name
     * @return The currently enabled channel
     */
    public abstract String getActiveChannel(String playerName);

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
