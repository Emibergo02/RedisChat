package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Config;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.commands.PlayerListManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static Component parse(String text, TagResolver... tagResolvers) {
        return parse(null, text, tagResolvers);
    }

    public static Component parse(String text) {
        return parse(text, StandardTags.defaults());
    }

    public static Component parse(CommandSender player, String text, TagResolver... tagResolvers) {
        return miniMessage.deserialize(
                parsePlaceholders(player,
                        parseMentions(
                                parseLinks(text, RedisChat.config.formats.get(0)),
                                RedisChat.config.formats.get(0))
                ), tagResolvers);
    }

    public static Component parse(CommandSender player, String text, boolean parsePlaceholders, TagResolver... tagResolvers) {
        if (!parsePlaceholders)
            return miniMessage.deserialize(
                    parseMentions(
                            parseLinks(text, RedisChat.config.formats.get(0)),
                            RedisChat.config.formats.get(0)
                    ), tagResolvers);
        else
            return parse(player, text, tagResolvers);
    }

    public static Component parseWithoutMentions(CommandSender player, String text, boolean parseMentions, boolean parsePlaceholders, TagResolver... tagResolvers) {
        if (!parseMentions) {
            if (parsePlaceholders)
                return miniMessage.deserialize(parsePlaceholders(player, text), tagResolvers);
            else
                return miniMessage.deserialize(text, tagResolvers);
        } else
            return parse(player, text, parsePlaceholders, tagResolvers);
    }

    public static Component parse(CommandSender player, String text) {
        return parse(player, text, StandardTags.defaults());
    }

    public static String parsePlaceholders(CommandSender cmdSender, String text) {
        String message =
                cmdSender instanceof OfflinePlayer
                        ? PlaceholderAPI.setPlaceholders((OfflinePlayer) cmdSender, text)
                        : PlaceholderAPI.setPlaceholders(null, text);
        return miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(message)).replace("\\", "");
    }

    public static String purgeTags(String text) {
        return miniMessage.stripTags(text, TagResolver.standard());
    }

    public static TagResolver getCustomTagResolver(CommandSender player, Config.ChatFormat chatFormat) {

        TagResolver.Builder builder = TagResolver.builder();

        String toParse = chatFormat.inventory_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-inventory");
        TagResolver inv = Placeholder.component("inv", parseWithoutMentions(player, toParse, false, true, StandardTags.defaults()));

        toParse = chatFormat.item_format();
        toParse = toParse.replace("%player%", player.getName());
        if (player instanceof Player p) {

            if (!p.getInventory().getItemInMainHand().getType().isAir())
                if (p.getInventory().getItemInMainHand().getItemMeta().hasDisplayName())
                    toParse = toParse.replace("%item_name%", p.getInventory().getItemInMainHand().getItemMeta().getDisplayName());
                else {
                    toParse = toParse.replace("%item_name%", p.getInventory().getItemInMainHand().getType().name().toLowerCase().replace("_", " "));
                }
            else {
                toParse = toParse.replace("%item_name%", "Nothing");
            }
        }
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-item");
        TagResolver item = Placeholder.component("item", parseWithoutMentions(player, toParse, false, true, StandardTags.defaults()));

        toParse = chatFormat.enderchest_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-enderchest");
        TagResolver ec = Placeholder.component("ec", parseWithoutMentions(player, toParse, false, true, StandardTags.defaults()));

        RedisChat.config.placeholders.forEach((key, value) -> builder.resolver(Placeholder.component(key, parse(player, value))));

        builder.resolver(inv);
        builder.resolver(item);
        builder.resolver(ec);

        return builder.build();
    }

    public static String parseMentions(String text, Config.ChatFormat format) {
        String toParse = text;

        for (String playerName : PlayerListManager.getPlayerList()) {
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
    public static String parseLinks(String text, Config.ChatFormat format){
        String toParse = text;
        Pattern p = Pattern.compile("(https?://\\S+)");
        Matcher m = p.matcher(text);
        if(m.find()){
            toParse = toParse.replace(m.group(), format.link_format().replace("%link%", m.group()));
        }
        return toParse;
    }

    public static String sanitize(String message) {
        for (String regex : RedisChat.config.regex_blacklist) {
            message = message.replaceAll(regex, "***");
        }
        return message;
    }
}

