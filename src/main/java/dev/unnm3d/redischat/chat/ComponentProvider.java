package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.configs.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ComponentProvider {
    private final RedisChat plugin;
    private final BukkitAudiences audiences;
    private final MiniMessage miniMessage;
    private final TagResolver standardTagResolver;
    private final ConcurrentHashMap<Player, List<Component>> cacheBlocked;
    @Getter
    private static ComponentProvider instance;
    private final List<TagResolver.Single> customPlaceholderResolvers;

    public ComponentProvider(RedisChat plugin) {
        instance = this;
        this.plugin = plugin;
        this.audiences = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
        this.cacheBlocked = new ConcurrentHashMap<>();
        this.customPlaceholderResolvers = plugin.config.placeholders.entrySet()
                .stream().map(entry ->
                        Placeholder.component(entry.getKey(), MiniMessage.miniMessage().deserialize(entry.getValue()))
                ).toList();
        this.standardTagResolver = StandardTags.defaults();

    }

    public BaseComponent[] toBaseComponent(Component component) {
        return BungeeComponentSerializer.get().serialize(component);
    }

    public Component parse(String text, TagResolver... tagResolvers) {
        return parse(null, text, tagResolvers);
    }

    public Component parse(String text) {
        return parse(text, this.standardTagResolver);
    }

    public Component parse(CommandSender player, String text, TagResolver... tagResolvers) {
        return parse(player, text, true, true, true, tagResolvers);
    }

    public Component parse(CommandSender player, String text, boolean parsePlaceholders, boolean parseMentions, boolean parseLinks, TagResolver... tagResolvers) {
        if (player != null)
            text = parseLegacy(player, text);
        if (parseLinks) {
            text = parseLinks(text, plugin.config.formats.get(0));
        }
        if (parseMentions) {
            text = parseMentions(text, plugin.config.formats.get(0));
        }
        if (parsePlaceholders) {
            text = parsePlaceholders(player, text);
        }
        if (plugin.config.debug)
            Bukkit.getLogger().info("Parsed SelectableParse: " + text);
        return miniMessage.deserialize(text, tagResolvers);
    }

    public Component parse(CommandSender player, String text) {
        return parse(player, text, this.standardTagResolver);
    }

    public String parsePlaceholders(CommandSender cmdSender, String text) {
        String message =
                cmdSender instanceof OfflinePlayer offlinePlayer
                        ? PlaceholderAPI.setPlaceholders(offlinePlayer, text)
                        : PlaceholderAPI.setPlaceholders(null, text);
        if (plugin.config.debug)
            Bukkit.getLogger().info("Parsed placeholders: " + message);
        return miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(message)).replace("\\", "");
    }

    public String purgeTags(String text) {
        return miniMessage.stripTags(text, TagResolver.standard());
    }

    public TagResolver getInvShareTagResolver(CommandSender player, Config.ChatFormat chatFormat) {

        TagResolver.Builder builder = TagResolver.builder();

        String toParse = chatFormat.inventory_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-inventory");
        TagResolver inv = Placeholder.component("inv", parse(player, toParse, true, false, false, this.standardTagResolver));

        toParse = chatFormat.item_format();
        toParse = toParse.replace("%player%", player.getName());
        if (player instanceof Player p) {
            if (!p.getInventory().getItemInMainHand().getType().isAir()) {
                if (p.getInventory().getItemInMainHand().getItemMeta() != null)
                    if (p.getInventory().getItemInMainHand().getItemMeta().hasDisplayName())
                        toParse = toParse.replace("%item_name%", p.getInventory().getItemInMainHand().getItemMeta().getDisplayName());
                    else {
                        toParse = toParse.replace("%item_name%", p.getInventory().getItemInMainHand().getType().name().toLowerCase().replace("_", " "));
                    }
            } else {
                toParse = toParse.replace("%item_name%", "Nothing");
            }
        }
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-item");
        TagResolver item = Placeholder.component("item", parse(player, toParse, true, false, false, this.standardTagResolver));

        toParse = chatFormat.enderchest_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-enderchest");
        TagResolver ec = Placeholder.component("ec", parse(player, toParse, true, false, false, this.standardTagResolver));

        customPlaceholderResolvers.forEach(builder::resolver);

        builder.resolver(inv);
        builder.resolver(item);
        builder.resolver(ec);
        return builder.build();
    }

    public String parseMentions(String text, Config.ChatFormat format) {
        String toParse = text;
        for (String playerName : plugin.getPlayerListManager().getPlayerList()) {
            Pattern p = Pattern.compile("(^" + playerName + "|" + playerName + "$|\\s" + playerName + "\\s)"); //
            Matcher m = p.matcher(text);
            if (m.find()) {
                String replacing = m.group();
                replacing = replacing.replace(playerName, format.mention_format().replace("%player%", playerName));
                toParse = toParse.replace(m.group(), replacing);
                if (plugin.config.debug)
                    Bukkit.getLogger().info("mention " + playerName + " : " + toParse);
            }
        }

        return toParse;
    }

    public String parseLinks(String text, Config.ChatFormat format) {
        String toParse = text;
        Pattern p = Pattern.compile("(https?://\\S+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String linkString = m.group();
            linkString = linkString.endsWith("/") ? linkString.substring(0, linkString.length() - 1) : linkString;
            toParse = toParse.replace(m.group(), format.link_format().replace("%link%", linkString));
        }
        if (plugin.config.debug)
            Bukkit.getLogger().info("links: " + toParse);
        return toParse;
    }

    public String sanitize(String message) {
        for (String regex : plugin.config.regex_blacklist) {
            message = message.replaceAll(regex, "***");
        }
        return message;
    }

    public boolean antiCaps(String message) {
        int capsCount = 0;
        for (char c : message.toCharArray())
            if (Character.isUpperCase(c))
                capsCount++;
        return capsCount > message.length() / 2 && message.length() > 20;//50% of the message is caps and the message is longer than 20 chars
    }

    public String parseLegacy(CommandSender player, String text) {
        if (plugin.config.legacyColorCodesSupport && player.hasPermission(Permission.REDIS_CHAT_USE_FORMATTING.getPermission())) {
            text = miniMessage.serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            if (plugin.config.debug) {
                Bukkit.getLogger().info("Parsed legacy: " + text);
            }
        }
        return text;
    }

    public void sendGenericChat(ChatMessageInfo chatMessageInfo) {
        String multicastPermission = chatMessageInfo.getReceiverName().charAt(0) == '@' ? chatMessageInfo.getReceiverName().substring(1) : null;
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (multicastPermission != null) {
                if (!onlinePlayer.hasPermission(multicastPermission)) continue;
            }
            sendComponentOrCache(onlinePlayer, MiniMessage.miniMessage().deserialize(chatMessageInfo.getMessage()));
        }
    }

    /**
     * Sends a spy message to watchers
     *
     * @param receiverName The name of the receiver of the message
     * @param senderName   The name of the sender of the message
     * @param watcher      The player who is spying the message
     * @param deserialize  The message to send
     */
    public void sendSpyChat(String receiverName, String senderName, Player watcher, String deserialize) {
        Component formatted = MiniMessage.miniMessage().deserialize(plugin.messages.spychat_format.replace("%receiver%", receiverName).replace("%sender%", senderName));

        //Parse into minimessage (placeholders, tags and mentions)
        Component toBeReplaced = parse(deserialize);
        //Put message into format
        formatted = formatted.replaceText(
                builder -> builder.match("%message%").replacement(toBeReplaced)
        );
        sendComponentOrCache(watcher, formatted);
    }

    /**
     * Sends a private message to the receiver
     * It is the final step of the private message process
     *
     * @param chatMessageInfo The chat packet to send
     */
    public void sendPrivateChat(ChatMessageInfo chatMessageInfo) {
        Player p = Bukkit.getPlayer(chatMessageInfo.getReceiverName());
        if (p != null)
            if (p.isOnline()) {
                List<Config.ChatFormat> chatFormatList = plugin.config.getChatFormats(p);
                if (chatFormatList.isEmpty()) return;
                Component formatted = parse(null, chatFormatList.get(0).receive_private_format()
                        .replace("%receiver%", chatMessageInfo.getReceiverName())
                        .replace("%sender%", chatMessageInfo.getSenderName()));
                Component toBeReplaced = parse(p, chatMessageInfo.getMessage(), false, false, false, this.standardTagResolver);
                //Put message into format
                formatted = formatted.replaceText(
                        builder -> builder.match("%message%").replacement(toBeReplaced)
                );
                sendComponentOrCache(p, formatted);
            }
    }

    /**
     * Sends a message to the player, or caches it if the player is blocked
     *
     * @param player    The player to send the message to
     * @param component The message to send
     */
    private void sendComponentOrCache(Player player, Component component) {
        if (cacheBlocked.computeIfPresent(player,
                (player1, components) -> {
                    components.add(component);
                    return components;
                }) == null) {//If the player is not blocked
            plugin.getComponentProvider().sendMessage(player, component);
        }
    }

    /**
     * Pauses the chat for a player and caches all messages sent to them
     *
     * @param player The player to pause the chat for
     */
    public void pauseChat(Player player) {
        cacheBlocked.put(player, new ArrayList<>());
    }

    public boolean isPaused(Player player) {
        return cacheBlocked.containsKey(player);
    }

    /**
     * Unpauses the chat for a player and sends all cached messages to them
     *
     * @param player The player to unpause the chat for
     */
    public void unpauseChat(Player player) {
        if (cacheBlocked.containsKey(player)) {
            for (Component component : cacheBlocked.remove(player)) {
                plugin.getComponentProvider().sendMessage(player, component);
            }
        }
    }

    public void sendMessage(CommandSender p, String message) {
        audiences.sender(p).sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void sendMessage(CommandSender p, Component component) {
        audiences.sender(p).sendMessage(component);
    }

    public void openBook(Player player, Book book) {
        audiences.player(player).openBook(book);
    }
}

