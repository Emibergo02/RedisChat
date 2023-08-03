package dev.unnm3d.redischat.api;

import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Parse text with RedisEconomy tagresolvers
     *
     * @param player            the player to parse the placeholders for
     * @param text              the text to parse
     * @param parsePlaceholders whether to parse placeholders
     * @param parseMentions     whether to parse mentions
     * @param parseLinks        whether to parse links
     * @param tagResolvers      the additionals tag resolvers to use
     * @return the component representing the text using RedisChat tag resolvers
     */
    public abstract @NotNull Component parse(@Nullable CommandSender player, @NotNull String text, boolean parsePlaceholders, boolean parseMentions, boolean parseLinks, @NotNull TagResolver... tagResolvers);

    /**
     * Parses PlaceholderAPI placeholders
     * If a placeholder returns a colored string, we replace the placeholder as a component
     * to not override the successive tag formatting
     *
     * @param cmdSender    Who is parsing the placeholders
     * @param text         The text to parse
     * @param tagResolvers The tag resolvers to use
     * @return The parsed component
     */
    public abstract @NotNull Component parsePlaceholders(CommandSender cmdSender, @NotNull String text, @NotNull TagResolver... tagResolvers);

    /**
     * Clean all MiniMessage tags from a text
     *
     * @param text The text to clean
     * @return The cleaned text
     */
    public abstract @NotNull String purgeTags(@NotNull String text);

    /**
     * Get the RedisChat tag resolver for a player
     *
     * @param player     The player to get the tag resolver for
     * @param chatFormat The chat format to use
     * @return The tag resolver
     */
    public abstract @NotNull TagResolver getRedisChatTagResolver(@NotNull CommandSender player, @NotNull ChatFormat chatFormat);

    /**
     * Clean a message from bad words using RedisChat blacklist
     *
     * @param message The message to clean
     * @return The cleaned message
     */
    public abstract @NotNull String sanitize(@NotNull String message);

    /**
     * Send a generic ChatMessageInfo to all local players
     * Checks multicast permissions and mentions
     * (It calls sendComponentOrCache at the end)
     *
     * @param chatMessageInfo The chat message to send
     */
    public abstract void sendGenericChat(@NotNull ChatMessageInfo chatMessageInfo);

    /**
     * Sends a spy message to watchers
     *
     * @param chatMessageInfo The chat content to send
     * @param watcher         The player who is spying the message
     */
    public abstract void sendSpyChat(@NotNull ChatMessageInfo chatMessageInfo, @NotNull Player watcher);

    /**
     * Sends a private message to the receiver inside the chatMessageInfo
     * It is the final step of the private message process
     *
     * @param chatMessageInfo The chat packet to send
     */
    public abstract void sendPrivateChat(@NotNull ChatMessageInfo chatMessageInfo);

    /**
     * Sends a message to the player, or caches it if the player has the chat paused
     *
     * @param player    The player to send the message to
     * @param component The message to send
     */
    public abstract void sendComponentOrCache(@NotNull Player player, @NotNull Component component);

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


}
