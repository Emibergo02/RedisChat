package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.configs.Config;
import lombok.AllArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ComponentProvider {
    private final MiniMessage miniMessage;
    private final RedisChat plugin;
    private final BukkitAudiences bukkitAudiences;
    private final TagResolver standardTagResolver;

    public ComponentProvider(RedisChat plugin) {

        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.bukkitAudiences = BukkitAudiences.create(plugin);

        TagResolver standardTagResolver1 = StandardTags.defaults();
        //try {
        //    Field oraxenTagResolver = Class.forName("io.th0rgal.oraxen.utils.AdventureUtils").getField("OraxenTagResolver");
        //    standardTagResolver1 = (TagResolver) oraxenTagResolver.get(null);
        //} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
        //}
        this.standardTagResolver = standardTagResolver1;
    }

    public Component parse(String text, TagResolver... tagResolvers) {
        return parse(null, text, tagResolvers);
    }

    public Component parse(String text) {
        return parse(text, this.standardTagResolver);
    }

    public Component parse(CommandSender player, String text, TagResolver... tagResolvers) {
        return miniMessage.deserialize(
                parsePlaceholders(player,
                        parseMentions(
                                parseLinks(text, plugin.config.formats.get(0)),
                                plugin.config.formats.get(0)
                        )
                ), tagResolvers);
    }

    public Component parse(CommandSender player, String text, boolean parsePlaceholders, TagResolver... tagResolvers) {
        if (!parsePlaceholders) {
            return miniMessage.deserialize(
                    parseMentions(
                            parseLinks(text, plugin.config.formats.get(0)),
                            plugin.config.formats.get(0)
                    ), tagResolvers);
        }

        return parse(player, text, tagResolvers);
    }

    public Component parseWithoutMentions(CommandSender player, String text, boolean parseMentions, boolean parsePlaceholders, TagResolver... tagResolvers) {
        if (!parseMentions) {
            if (parsePlaceholders) {
                return miniMessage.deserialize(parsePlaceholders(player, text), tagResolvers);
            }
            return miniMessage.deserialize(text, tagResolvers);
        }

        return parse(player, text, parsePlaceholders, tagResolvers);
    }

    public Component parse(CommandSender player, String text) {
        return parse(player, text, this.standardTagResolver);
    }

    public String parsePlaceholders(CommandSender cmdSender, String text) {
        String message =
                cmdSender instanceof OfflinePlayer
                        ? PlaceholderAPI.setPlaceholders((OfflinePlayer) cmdSender, text)
                        : PlaceholderAPI.setPlaceholders(null, text);
        return miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(message)).replace("\\", "");
    }

    public String purgeTags(String text) {
        return miniMessage.stripTags(text, TagResolver.standard());
    }

    public TagResolver getCustomTagResolver(CommandSender player, Config.ChatFormat chatFormat) {

        TagResolver.Builder builder = TagResolver.builder();

        String toParse = chatFormat.inventory_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-inventory");
        TagResolver inv = Placeholder.component("inv", parseWithoutMentions(player, toParse, false, true, this.standardTagResolver));

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
        TagResolver item = Placeholder.component("item", parseWithoutMentions(player, toParse, false, true, this.standardTagResolver));

        toParse = chatFormat.enderchest_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-enderchest");
        TagResolver ec = Placeholder.component("ec", parseWithoutMentions(player, toParse, false, true, this.standardTagResolver));

        plugin.config.placeholders.forEach((key, value) -> builder.resolver(Placeholder.component(key, parse(player, value))));

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
            }
        }
        return toParse;
    }

    public String parseLinks(String text, Config.ChatFormat format) {
        String toParse = text;
        Pattern p = Pattern.compile("(https?://\\S+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            toParse = toParse.replace(m.group(), format.link_format().replace("%link%", m.group()));
        }
        return toParse;
    }

    public String sanitize(String message) {
        for (String regex : plugin.config.regex_blacklist) {
            message = message.replaceAll(regex, "***");
        }
        return message;
    }

    public void sendPublicChat(String serializedText) {
        bukkitAudiences.all().sendMessage(MiniMessage.miniMessage().deserialize(serializedText));
    }

    public void sendSpyChat(String receiverName, String senderName, Player watcher, String deserialize) {
        Component formatted = MiniMessage.miniMessage().deserialize(plugin.messages.spychat_format.replace("%receiver%", receiverName).replace("%sender%", senderName));

        //Parse into minimessage (placeholders, tags and mentions)
        Component toBeReplaced = parse(deserialize);
        //Put message into format
        formatted = formatted.replaceText(
                builder -> builder.match("%message%").replacement(toBeReplaced)
        );
        plugin.messages.sendMessage(watcher, formatted);
    }

    public void sendPrivateChat(String senderName, String receiverName, String text) {
        Player p = Bukkit.getPlayer(receiverName);
        if (p != null)
            if (p.isOnline()) {
                List<Config.ChatFormat> chatFormatList = plugin.config.getChatFormats(p);
                if (chatFormatList.isEmpty()) return;
                Component formatted = parse(null, chatFormatList.get(0).receive_private_format().replace("%receiver%", receiverName).replace("%sender%", senderName));
                Component toBeReplaced = parse(null, text);
                //Put message into format
                formatted = formatted.replaceText(
                        builder -> builder.match("%message%").replacement(toBeReplaced)
                );
                plugin.config.sendMessage(p, formatted);
            }

    }
}

