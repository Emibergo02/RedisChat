package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.chat.objects.NewChannel;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.mail.MailGUIManager;
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
     * Register a channel
     *
     * @param channel The channel to register
     */
    public abstract void registerChannel(NewChannel channel);

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
     * Sends a message into the RedisChat system (cross-server)
     *
     * @param player  The player who sent the message
     * @param message The message content
     */
    public abstract void outgoingMessage(CommandSender player, @NotNull String message);

    /**
     * Sends a private or public message inside the current server
     * Registers the message inside keep chat feature (1.20.2+ Check docs for more info)
     *
     * @param chatMessageInfo The ChatMessageInfo to send
     */
    public abstract void sendAndKeepLocal(NewChatMessage chatMessageInfo);

    /**
     * Send a generic (non-private) ChatMessageInfo to all local players
     * Checks multicast permissions and mentions
     * (It calls sendComponentOrCache at the end)
     *
     * @param chatMessage The message to send
     */
    public abstract void sendGenericChat(@NotNull NewChatMessage chatMessage);


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
    public abstract Optional<NewChannel> getChannel(@Nullable String channelName);

    /**
     * Get the public channel
     * The format of the channel is defined by the player permissions
     *
     * @param player The player
     * @return The public channel
     */
    public abstract NewChannel getPublicChannel(CommandSender player);

    /**
     * Get the staff chat channel
     *
     * @return The staff chat channel
     */
    public abstract NewChannel getStaffChatChannel();

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
