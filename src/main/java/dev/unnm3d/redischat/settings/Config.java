package dev.unnm3d.redischat.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.chat.ChatFormat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Configuration
public final class Config implements ConfigValidator {

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
    public String clusterId = "0";
    @Comment("Webeditor URL")
    public String webEditorUrl = "https://webui.advntr.dev/";
    @Comment("Enables /rmail /mail and the whole feature")
    public boolean enableMails = true;
    @Comment("Register tag integrations (Like Oraxen Integration which is internal)")
    public boolean useTagsIntegration = false;
    @Comment({"Use RedisChat for join and quit messages",
            "The quit message will be delayed because it might be a early reconnection to one of the servers using RedisChat"})
    public boolean enableQuitJoinMessages = true;
    @Comment("Re-enables bukkit color glitches for colored placeholders")
    public boolean enablePlaceholderGlitch = true;
    @Comment("If true, RedisChat will log public chat messages")
    public boolean chatLogging = false;
    @Comment({"Here you can decide your chat format", "Permission format is overridden on descending order", "(if a player has default and vip, if default is the first element, vip will be ignored)"})
    public List<ChatFormat> formats = List.of(
            new ChatFormat("redischat.default",
                    "<click:suggest_command:/msg %player_name%><hover:show_text:'<gray>Info" +
                            "|</gray> <white>%player_displayname%</white> <br>↪ <gold>Money</gold>: <white>%vault_eco_balance%$</white>" +
                            "<br>↪ <green>Server</green>: <white>%server_name%</white> <br><br><gray>Click" +
                            "to send a private message</gray>'>%vault_prefix% %player_name%</click>" +
                            "<dark_gray>» <reset>%redischat_chat_color%%message%",
                    "<white>✉<green>⬆</green></white> <dark_aqua>MSG <grey>(Me ➺ <green>%receiver%<grey>): <white>%message%",
                    "<white>✉<green>⬇</green></white> <dark_aqua>MSG <grey>(<green>%sender%<grey> ➺ Me): <white>%message%",
                    "<aqua>@%player%</aqua>",
                    "<aqua><click:open_url:%link%>[Open web page <red>(be careful)</red>]</aqua>",
                    "<green>%player_name% joined the server",
                    "<red>%player_name% is no longer online"),
            new ChatFormat("redischat.staff",
                    "<click:suggest_command:/msg %player_name%><hover:show_text:'<gray>Info" +
                            "|</gray> <white>%player_displayname%</white> <br>↪ <gold>Money</gold>: <white>%vault_eco_balance%$</white>" +
                            "<br>↪ <green>Server</green>: <white>%server_name%</white> <br><br><gray>Click" +
                            "to send a private message</gray>'>%vault_prefix% %player_name%</click>" +
                            "<dark_gray> <gold>(STAFF)</gold>»</dark_gray> <gold>%message%",
                    "<white>✉<green>⬆</green></white> <dark_aqua>MSG <grey>(Me ➺ <green>%receiver%<grey>): <white>%message%",
                    "<white>✉<green>⬇</green></white> <dark_aqua>MSG <grey>(<green>%sender%<grey> ➺ Me): <white>%message%",
                    "<aqua>@%player% (staff)</aqua>",
                    "<aqua><click:open_url:%link%>%link%</aqua>",
                    "<green>%player_name% joined the server",
                    "<red>%player_name% is no longer online"

            ));

    @Comment("Fallback format if the player doesn't have any of the formats above")
    public ChatFormat defaultFormat = new ChatFormat("none",
            "No format » <reset><gray>%message%",
            "No format: Me ➺ <green>%receiver%<grey> : <white>%message%",
            "No format: <green>%sender%<grey> ➺ Me : <white>%message%",
            "<red>@%player%</red>",
            "No format: <aqua><click:open_url:%link%>[Open web page <red>(be careful)</red>]</aqua>",
            "No format: %player_name% joined the server",
            "No format: %player_name% is no longer online"
    );

