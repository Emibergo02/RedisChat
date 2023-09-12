package dev.unnm3d.redischat.configs;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.chat.ChatFormat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Configuration
public final class Config {

    @Comment({"RedisChat storage type, can be REDIS , MySQL+PM or H2+PM (PM means PluginMessages)",
            "If you use Mysql you need a proxy. The plugin will send the data to the proxy via pluginmessages",
            "If you use REDIS you don't need any proxy, THIS IS THE RECOMMENDED AND MOST EFFICIENT OPTION"})
    public String dataMedium = DataType.H2.keyName;
    @Comment("Leave password or user empty if you don't have a password or user")
    public RedisSettings redis = new RedisSettings("localhost",
            6379,
            "",
            "",
            0,
            1000,
            "RedisChat",
            3);
    public Mysql mysql = new Mysql("127.0.0.1",
            3306,
            "redischat",
            "com.mysql.cj.jdbc.Driver",
            "?autoReconnect=true"
                    + "&useSSL=false"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8",
            "root",
            "password",
            5,
            5,
            1800000,
            30000,
            20000);
    @Comment({"The cluster id, if you have multiple servers you need to set a different id for each group of servers",
            "Doesn't work completely if you're using something different than redis"})
    public int clusterId = 0;
    @Comment("Webeditor URL")
    public String webEditorUrl = "https://webui.advntr.dev/";
    @Comment("Enables & (ampersand) and § (section) color codes")
    public boolean legacyColorCodesSupport = true;
    @Comment("Enables /rmail /mail and the whole feature")
    public boolean enableMails = true;
    @Comment({"Use RedisChat for join and quit messages",
            "The quit message will be delayed because it might be a early reconnection to one of the servers using RedisChat"})
    public boolean enableQuitJoinMessages = true;
    @Comment("Re-enables bukkit color glitches for colored placeholders")
    public boolean enablePlaceholderGlitch = false;
    @Comment("If true, RedisChat will log public chat messages")
    public boolean chatLogging = false;
    @Comment({"Here you can decide your chat format", "Permission format is overridden on descending order", "(if a player has default and vip, if default is the first element, vip will be ignored)"})
    public List<ChatFormat> formats = List.of(new ChatFormat("redischat.default",
            "<click:suggest_command:/msg %player_name%><hover:show_text:'<gray>Info" +
                    "|</gray> <white>%player_displayname%</white> <br>↪ <gold>Money</gold>: <white>%vault_eco_balance%$</white>" +
                    "<br>↪ <green>Server</green>: <white>%server_name%</white> <br><br><gray>Click" +
                    "to send a private message</gray>'>%vault_prefix% %player_name% %vault_suffix%</click>" +
                    "<dark_gray>» <reset><gray>%message%",
            "<white>✉<green>⬆</green></white> <dark_aqua>MSG <grey>(Me ➺ <green>%receiver%<grey>): <white>%message%",
            "<white>✉<green>⬇</green></white> <dark_aqua>MSG <grey>(<green>%sender%<grey> ➺ Me): <white>%message%",
            "<aqua>@%player%</aqua>",
            "<aqua><click:open_url:%link%>[Open web page <red>(be careful)</red>]</aqua>",
            "<green>%player_name% joined the server",
            "<red>%player_name% is no longer online"
    ));
    @Comment({
            "Announcer configurations",
            "delay and interval are in seconds",
            "If you want to disable an announce, just remove it from the list, remember that in yaml [] is an empty list",
            "If you specify a permission, only players with that permission will see the announce. Keep it empty to make it public",
    })
    public List<Announce> announces = List.of(new Announce("default", "<yellow>RedisChat</yellow> <gray>»</gray><red>To EssentialsX and CMI users: <aqua><br>disable <gold>/msg, /reply, /broadcast, /ignore, etc</gold> commands inside CMI and EssentialsX<br>Or RedisChat commands <red>will <u>not</u> work</red>!!!</aqua>", "", 5, 300));
    @Comment({"Here you can create your own placeholders", "You can give them an identifier, which will go under the format <>", "You can give them actions, like click url"})
    public Map<String, String> placeholders = Map.of(
            "discord", "<click:open_url:https://discord.gg/C8d7EqQz>Click to join our discord server</click>",
            "position", "<white><blue>Server:</blue> %server_name% <aqua>World:</aqua> %player_world% <gold>X:</gold> %player_x% <gold>Y:</gold> %player_y% <gold>Z:</gold> %player_z%</white>");
    @Comment({"Here you can blacklist some terms (like swears, insults and unwanted urls)", "They will be replaced with a *", "You can use the regex syntax and the * wildcard"})
    public List<String> regex_blacklist = List.of("discord.gg/.*");
    @Comment({"What to replace the blacklisted words with"})
    public String blacklistReplacement = "<obf>*****</obf>";
    @Comment({"Private message notification sound",
            "You can find the list of sounds here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html",
            "Leave it empty to disable the sound"})
    public String privateMessageNotificationSound = "BLOCK_NOTE_BLOCK_BELL";
    @Comment({"Title of the ShowInventory GUI"})
    public String inv_title = "Inventory of %player%";
    @Comment({"Title of the ShowItem GUI"})
    public String item_title = "Item of %player%";
    @Comment({"Title of the ShowShulkerBox GUI"})
    public String shulker_title = "Shulker of %player%";
    @Comment({"Title of the ShowEnderchest GUI"})
    public String ec_title = "Enderchest of %player%";
    @Comment("There are some others chat formats, like broadcast and clear chat messages")
    public String broadcast_format = "<red>Announce <dark_gray>» <white>%message%";
    @Comment({"This message will be sent when a player logs in for the first time",
            "Put an empty string \"\" to disable this feature"})
    public String first_join_message = "<red>Welcome to the server, <white>%player_name%<red>!";
    @Comment("This message will be sent to all players when the chat is cleared")
    public String clear_chat_message = "<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared";
    @Comment("Here you can set the number of messages that a player can send without being rate limited")
    public int rate_limit = 3;
    @Comment("Here you can set the time in seconds that a player can send the number of messages specified in rate_limit")
    public int rate_limit_time_seconds = 5;
    @Comment("Messages with this prefix will be sent to staff chat")
    public String staffChatPrefix = "!";
    @Comment("The format of the staff chat messages")
    public String staffChatFormat = "<gold>StaffChat </gold> : %message%";
    @Comment("The discord webhook of the staff chat")
    public String staffChatDiscordWebhook = "";
    public String inventoryFormat = "<click:run_command:%command%><gold>[%player%'s Inventory]</gold></click>";
    public String itemFormat = "<click:run_command:%command%>[%item_name%]</click>";
    public String enderChestFormat = "<click:run_command:%command%><light_purple>[%player%'s EnderChest]</light_purple></click>";
    @Comment("The discord webhook of the public chat")
    public String publicDiscordWebhook = "";
    @Comment("The format of the timestamp in mails (by default is like 31/07/2023 15:24)")
    public String mailTimestampFormat = "dd/MM/yyyy HH:mm";
    @Comment("The timezone of the timestamp in mails (by default is Central European Time)")
    public String mailTimestampZone = "UTC+1";
    @Comment("Those commands will be disabled")
    public List<String> disabledCommands = List.of();
    @Comment("The [inv], [item] and [ec] placeholders will be considered as minimessage tags")
    public boolean interactiveChatNostalgia = false;

