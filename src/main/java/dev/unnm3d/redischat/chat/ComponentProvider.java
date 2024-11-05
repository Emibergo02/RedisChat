package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.TagResolverIntegration;
import dev.unnm3d.redischat.utils.ItemNameProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ComponentProvider {
    private final RedisChat plugin;
    private final MiniMessage miniMessage;
    @Getter
    private final TagResolver standardTagResolver;
    private final Map<CommandSender, List<Component>> cacheBlocked;
    private final List<TagResolverIntegration> tagResolverIntegrationList;
    @Getter
    private final BukkitAudiences bukkitAudiences;
    @Getter
    private final ItemNameProvider itemNameProvider;


    public ComponentProvider(RedisChat plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.cacheBlocked = Collections.synchronizedMap(new WeakHashMap<>());
        this.standardTagResolver = StandardTags.defaults();
        this.tagResolverIntegrationList = new ArrayList<>();
        this.bukkitAudiences = BukkitAudiences.create(plugin);
        this.itemNameProvider = new ItemNameProvider(plugin.config.useItemName);
    }

    /**
     * Add a custom tag resolver integration
     *
     * @param integration The integration to add
     */
    public void addResolverIntegration(TagResolverIntegration integration) {
        this.tagResolverIntegrationList.add(integration);
    }

    /**
     * Remove a custom tag resolver integration
     *
     * @param integration The integration to add
     */
    public void removeResolverIntegration(TagResolverIntegration integration) {
        this.tagResolverIntegrationList.remove(integration);
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

    public @NotNull Component parse(@Nullable CommandSender player, @NotNull String text, boolean parsePlaceholders, boolean parseMentions, boolean parseLinks, @NotNull TagResolver... tagResolvers) {
        Map.Entry<String, Component> parsedLinks = new AbstractMap.SimpleEntry<>(null, null);

        final ChatFormat format = plugin.config.getChatFormat(player);

        if (parseLinks) {
            parsedLinks = parseLinks(text, format);
            text = parsedLinks.getKey();
        }
        if (parseMentions) {
            text = parseMentions(text, format, player);
        }

        if (player == null || //Is without permissions or if it has permissions
                player.hasPermission(Permissions.USE_FORMATTING.getPermission())) {
            text = parseLegacy(text, true);
        }

        Component finalComponent = parsePlaceholders ?
                parsePlaceholders(player, parseResolverIntegrations(text), tagResolvers) :
                miniMessage.deserialize(text.replace('§', '&'), tagResolvers);

        if (parsedLinks.getValue() != null) {
            Map.Entry<String, Component> finalParsedLinks = parsedLinks;
            finalComponent = finalComponent.replaceText(rTextBuilder ->
                    rTextBuilder.matchLiteral("%link%")
                            .replacement(finalParsedLinks.getValue()));
        }
        return finalComponent;
    }

    public String replaceAmpersandCodesWithSection(String text) {
        final char[] b = text.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            //If the character is an ampersand replace it with a section symbol
            //In case of a color code, put it lower case for more compatibility with MiniMessage Legacy Serializer
            if (b[i] == '§' || (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx#".indexOf(b[i + 1]) > -1)) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    /**
     * Parse placeholders
     *
     * @param cmdSender    The command sender to parse the placeholders for
     * @param text         The text to parse
     * @param tagResolvers The tag resolvers to use
     * @return The parsed text
     */
    public @NotNull Component parsePlaceholders(@Nullable CommandSender cmdSender, @NotNull String text, TagResolver... tagResolvers) {
        final String[] stringPlaceholders = text.split("%");
        final LinkedHashMap<String, Component> placeholders = new LinkedHashMap<>();
        int placeholderStep = 1;
        // we need to split the text by % and then check if the placeholder is a placeholder or not
        for (int i = 0; i < stringPlaceholders.length; i++) {
            if (i % 2 == placeholderStep) {
                final String placeholderStringToBeReplaced = "%" + stringPlaceholders[i] + "%";
                String parsedPlaceH;
                try {
                    parsedPlaceH = replaceAmpersandCodesWithSection(
                            cmdSender instanceof OfflinePlayer offlinePlayer
                                    ? PlaceholderAPI.setPlaceholders(offlinePlayer, placeholderStringToBeReplaced)
                                    : PlaceholderAPI.setPlaceholders(null, placeholderStringToBeReplaced)
                    );
                } catch (Exception e) {
                    plugin.getLogger().warning("Error while parsing placeholder " + placeholderStringToBeReplaced + ": " + e.getMessage());
                    parsedPlaceH = placeholderStringToBeReplaced;
                }

                //If the placeholder is not a valid placeholder skip to the next "%"
                if (parsedPlaceH.equals(placeholderStringToBeReplaced)) {
                    placeholderStep = Math.abs(placeholderStep - 1);
                    continue;
                }

                boolean containsMiniMessageTags = MiniMessage.miniMessage().stripTags(parsedPlaceH).length()
                        != parsedPlaceH.length();
                boolean hasLegacyColors = parsedPlaceH.contains("§");

                //The objective is to glitch the color only if the color is a legacy color code
                //(by default is glitched since the beginning of Minecraft)
                if (plugin.config.enablePlaceholderGlitch && !containsMiniMessageTags) {
                    text = text.replace(placeholderStringToBeReplaced, miniMessage.serialize(
                            //Translate legacy color codes to MiniMessage
                            LegacyComponentSerializer.legacySection().deserialize(parsedPlaceH)
                    ));
                } else if (hasLegacyColors) {
                    //Colored placeholder needs to be pasted after the normal text is parsed
                    placeholders.put(placeholderStringToBeReplaced, LegacyComponentSerializer.legacySection().deserialize(parsedPlaceH));
                } else {
                    text = text.replace(placeholderStringToBeReplaced, parsedPlaceH);
                }
            }
        }

        Component answer = miniMessage.deserialize(text, tagResolvers);
        for (String placeholder : placeholders.keySet()) {
            answer = answer.replaceText(rBuilder -> rBuilder
                    .matchLiteral(placeholder)//Replace string with components (only if enablePlaceholderGlitch is false)
                    .replacement(placeholders.get(placeholder))
            );
        }
        return answer;
    }

    public Component parseChatMessageFormat(@NotNull CommandSender cmdSender, @NotNull String text) {
        Component component = parsePlaceholders(cmdSender,
                parseResolverIntegrations(
                        parseLegacy(text, true)), this.standardTagResolver);

        for (Map.Entry<String, String> replacementEntry : plugin.config.components.entrySet()) {
            component = component.replaceText(rBuilder -> rBuilder
                    .matchLiteral("{" + replacementEntry.getKey() + "}")
                    .replacement(parsePlaceholders(cmdSender,
                            parseResolverIntegrations(
                                    parseLegacy(replacementEntry.getValue(), true)), this.standardTagResolver)));
        }
        return component;
    }

    public Component parseChatMessageContent(@NotNull CommandSender cmdSender, @NotNull String text) {
        Component component = parse(cmdSender,
                invShareFormatting(cmdSender, text),
                cmdSender.hasPermission(Permissions.USE_FORMATTING.getPermission()),
                true, true, getRedisChatTagResolver(cmdSender));

        if (!cmdSender.hasPermission(Permissions.USE_CUSTOM_PLACEHOLDERS.getPermission())) return component;

        for (Map.Entry<String, String> replacementEntry : plugin.config.placeholders.entrySet()) {
            component = component.replaceText(rBuilder ->
                    rBuilder.matchLiteral(replacementEntry.getKey())
                            .replacement(parsePlaceholders(cmdSender,
                                    parseLegacy(replacementEntry.getValue(), true), this.standardTagResolver)));
        }
        return component;
    }

    /**
     * Purge MiniMessage tags from a text
     * Use this to prevent unprivileged players from using tags
     *
     * @param text The text to purge
     * @return The purged text
     */
    public @NotNull String purgeTags(@NotNull String text) {
        return miniMessage.stripTags(text, TagResolver.standard());
    }

    /**
     * Get the tag resolver for the invshare feature
     *
     * @param player The player to get the tag resolver for
     * @return The tag resolver
     */
    public @NotNull TagResolver getRedisChatTagResolver(@NotNull CommandSender player) {

        final TagResolver.Builder builder = TagResolver.builder();

        if (player.hasPermission(Permissions.USE_INVENTORY.getPermission())) {
            String toParseInv = plugin.config.inventoryFormat;
            toParseInv = toParseInv.replace("%player%", player.getName());
            toParseInv = toParseInv.replace("%command%", "/invshare " + player.getName() + "-inventory");
            TagResolver inv = Placeholder.component("inv", parse(player, toParseInv, true, false, false, this.standardTagResolver));
            builder.resolver(inv);
        }

        if (player.hasPermission(Permissions.USE_ITEM.getPermission()) && player instanceof Player p) {
            String toParseItem = plugin.config.itemFormat;
            toParseItem = toParseItem.replace("%player%", player.getName());
            toParseItem = toParseItem.replace("%command%", "/invshare " + player.getName() + "-item");
            Component toParseItemComponent = parse(player, toParseItem, true, false, false, this.standardTagResolver);

            final ItemMeta itemMeta = p.getInventory().getItemInMainHand().getItemMeta();

            if (itemMeta != null && itemNameProvider.hasItemName(itemMeta)) {
                toParseItemComponent = toParseItemComponent.replaceText(rTextBuilder ->
                        rTextBuilder.matchLiteral("%item_name%")
                                .replacement(LegacyComponentSerializer.legacySection()
                                        .deserialize(replaceAmpersandCodesWithSection(itemNameProvider.getItemName(itemMeta)))));
            } else {
                toParseItemComponent = toParseItemComponent.replaceText(rTextBuilder ->
                        rTextBuilder.matchLiteral("%item_name%")
                                .replacement(Component.translatable(
                                        p.getInventory().getItemInMainHand().getType().getTranslationKey())));
            }
            TagResolver item = Placeholder.component("item", toParseItemComponent);
            builder.resolver(item);
        }

        if (player.hasPermission(Permissions.USE_ENDERCHEST.getPermission())) {
            String toParseEnderChest = plugin.config.enderChestFormat;
            toParseEnderChest = toParseEnderChest.replace("%player%", player.getName());
            toParseEnderChest = toParseEnderChest.replace("%command%", "/invshare " + player.getName() + "-enderchest");
            TagResolver ec = Placeholder.component("ec", parse(player, toParseEnderChest, true, false, false, this.standardTagResolver));
            builder.resolver(ec);
        }

        return builder.build();
    }

    /**
     * Parse mentions
     *
     * @param text   The text to parse
     * @param format The format to use
     * @return The parsed text
     */
    private String parseMentions(@NotNull String text, @NotNull ChatFormat format, CommandSender mentioner) {
        String toParse = text;
        for (String playerName : plugin.getPlayerListManager().getPlayerList(mentioner)) {
            playerName = playerName.replace("*", "\\*");
            Pattern p = Pattern.compile("(^" + playerName + "|" + playerName + "$|\\s" + playerName + "\\s)");
            Matcher m = p.matcher(text);
            if (m.find()) {
                String replacing = m.group();
                replacing = replacing.replace(m.group().trim(), format.mention_format().replace("%player%", playerName));
                toParse = toParse.replace(m.group(), replacing);
            }
        }

        return toParse.replace("\\*", "*");
    }

    /**
     * Parse links
     *
     * @param text   The text to parse
     * @param format The link format to use
     * @return The parsed text with %link% as a placeholder for the link component and the link component
     */
    private Map.Entry<String, Component> parseLinks(@NotNull String text, @NotNull ChatFormat format) {
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

        return new AbstractMap.SimpleEntry<>(text, linkComponent);
    }

    /**
     * Parse text with the integration tag resolvers (Oraxen and other plugins)
     *
     * @param text The text to parse
     * @return The parsed text
     */
    private String parseResolverIntegrations(String text) {
        if (!plugin.config.useTagsIntegration) return text;
        try {
            for (TagResolverIntegration resolver : this.tagResolverIntegrationList) {
                text = resolver.parseTags(text).replace("\\<", "<");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error while parsing tags: " + e.getMessage());
            Bukkit.getLogger().warning("If you don't want any tag integration disable it in the config");
        }
        return text;
    }

    /**
     * Parse legacy color codes (§ and ampersand)
     *
     * @param text The text to parse
     * @return The parsed text
     */
    public @NotNull String parseLegacy(@NotNull String text, boolean parseAmpersand) {

        text = miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(
                parseAmpersand ? replaceAmpersandCodesWithSection(text) : text
        ));
        return text.replace("\\<", "<");

    }

    /**
     * Send a component to a player or caches it if the player has paused the chat
     *
     * @param player    The player to send the component to
     * @param component The component to send
     */
    public void sendComponentOrCache(@NotNull CommandSender player, @NotNull Component component) {
        if (cacheBlocked.computeIfPresent(player,
                (player1, components) -> {
                    components.add(component);
                    return components;
                }) == null) {//If the player is not blocked
            plugin.getComponentProvider().sendMessage(player, component);
        }
    }

    public String invShareFormatting(CommandSender sender, String message) {
        if (!(sender instanceof Player player)) return message;

        //Placeholder aliases
        message = message.replace("<inventory>", "<inv>")
                .replace("<i>", "<item>")
                .replace("<enderchest>", "<ec>");

        if (plugin.config.interactiveChatNostalgia) {
            message = message.replace("[inv]", "<inv>")
                    .replace("[inventory]", "<inv>")
                    .replace("[i]", "<item>")
                    .replace("[item]", "<item>")
                    .replace("[enderchest]", "<ec>")
                    .replace("[ec]", "<ec>");
        }
        if (message.contains("<inv>")) {
            plugin.getDataManager().addInventory(player.getName(), player.getInventory().getContents());
        }
        if (message.contains("<item>")) {
            plugin.getDataManager().addItem(player.getName(), player.getInventory().getItemInMainHand());
        }
        if (message.contains("<ec>")) {
            plugin.getDataManager().addEnderchest(player.getName(), player.getEnderChest().getContents());
        }
        return message;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void logComponent(Component component) {
        if (!plugin.config.chatLogging) {
            bukkitAudiences.console().sendMessage(component);
            return;
        }

        //Log to history file
        Date now = new Date();
        File logFile = new File(plugin.getDataFolder(), "logs" + File.separator +
                "chatlog_" + new SimpleDateFormat("dd-MM-yyyy").format(now) + ".log");
        logFile.getParentFile().mkdirs();

        FileWriter fw;
        try {
            fw = new FileWriter(logFile, true);

            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("[" + new SimpleDateFormat("HH:mm:ss").format(now) + "] " + PlainTextComponentSerializer.plainText().serialize(component));
            bw.newLine();
            bw.close();

        } catch (IOException e) {
            plugin.getLogger().warning("Error while logging to history: " + e.getMessage());
        }
    }

    public void pauseChat(@NotNull Player player) {
        cacheBlocked.put(player, new ArrayList<>());
    }


    public boolean isPaused(@NotNull Player player) {
        return cacheBlocked.containsKey(player);
    }

    /**
     * Unpause the chat for a player and send all cached components
     *
     * @param player The player to unpause the chat for
     */
    public void unpauseChat(@NotNull Player player) {
        if (cacheBlocked.containsKey(player)) {
            for (Component component : cacheBlocked.remove(player)) {
                plugin.getComponentProvider().sendMessage(player, component);
            }
        }
    }

    public void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, MiniMessage.miniMessage().deserialize(message));
    }

    public void sendMessage(CommandSender sender, Component component) {
        bukkitAudiences.sender(sender).sendMessage(component);
    }

}