    @Comment({
            "Announcer configurations",
            "delay and interval are in seconds",
            "If you want to disable an announce, just remove it from the list, remember that in yaml [] is an empty list",
            "If you specify a permission, only players with that permission will see the announce. Keep it empty to make it public",
    })
    public List<Announcement> announcer = List.of(new Announcement("default", "<yellow>RedisChat</yellow> <gray>»</gray><red>To EssentialsX and CMI users: <aqua><br>disable <gold>/msg, /reply, /broadcast, /ignore, etc</gold> commands inside CMI and EssentialsX<br>Or RedisChat commands <red>will <u>not</u> work</red>!!!</aqua>", "public", 5, 300));
    @Comment({"Here you can create your own placeholders", "You can give them an identifier, which will go under the format <>", "You can give them actions, like click url"})
    public Map<String, String> placeholders = new TreeMap<>(Map.ofEntries(
            Map.entry("*check*", "§a✔"),
            Map.entry("*cross*", "§c✖"),
            Map.entry("*star*", "★"),
            Map.entry("*caution*", "⚠"),
            Map.entry("*timer*", "⌛"),
            Map.entry("*clock*", "⌚"),
            Map.entry("*music*", "♫"),
            Map.entry("*peace*", "☮"),
            Map.entry("*hazard*", "☣"),
            Map.entry("*radioactive*", "☢"),
            Map.entry("*snow*", "❄"),
            Map.entry("*pirate*", "☠"),
            Map.entry("<<", "«"),
            Map.entry(">>", "»"),
            Map.entry(":)", "☺"),
            Map.entry(":(", "☹"),
            Map.entry("<3", "§c❤"),
            Map.entry("discord", "<click:open_url:https://discord.gg/C8d7EqQz>Click to join our discord server</click>"),
            Map.entry("position", "<white><blue>Server:</blue> %server_name% <aqua>World:</aqua> %player_world% <gold>X:</gold> %player_x% <gold>Y:</gold> %player_y% <gold>Z:</gold> %player_z%</white>")
    ));
    @Comment({"Here you can blacklist some terms (like swears, insults and unwanted urls)", "They will be replaced with a *", "You can use the regex syntax and the * wildcard"})
    public List<String> regex_blacklist = List.of(
            "discord.gg/.*",
            "(?i)shit",
            "(?i)sh!t",
            "(?i)niggers?",
            "(?i)fuck",
            "(?i)bicth",
            "(?i)bitch",
            "(?i)dick",
            "(?i)d1ck",
            "(?i)dik",
            "(?i)d1c",
            "(?i)ashole",
            "(?i)azzhole",
            "(?i)nigar",
            "(?i)niger",
            "(?i)c0ck",
            "(?i)kock",
            "(?i)fuck",
            "(?i)cunt",
            "(?i)dickhead",
            "(?i)asshole",
            "(?i)arsehole",
            "(?i)fuckhead",
            "(?i)faggots?",
            "(?i)kkk",
            "(?i)whores?",
            "(?i)sluts?",
            "(?i)cunts?",
            "(?i)dickheads?",
            "(?i)fucktard",
            "(?i)fucker",
            "(?i)pussy",
            "(?i)pussies",
            "(?i)cocks?",
            "(?i)dicks?",
            "(?i)twats?",
            "(?i)hump",
            "(?i)rednecks?",
            "(?i)chingchong",
            "(?i)anus",
            "(?i)bastard",
            "(?i)blowjob",
            "(?i)boner",
            "(?i)boobs?",
            "(?i)boobies",
            "(?i)dildo",
            "(?i)whore",
            "(?i)cum",
            "(?i)heil",
            "(?i)sex",
            "(?i)piss",
            "(?i)raped",
            "(?i)卐",
            "(?i)卍",
            "(?i)♿"
    );
    @Comment({"Whether to hide the bad word or to not send the message at all"})
    public boolean doNotSendCensoredMessage = false;
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
    @Comment("Rejoin delay in milliseconds")
    public int rejoinSendDelay = 500;
    @Comment("Quit delay in milliseconds")
    public int quitSendWaiting = 3000;
    @Comment({"Format id:volume:pitch",
            "You can find the list of sounds here: https://jd.papermc.io/paper/1.20/org/bukkit/Sound.html",
            "Leave it empty \"\" to disable the sound"})
    public String mentionSound = "ENTITY_EXPERIENCE_ORB_PICKUP:1:1";
    @Comment("Do not send public messages to players that are ignoring the sender")
    public boolean ignorePublicMessages = true;
    @Comment({"Send a warning message to the player when ignoring a player on public chat",
            "\"publicly_ignored_player\" notify will be sent to the player if this is set to true"})
    public boolean sendWarnWhenIgnoring = true;
    @Comment("Enable or disable the staff chat")
    public boolean enableStaffChat = true;
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
    @Comment("The timeout of the mail editor in seconds")
    public int mailEditorTimeout = 300;
    @Comment("Those commands will be disabled")
    public List<String> disabledCommands = List.of();
    @Comment("The [inv], [item] and [ec] placeholders will be considered as minimessage tags")
    public boolean interactiveChatNostalgia = true;
    @Comment("Command aliases (works for msg, mail, reply, staffchat and channel)")
    public Map<String, List<String>> commandAliases = new HashMap<>(Map.of(
            "msg", List.of("rmsg", "whisper", "msg", "pm", "w"),
            "rmail", List.of("mail", "mails"),
            "reply", List.of("r"),
            "channel", List.of("ch", "channels"),
            "staffchat", List.of("sc"),
            "rmutechat", List.of("mutechat", "mute"),
            "runmutechat", List.of("unmutechat", "unmute"),
            "rbroadcast", List.of("bc", "broadcast"),
            "rbroadcastraw", List.of("bcraw", "broadcastraw"),
            "announcer", List.of("announce")
    ));
    @Comment({"The priority of the listening event (LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR)",
            "adjust this if other plugins are interfering with RedisChat"})
    public String listeningPriority = "NORMAL";
    @Comment("Toggle debug mode (by default is false)")
    public boolean debug = false;
    @Comment("The number of threads for chat tasks")
    public int chatThreads = 2;
    @Comment({"botName is the botId associated to the bot inside the spicord configuration",
            "Every channel of RedisChat is linked with a channel on Discord",
            "The first element is a RedisChat channel, the second one is a Discord channel id",
            "You can find the Discord channel id by right clicking on the channel and clicking on 'Copy ID'"})
    public SpicordSettings spicord = new SpicordSettings(true, "<blue>[Discord]</blue> %role% %username% » %message%", "**%channel%** %sender% » %message%", Map.of("public", "1127207189547847740"));

