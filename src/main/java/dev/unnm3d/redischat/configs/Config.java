package dev.unnm3d.redischat.configs;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Configuration
public final class Config {

    @Comment({"Redis uri", "Example: redis://user:password@localhost:6379"})
    public Redis redis = new Redis("redis://localhost:6379/0?timeout=1s&clientName=RedisChat");
    @Comment("Webeditor URL")
    public String webEditorUrl = "https://webui.advntr.dev/";
    @Comment({"Here you can decide your chat format", "Permission format is overridden on descending order", "(if a player has default and vip, if default is the first element, vip will be ignored)"})
    public List<ChatFormat> formats = List.of(new ChatFormat("redischat.default",
            "<click:suggest_command:/msg %player_name%><hover:show_text:'" +
                    "<reset>Information | <white>%player_displayname%<br>" +
                    "<gold><bold>➧</bold> Money<reset>: <white>%vault_eco_balance% <gold>✵<br>" +
                    "<br><reset><underlined>Click to send a message" +
                    "'><white>%vault_prefix%%player_displayname%%luckperms_suffix%</click> <dark_gray>» <reset>%message%",
            "<dark_aqua>MSG <white>(<reset>You <white>to <green>%receiver%<white>)<reset>: <white>%message%",
            "<dark_aqua>MSG <white>(<green>%sender% <white>to <reset>You<white>)<reset>: <white>%message%",
            "<click:run_command:%command%>[Open the inventory of %player%]</click>",
            "<click:run_command:%command%>[%item_name% of %player%]</click>",
            "<click:run_command:%command%>[Open the enderchest of %player%]</click>",
            "<aqua>@%player%</aqua>",
            "<bold><click:open_url:%link%>[Click to open URL (be careful)]</click></bold>",
            "<gold>StaffChat <dark_gray>» <white>%message%"
    ));
    @Comment({
            "Announcer configurations",
            "delay and interval are in seconds",
            "If you want to disable an announce, just remove it from the list",
            "If you specify a permission, only players with that permission will see the announce. Keep it empty to make it public",
    })
    public List<Announce> announces = List.of(new Announce("default", "<red>RedisChat Announce: <br><white>lorem ipsum dolor sit amet", "", 5, 300));
    @Comment({"Here you can create your own placeholders", "You can give them an identifier, which will go under the format <>", "You can give them actions, like click url"})
    public Map<String, String> placeholders = Map.of("discord", "<click:open_url:https://discord.gg/uq6bBqAQ>Click to join our discord server</click>");
    @Comment({"Here you can blacklist some terms (like swears, insults and unwanted urls)", "They will be replaced with a *", "You can use the regex syntax and the * wildcard"})
    public List<String> regex_blacklist = List.of("discord.gg/.*");
    @Comment({"Here you can the decide the titles of the GUI", "These titles will be shown on the top of the GUI"})
    public String inv_title = "Inventory of %player%";
    public String item_title = "Item of %player%";
    public String ec_title = "Enderchest of %player%";
    @Comment("There are some others chat formats, like broadcast and clear chat messages")
    public String broadcast_format = "<red>Announce <dark_gray>» <white>%message%";
    public String clear_chat_message = "<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared";
    @Comment("Here you can decide the time between two messages of the same player")
    public int rate_limit = 3;
    public int rate_limit_time_seconds = 5;
    public String staffChatPrefix = "!";
    @Comment("Enabling this")
    public boolean legacyColorCodesSupport = false;
    public boolean enableMails = false;
    @Comment("Toggle debug mode (by default is false)")
    public boolean debug = false;


    public record Redis(
            String redisUri) {
    }

    public record ChatFormat(
            String permission,
            String format,
            String private_format,
            String receive_private_format,
            String inventory_format,
            String item_format,
            String enderchest_format,
            String mention_format,
            String link_format,
            String staff_chat_format) {
    }

    public record Announce(
            String announceName,
            String message,
            String permission,
            int delay,
            int interval) {
    }

    public @NotNull List<ChatFormat> getChatFormats(CommandSender p) {
        List<Config.ChatFormat> chatFormatList = formats.stream().filter(format -> p.hasPermission(format.permission())).toList();
        if (chatFormatList.isEmpty()) {
            Bukkit.getLogger().info("No format found for " + p.getName());
            return List.of();
        }
        return chatFormatList;
    }
}