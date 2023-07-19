package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.configs.Config;
import dev.unnm3d.redischat.integrations.TagResolverIntegration;
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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
    private final List<TagResolverIntegration> tagResolverIntegrationList;

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
        this.tagResolverIntegrationList = new ArrayList<>();
    }

    public void addResolverIntegration(TagResolverIntegration integration) {
        this.tagResolverIntegrationList.add(integration);
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
        AbstractMap.SimpleEntry<String, Component> parsedLinks = new AbstractMap.SimpleEntry<>(null, null);
        if (parseLinks) {
            parsedLinks = parseLinks(text, plugin.config.formats.get(0));
            text = parsedLinks.getKey();
        }
        if (parseMentions) {
            text = parseMentions(text, plugin.config.formats.get(0));
        }
        if (plugin.config.legacyColorCodesSupport) {
            text = parseLegacy(player, text);
        }

        Component finalComponent = parsePlaceholders ?
                parsePlaceholders(player, parseResolverIntegrations(text), tagResolvers) :
                miniMessage.deserialize(text, tagResolvers);

        if (parsedLinks.getValue() != null) {
            AbstractMap.SimpleEntry<String, Component> finalParsedLinks = parsedLinks;
            finalComponent = finalComponent.replaceText(rTextBuilder ->
                    rTextBuilder.matchLiteral("%link%")
                            .replacement(finalParsedLinks.getValue()));
        }
        return finalComponent;
    }

    public String replaceBukkitColorCodesWithSection(String text) {
        if (!plugin.config.legacyColorCodesSupport) { // if legacy color codes support is disabled, we don't need to replace anything
            return text;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                int charCode = chars[i + 1];

                if ((charCode >= 48 && charCode <= 57)          // 0-9
                        || (charCode >= 97 && charCode <= 102)  // a-f
                        || (charCode >= 107 && charCode <= 111) // k-o
                        || charCode == 114                      // r
                        || charCode == 120                      // x
                ) {
                    chars[i] = '§';
                }
            }
        }
        return new String(chars);
    }

    public Component parse(CommandSender player, String text) {
        return parse(player, text, this.standardTagResolver);
    }

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
    public Component parsePlaceholders(CommandSender cmdSender, String text, TagResolver[] tagResolvers) {
        final String[] stringPlaceholders = text.split("%");

        final LinkedHashMap<String, Component> placeholders = new LinkedHashMap<>();
        int placeholderStep = 1;
        // we need to split the text by % and then check if the placeholder is a placeholder or not
        for (int i = 0; i < stringPlaceholders.length; i++) {
            if (i % 2 == placeholderStep) {
                final String reformattedPlaceholder = "%" + stringPlaceholders[i] + "%";
                final String parsedPlaceH = replaceBukkitColorCodesWithSection(
                        cmdSender instanceof OfflinePlayer offlinePlayer
                                ? PlaceholderAPI.setPlaceholders(offlinePlayer, reformattedPlaceholder)
                                : PlaceholderAPI.setPlaceholders(null, reformattedPlaceholder)
                );
                if (parsedPlaceH.equals(reformattedPlaceholder)) {
                    placeholderStep = Math.abs(placeholderStep - 1);
                    continue;
                }
                if (plugin.config.enablePlaceholderGlitch) {
                    text = text.replace(reformattedPlaceholder, miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(parsedPlaceH)));
                } else if (parsedPlaceH.contains("§")) {
                    //Colored placeholder needs to be pasted after the normal text is parsed
                    placeholders.put(reformattedPlaceholder, LegacyComponentSerializer.legacySection().deserialize(parsedPlaceH));
                } else {
                    text = text.replace(reformattedPlaceholder, parsedPlaceH);
                }
            }
        }
        Component answer = miniMessage.deserialize(text, tagResolvers);
        for (String placeholder : placeholders.keySet()) {
            answer = answer.replaceText(rBuilder -> rBuilder.matchLiteral(placeholder).replacement(placeholders.get(placeholder)));
        }
        return answer;
    }

    public String purgeTags(String text) {
        return miniMessage.stripTags(text, TagResolver.standard());
    }

    public TagResolver getInvShareTagResolver(CommandSender player, Config.ChatFormat chatFormat) {

        TagResolver.Builder builder = TagResolver.builder();

        String toParseInv = chatFormat.inventory_format();
        toParseInv = toParseInv.replace("%player%", player.getName());
        toParseInv = toParseInv.replace("%command%", "/invshare " + player.getName() + "-inventory");
        TagResolver inv = Placeholder.component("inv", parse(player, toParseInv, true, false, false, this.standardTagResolver));

        String toParseItem = chatFormat.item_format();
        toParseItem = toParseItem.replace("%player%", player.getName());
        toParseItem = toParseItem.replace("%command%", "/invshare " + player.getName() + "-item");
        Component toParseItemComponent = parse(player, toParseItem, true, false, false, this.standardTagResolver);
        if (player instanceof Player p) {
            if (!p.getInventory().getItemInMainHand().getType().isAir()) {
                if (p.getInventory().getItemInMainHand().getItemMeta() != null)
                    if (p.getInventory().getItemInMainHand().getItemMeta().hasDisplayName()) {
                        toParseItemComponent = toParseItemComponent.replaceText(rTextBuilder ->
                                rTextBuilder.matchLiteral("%item_name%")
                                        .replacement(
                                                parse(player,
                                                        parseLegacy(null, p.getInventory().getItemInMainHand().getItemMeta().getDisplayName()),
                                                        false,
                                                        false,
                                                        false,
                                                        this.standardTagResolver))
                        );
                    } else {
                        toParseItemComponent = toParseItemComponent.replaceText(rTextBuilder ->
                                rTextBuilder.matchLiteral("%item_name%")
                                        .replacement(
                                                parse(player,
                                                        p.getInventory().getItemInMainHand().getType().name().toLowerCase().replace("_", " "),
                                                        false,
                                                        false,
                                                        false,
                                                        this.standardTagResolver))
                        );
                    }
            } else {
                toParseItemComponent = toParseItemComponent.replaceText(rTextBuilder ->
                        rTextBuilder.matchLiteral("%item_name%")
                                .replacement("Nothing")
                );
            }
        }
        TagResolver item = Placeholder.component("item", toParseItemComponent);

        String toParseEnderChest = chatFormat.enderchest_format();
        toParseEnderChest = toParseEnderChest.replace("%player%", player.getName());
        toParseEnderChest = toParseEnderChest.replace("%command%", "/invshare " + player.getName() + "-enderchest");
        TagResolver ec = Placeholder.component("ec", parse(player, toParseEnderChest, true, false, false, this.standardTagResolver));

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
                    Bukkit.getLogger().info("mention parsed for " + playerName + " : " + toParse);
            }
        }

        return toParse;
    }

    public AbstractMap.SimpleEntry<String, Component> parseLinks(String text, Config.ChatFormat format) {
        Component linkComponent = null;
        Pattern p = Pattern.compile("(https?://\\S+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String linkString = m.group();
            linkString = linkString.endsWith("/") ? // the last slash breaks the closing tag of <click>
                    linkString.substring(0, linkString.length() - 1) :
                    linkString;
            text = text.replace(m.group(), "%link%");//replace the link with a placeholder
            linkComponent = miniMessage.deserialize(format.link_format().replace("%link%", linkString));
        }

        if (plugin.config.debug)
            Bukkit.getLogger().info("links: " + text);
        return new AbstractMap.SimpleEntry<>(text, linkComponent);
    }

    private String parseResolverIntegrations(String text){
        for (TagResolverIntegration resolver : this.tagResolverIntegrationList) {
            text = resolver.parseTags(text);
        }
        return text;
    }

    public String sanitize(String message) {
        for (String regex : plugin.config.regex_blacklist) {
            message = message.replaceAll(regex, "<obf>swear</obf>");
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

    public String parseLegacy(@Nullable Permissible player, String text) {
        if (
                player == null || //Is without permissions or if it has permissions
                        player.hasPermission(Permission.REDIS_CHAT_USE_FORMATTING.getPermission())
        ) {
            text = miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(replaceBukkitColorCodesWithSection(text)));
            if (plugin.config.debug) {
                Bukkit.getLogger().info("Parsed legacy: " + text);
            }
            return text.replace("\\", "");
        }
        return text;
    }

    public void sendGenericChat(ChatMessageInfo chatMessageInfo) {
        String multicastPermission = chatMessageInfo.getReceiverName().charAt(0) == '@' ? chatMessageInfo.getReceiverName().substring(1) : null;
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (multicastPermission != null) {
                if (!onlinePlayer.hasPermission(multicastPermission)) continue;
            }
            if(chatMessageInfo.getMessage().contains(
                    plugin.config.getChatFormats(onlinePlayer).get(0).mention_format().replace("%player%", onlinePlayer.getName())
            )){
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2.0f);
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
                builder -> builder.matchLiteral("%message%").replacement(toBeReplaced)
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
                        builder -> builder.matchLiteral("%message%").replacement(toBeReplaced)
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