    @Override
    public void validateConfig() {
        formats.forEach(format -> {
            if (!format.format().contains("%message%")) {
                Bukkit.getLogger().severe("Format " + format.permission() + " doesn't contain %message% placeholder");
            }
            if (!format.private_format().contains("%message%")) {
                Bukkit.getLogger().severe("Private format " + format.permission() + " doesn't contain %message% placeholder");
            }
            if (!format.receive_private_format().contains("%message%")) {
                Bukkit.getLogger().severe("Receive private format " + format.permission() + " doesn't contain %message% placeholder");
            }
            if (format.quit_format() == null) {
                Bukkit.getLogger().severe("Quit format " + format.permission() + " is empty. TO DISABLE IT, SET IT TO \"\"");
            }
            if (format.join_format() == null) {
                Bukkit.getLogger().severe("Join format " + format.permission() + " is empty. TO DISABLE IT, SET IT TO \"\"");
            }
            if (!dataMedium.equals(DataType.REDIS.keyName)) {
                Bukkit.getLogger().warning("You're not using REDIS as data medium, it is recommended to use it or you may not be able to use some features");
            }
        });
        if (!defaultFormat.format().contains("%message%")) {
            Bukkit.getLogger().warning("Default format doesn't contain %message% placeholder");
        }
        if (!defaultFormat.private_format().contains("%message%")) {
            Bukkit.getLogger().warning("Default private format doesn't contain %message% placeholder");
        }
        if (!defaultFormat.receive_private_format().contains("%message%")) {
            Bukkit.getLogger().warning("Default receive private format doesn't contain %message% placeholder");
        }
        if (!defaultFormat.mention_format().contains("%player%")) {
            Bukkit.getLogger().warning("Default mention format doesn't contain %player% placeholder");
        }
        if (!commandAliases.containsKey("mutechat")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("mutechat", List.of("mute"));
            Bukkit.getLogger().warning("You didn't set any aliases for mutechat, using default aliases");
        }
        if (!commandAliases.containsKey("unmutechat")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("unmutechat", List.of("unmute"));
            Bukkit.getLogger().warning("You didn't set any aliases for unmutechat, using default aliases");
        }
        if (!commandAliases.containsKey("rbroadcast")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("rbroadcast", List.of("broadcast", "bc"));
            Bukkit.getLogger().warning("You didn't set any aliases for rbroadcast, using default aliases");
        }
        if (!commandAliases.containsKey("rbroadcastraw")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("rbroadcastraw", List.of("broadcastraw", "bcraw"));
            Bukkit.getLogger().warning("You didn't set any aliases for rbroadcastraw, using default aliases");
        }
        for (Announcement announcement : announcer) {
            if (announcement.channelName == null || announcement.channelName.isEmpty()) {
                Bukkit.getLogger().warning("Announce " + announcement.announcementName() + " doesn't have a channel name, using \"public\" as default");
            }
        }
    }

    public record RedisSettings(String host, int port, String user, String password,
                                int database, int timeout,
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


    public record Announcement(
            String announcementName,
            String message,
            String channelName,
            int delay,
            int interval) {
    }

    public record SpicordSettings(
            boolean enabled,
            String chatFormat,
            String discordFormat,
            Map<String, String> spicordChannelLink
    ) {
    }

    public @NotNull ChatFormat getChatFormat(@Nullable CommandSender p) {
        if (p == null) return defaultFormat;
        return formats.stream()
                .filter(format -> p.hasPermission(format.permission()))
                .findFirst()
                .orElse(defaultFormat);
    }

    public String[] getCommandAliases(String command) {
        return commandAliases.getOrDefault(command, List.of()).toArray(new String[0]);
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