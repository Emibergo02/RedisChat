package dev.unnm3d.redischat;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.ConfigurationException;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.channels.ChannelCommand;
import dev.unnm3d.redischat.channels.ChannelManager;
import dev.unnm3d.redischat.chat.*;
import dev.unnm3d.redischat.chat.listeners.ChatListenerWithPriority;
import dev.unnm3d.redischat.chat.listeners.JoinQuitManager;
import dev.unnm3d.redischat.chat.listeners.UtilsListener;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.datamanagers.RedisDataManager;
import dev.unnm3d.redischat.datamanagers.sqlmanagers.MySQLDataManager;
import dev.unnm3d.redischat.datamanagers.sqlmanagers.SQLiteDataManager;
import dev.unnm3d.redischat.discord.DiscordWebhook;
import dev.unnm3d.redischat.discord.IDiscordHook;
import dev.unnm3d.redischat.discord.SpicordHook;
import dev.unnm3d.redischat.integrations.OraxenTagResolver;
import dev.unnm3d.redischat.integrations.PremiumVanishIntegration;
import dev.unnm3d.redischat.mail.MailCommand;
import dev.unnm3d.redischat.mail.MailGUIManager;
import dev.unnm3d.redischat.moderation.MuteCommand;
import dev.unnm3d.redischat.moderation.SpyChatCommand;
import dev.unnm3d.redischat.moderation.SpyManager;
import dev.unnm3d.redischat.moderation.StaffChatCommand;
import dev.unnm3d.redischat.permission.LuckPermsProvider;
import dev.unnm3d.redischat.permission.PermissionProvider;
import dev.unnm3d.redischat.permission.VaultPermissionProvider;
import dev.unnm3d.redischat.settings.Config;
import dev.unnm3d.redischat.settings.FiltersConfig;
import dev.unnm3d.redischat.settings.GuiSettings;
import dev.unnm3d.redischat.settings.Messages;
import dev.unnm3d.redischat.task.AnnouncerManager;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import dev.unnm3d.redischat.utils.Metrics;
import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RedisChat extends JavaPlugin {

    @Getter
    private static RedisChat instance;
    @Getter
    private static TaskScheduler scheduler;
    private List<CommandAPICommand> registeredCommands;
    public GuiSettings guiSettings;
    @Getter
    private DataManager dataManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private ChannelManager channelManager;
    @Getter
    private AnnouncerManager announcerManager;
    @Getter
    private SpyManager spyManager;
    @Getter
    private PlaceholderManager placeholderManager;
    @Getter
    private ComponentProvider componentProvider;
    @Getter
    private AdventureWebuiEditorAPI webEditorAPI;
    @Getter
    private JoinQuitManager joinQuitManager;
    @Getter
    private PermissionProvider permissionProvider;
    @Getter
    private IDiscordHook discordHook;
    @Getter
    private ExecutorService executorService;
    @Getter
    private MailGUIManager mailGUIManager;

    public Config config;
    public FiltersConfig filterSettings;
    public Messages messages;


    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .usePluginNamespace()
                .silentLogs(true)
                .shouldHookPaperReload(true)
                .verboseOutput(false));
    }


    @Override
    public void onEnable() {
        instance = this;
        CommandAPI.onEnable();
        registeredCommands = new ArrayList<>();
        scheduler = UniversalScheduler.getScheduler(this);

        try {
            loadYML();
        } catch (ConfigurationException e) {
            getLogger().severe("config.yml or messages.yml or guis.yml is invalid! Please regenerate them (starting from config.yml: " + e.getMessage());
        }

        this.executorService = Executors.newFixedThreadPool(config.chatThreads);

        //Redis section
        this.dataManager = switch (config.getDataType()) {
            case REDIS -> RedisDataManager.startup(this);
            case MYSQL -> new MySQLDataManager(this);
            case SQLITE -> new SQLiteDataManager(this);
        };

        //Permission section
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("LuckPerms found, enabling integration");
            this.permissionProvider = new LuckPermsProvider();
        } else if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("Vault found, enabling integration");
            this.permissionProvider = new VaultPermissionProvider();
        } else {
            this.permissionProvider = new PermissionProvider() {
            };
        }

        //Chat section
        this.componentProvider = new ComponentProvider(this);

        ChatListenerWithPriority listenerWithPriority;
        try {
            listenerWithPriority = ChatListenerWithPriority.valueOf(config.listeningPriority);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid listening priority, using NORMAL");
            listenerWithPriority = ChatListenerWithPriority.NORMAL;
        }
        getServer().getPluginManager().registerEvents(listenerWithPriority.getListener(), this);

        if (config.enableStaffChat)
            loadCommandAPICommand(new StaffChatCommand(this).getCommand());


        this.channelManager = new ChannelManager(this);
        loadCommandAPICommand(new ChannelCommand(this).getCommand());

        if (config.enableQuitJoinMessages) {
            if (config.getDataType() == Config.DataType.REDIS) {
                this.joinQuitManager = new JoinQuitManager(this);
                getServer().getPluginManager().registerEvents(this.joinQuitManager, this);
            } else {
                getLogger().warning("Join/Quit messages are not supported with SQLite or MySQL");
            }
        }

        //Update active channel on join and spy listener
        getServer().getPluginManager().registerEvents(new UtilsListener(this), this);

        //Mail section
        if (config.enableMails) {
            this.mailGUIManager = new MailGUIManager(this);
            loadCommandAPICommand(new MailCommand(this.mailGUIManager).getCommand());
        }


        //Announce feature
        this.announcerManager = new AnnouncerManager(this);
        loadCommandAPICommand(new AnnounceCommand(this, this.announcerManager).getCommand());


        this.playerListManager = new PlayerListManager(this);
        this.webEditorAPI = new AdventureWebuiEditorAPI(config.webEditorUrl);

        //Commands section
        loadCommandAPICommand(new MainCommand(this, this.webEditorAPI).getCommand());
        loadCommandAPICommand(new MsgCommand(this).getCommand());
        loadCommandAPICommand(new ReplyCommand(this).getCommand());
        loadCommandAPICommand(new ChatAsCommand(this).getCommand());
        final BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        loadCommandAPICommand(broadcastCommand.getBroadcastCommand());
        loadCommandAPICommand(broadcastCommand.getBroadcastRawCommand());
        if (config.dataMedium.equals(Config.DataType.REDIS.toString())) {
            loadCommandAPICommand(new MuteCommand(this).getMuteCommand());
            loadCommandAPICommand(new MuteCommand(this).getUnMuteCommand());
        } else {
            getLogger().warning("Mute command is currently not supported with SQLite or MySQL");
            getLogger().warning("UnMute command is currently not supported with SQLite or MySQL");
        }

        //Old command API
        SetItemCommand setItemCommand = new SetItemCommand(this);
        loadCommand("redischat-setitem", setItemCommand, setItemCommand);

        this.spyManager = new SpyManager(this);
        loadCommand("spychat", new SpyChatCommand(this), null);
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        loadCommand("ignore", ignoreCommand, ignoreCommand);
        loadCommandAPICommand(new IgnoreWhitelistCommand(this).getCommand());
        loadCommand("clearchat", new ClearChatCommand(this), null);

        //RedisChat Placeholders
        this.placeholderManager = new PlaceholderManager(this);
        final ChatColorCommand chatColorCommand = new ChatColorCommand(this);
        loadCommand("chatcolor", chatColorCommand, chatColorCommand);
        final SetPlaceholderCommand placeholderCommand = new SetPlaceholderCommand(this);
        loadCommand("setchatplaceholder", placeholderCommand, placeholderCommand);

        //InvShare part
        loadCommand("invshare", new InvShareCommand(this), null);

        new Metrics(this, 17678);

        //Integration section
        if (getServer().getPluginManager().getPlugin("Oraxen") != null) {
            getLogger().info("Oraxen found, enabling integration");
            componentProvider.addResolverIntegration(new OraxenTagResolver());
        }
        if (getServer().getPluginManager().getPlugin("PremiumVanish") != null) {
            getLogger().info("PremiumVanish found, enabling integration");
            playerListManager.addVanishIntegration(new PremiumVanishIntegration(this));
        }
        if (getServer().getPluginManager().getPlugin("Spicord") != null && config.spicord.enabled()) {
            getLogger().info("Spicord found, enabling integration");
            this.discordHook = new SpicordHook(this);
        } else {
            getLogger().info("Spicord not found, using default DiscordWebhook");
            this.discordHook = new DiscordWebhook(this);
        }
        //PlaceholderAPI is always enabled as it is a dependency
        new RedisChatPAPI(this).register();

        new UpdateCheck(this).getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                getLogger().info("§k*******§r §6New version available: " + version + " §k*******");
                getLogger().info("Check it on https://www.spigotmc.org/resources/redischat%E2%9A%A1simple-intuitive-chat-suite%E2%9A%A1cross-server-support.111015/");
            }
        });
    }


    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.config = YamlConfigurations.update(
                configFile,
                Config.class,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat config")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
        if (this.config.validateConfig())
            YamlConfigurations.save(configFile, Config.class, this.config);

        Path filtersFile = new File(getDataFolder(), "filters.yml").toPath();
        this.filterSettings = YamlConfigurations.update(
                filtersFile,
                FiltersConfig.class,
                YamlConfigurationProperties.newBuilder()
                        .header("""
                                How to configure filters:
                                    enabled: true/false  # If the filter is enabled at all
                                    priority: 1  # The priority of the filter
                                    audienceWhitelist:  # The audience type of the filter (who is the target of the filter)
                                      - DISCORD
                                      - PLAYER   #Private messages
                                      - CHANNEL  #Channel messages
                                    channelWhitelist: []  # Which channels are affected by the filter, leave empty for all channels
                                
                                """)
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );

        Path messagesFile = new File(getDataFolder(), "messages.yml").toPath();
        this.messages = YamlConfigurations.update(
                messagesFile,
                Messages.class,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat messages")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );

        Path guiSettingsFile = new File(getDataFolder(), "guis.yml").toPath();
        this.guiSettings = YamlConfigurations.update(
                guiSettingsFile,
                GuiSettings.class,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                        .header("RedisChat guis")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
        if (this.guiSettings.validateConfig())
            YamlConfigurations.save(guiSettingsFile, GuiSettings.class, this.guiSettings);
    }

    public void saveMessages() {
        YamlConfigurations.save(
                new File(this.getDataFolder(), "messages.yml").toPath(),
                Messages.class,
                messages,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat messages")
                        .footer("Authors: Unnm3d")
                        .build());
    }

    public void saveGuiSettings() {
        YamlConfigurations.save(
                new File(this.getDataFolder(), "guis.yml").toPath(),
                GuiSettings.class,
                guiSettings,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                        .header("RedisChat guis")
                        .footer("Authors: Unnm3d")
                        .build());
    }

    @Override
    public void onDisable() {
        getLogger().warning("RedisChat is disabling...");
        if (this.dataManager != null)
            this.dataManager.clearInvShareCache();

        registeredCommands.forEach(command -> CommandAPI.unregister(command.getName(), true));
        CommandAPI.onDisable();

        if (this.playerListManager != null)
            this.playerListManager.stop();
        if (this.dataManager != null)
            this.dataManager.close();
        if (this.announcerManager != null)
            this.announcerManager.cancelAll();
    }

    private void loadCommand(@NotNull String cmdName, @NotNull CommandExecutor executor, @Nullable TabCompleter tabCompleter) {
        if (config.disabledCommands.contains(cmdName)) {
            getLogger().warning("Command " + cmdName + " is disabled in the config.yml file!");
            return;
        }
        PluginCommand cmd = getServer().getPluginCommand(cmdName);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(tabCompleter);
        } else {
            getLogger().warning("Command " + cmdName + " not found!");
        }
    }

    private void loadCommandAPICommand(CommandAPICommand commandAPICommand) {

        if (config.disabledCommands.contains(commandAPICommand.getName())) {
            getLogger().warning("Command " + commandAPICommand.getName() + " is disabled in the config.yml file!");
            return;
        }

        CommandAPI.unregister(commandAPICommand.getName(), true);
        commandAPICommand.register();
        registeredCommands.add(commandAPICommand);
        getLogger().info("Command " + commandAPICommand.getName() + " registered on CommandAPI!");
    }

}