    @Comment("Reply only to the last player you have messaged")
    public boolean replyToLastMessaged = false;
    @Comment("Toggle debug mode (by default is false)")
    public boolean debug = false;


    public record RedisSettings(String host, int port, String user, String password, int database, int timeout,
                                String clientName, int poolSize) {
    }

    public record Mysql(
            String host,
            int port,
            String database,
            String driverClass,
            String connectionParameters,
            String username,
            String password,
            int poolSize,
            int poolIdle,
            long poolLifetime,
            long poolKeepAlive,
            long poolTimeout) {
    }


    public record Announce(
            String announceName,
            String message,
            String channelName,
            int delay,
            int interval) {
    }

    public @NotNull List<ChatFormat> getChatFormats(@NotNull CommandSender p) {
        List<ChatFormat> chatFormatList = formats.stream().filter(format -> p.hasPermission(format.permission())).toList();
        if (chatFormatList.isEmpty()) {
            Bukkit.getLogger().info("No format found for " + p.getName());
            return List.of();
        }
        return chatFormatList;
    }

    public DataType getDataType() {
        return DataType.fromString(dataMedium.toUpperCase());
    }

    public enum DataType {
        MYSQL("MYSQL+PM"),
        REDIS("REDIS"),
        H2("H2+PM"),
        ;
        private final String keyName;

        /**
         * @param keyName the name of the key
         */
        DataType(final String keyName) {
            this.keyName = keyName;
        }

        public static DataType fromString(String text) {
            for (DataType b : DataType.values()) {
                if (b.keyName.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return keyName;
        }
    }
}