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

    @Comment({"RedisChat storage type, can be REDIS or MySQL+PM",
            "If you use Mysql you need a proxy. The plugin will send the data to the proxy via pluginmessages",
            "If you use REDIS you don't need any proxy, this is the recommended option"})
    public String dataMedium = DataType.REDIS.keyName;
    @Comment("Leave password or user empty if you don't have a password or user")
    public RedisSettings redis = new RedisSettings("localhost",
            6379,
            "",
            "",
            1,
            1000,
            "RedisChat");
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
            "<aqua>@%player%</aqua>",
            "<bold><click:open_url:%link%>[Click to open URL (be careful)]</click></bold>",
            "<gold>StaffChat <dark_gray>» <white>%message%"
    ));
    @Comment({"Here you can blacklist some terms (like swears, insults and unwanted urls)", "They will be replaced with a *", "You can use the regex syntax and the * wildcard"})
    public List<String> regex_blacklist = List.of("discord.gg/.*");
    @Comment({"Title of the ShowInventory GUI"})
    public String inv_title = "Inventory of %player%";
    @Comment("There are some others chat formats, like broadcast and clear chat messages")
    public String broadcast_format = "<red>Announce <dark_gray>» <white>%message%";
    @Comment("This message will be sent to all players when the chat is cleared")
    public String clear_chat_message = "<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared";
    @Comment("Here you can set the number of messages that a player can send without being rate limited")
    public int rate_limit = 3;
    @Comment("Here you can set the time in seconds that a player can send the number of messages specified in rate_limit")
    public int rate_limit_time_seconds = 5;
    @Comment("Messages with this prefix will be sent to staff chat")
    public String staffChatPrefix = "!";
    @Comment("Re-enables bukkit color glitches for colored placeholders")
    public boolean enablePlaceholderGlitch = false;
    @Comment("Toggle debug mode (by default is false)")
    public boolean debug = false;


    public record RedisSettings(String host, int port, String user, String password, int database, int timeout,
                                String clientName) {
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

    public @NotNull List<ChatFormat> getChatFormats(CommandSender p) {
        List<ChatFormat> chatFormatList = formats.stream().filter(format -> p.hasPermission(format.permission())).toList();
        if (chatFormatList.isEmpty()) {
            Bukkit.getLogger().info("No format found for " + p.getName());
            return List.of();
        }
        return chatFormatList;
    }

    public DataType getDataMedium() {
        return DataType.fromString(dataMedium.toUpperCase());
    }

    public enum DataType {
        MYSQL("MYSQL+PM"),
        REDIS("REDIS");
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