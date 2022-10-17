package dev.unnm3d.kalyachat.chat;

import dev.unnm3d.kalyachat.Config;
import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.commands.PlayerListManager;
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

public class TextParser {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static Component parse(String text, TagResolver... tagResolvers) {
        return parse(null, text, tagResolvers);
    }

    public static Component parse(String text) {
        return parse(text, StandardTags.defaults());
    }

    public static Component parse(CommandSender player, String text, TagResolver... tagResolvers) {
        return miniMessage.deserialize(parsePlaceholders(player, parseMentions(text, KalyaChat.config.formats.get(0))), tagResolvers);
    }

    public static Component parse(CommandSender player, String text, boolean parsePlaceholders, TagResolver... tagResolvers) {
        if (!parsePlaceholders)
            return miniMessage.deserialize(parseMentions(text, KalyaChat.config.formats.get(0)), tagResolvers);
        else
            return parse(player, text, tagResolvers);
    }
    public static Component parse(CommandSender player, String text) {
        return parse(player, text, StandardTags.defaults());
    }

    public static String parsePlaceholders(CommandSender cmdSender, String text) {
        String message=
                cmdSender instanceof OfflinePlayer
                ?PlaceholderAPI.setPlaceholders((OfflinePlayer) cmdSender, text)
                :PlaceholderAPI.setPlaceholders(null, text);
        return miniMessage.serialize(LegacyComponentSerializer.legacySection().deserialize(message)).replace("\\","");
    }
    public static String purify(String text) {
        return miniMessage.stripTags(text, TagResolver.standard());
    }

    public static TagResolver getCustomTagResolver(CommandSender player, Config.ChatFormat chatFormat) {

        TagResolver.Builder builder = TagResolver.builder();

        String toParse = chatFormat.inventory_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-inventory");
        TagResolver inv = Placeholder.component("inv", parse(player, toParse));

        toParse = chatFormat.item_format();
        toParse = toParse.replace("%player%", player.getName());
        if(player instanceof Player p){

            if(!p.getInventory().getItemInMainHand().getType().isAir())
                if (p.getInventory().getItemInMainHand().getItemMeta().hasDisplayName())
                    toParse = toParse.replace("%item_name%", p.getInventory().getItemInMainHand().getItemMeta().getDisplayName());
                else {
                    toParse = toParse.replace("%item_name%", p.getInventory().getItemInMainHand().getType().name().toLowerCase().replace("_", " "));
                }
            else{
                System.out.println(p.getInventory().getItemInMainHand().getType().name());
                toParse = toParse.replace("%item_name%", "Nothing");
            }

        }

        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-item");
        TagResolver item = Placeholder.component("item", parse(player, toParse));

        toParse = chatFormat.enderchest_format();
        toParse = toParse.replace("%player%", player.getName());
        toParse = toParse.replace("%command%", "/invshare " + player.getName() + "-enderchest");
        TagResolver ec = Placeholder.component("ec", parse(player, toParse));

        KalyaChat.config.placeholders.forEach((key, value) -> builder.resolver(Placeholder.component(key, parse(player, value))));

        builder.resolver(inv);
        builder.resolver(item);
        builder.resolver(ec);

        return builder.build();
    }

    public static String parseMentions(String text, Config.ChatFormat format) {
        String toParse = text;
        for(String playerName: PlayerListManager.getPlayerList()){
            if (text.contains(" " + playerName + " ")||text.startsWith(playerName + " ")||text.endsWith(" " + playerName)) {
                toParse = toParse.replace(playerName, format.mention_format().replace("%player%", playerName));
            }
        }
        return toParse;
    }


    public static String sanitize(String message) {
        for (String regex : KalyaChat.config.regex_blacklist) {
            message = message.replaceAll(regex, "***");
        }
        return message;
    }
}

