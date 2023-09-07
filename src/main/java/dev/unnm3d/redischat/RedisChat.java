package dev.unnm3d.redischat;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.channels.ChannelCommand;
import dev.unnm3d.redischat.channels.ChannelManager;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.chat.JoinQuitManager;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.configs.Config;
import dev.unnm3d.redischat.configs.GuiSettings;
import dev.unnm3d.redischat.configs.Messages;
import dev.unnm3d.redischat.datamanagers.RedisDataManager;
import dev.unnm3d.redischat.datamanagers.sqlmanagers.H2SQLDataManager;
import dev.unnm3d.redischat.datamanagers.sqlmanagers.MySQLDataManager;
import dev.unnm3d.redischat.integrations.OraxenTagResolver;
import dev.unnm3d.redischat.mail.MailCommand;
import dev.unnm3d.redischat.mail.MailManager;
import dev.unnm3d.redischat.moderation.SpyChatCommand;
import dev.unnm3d.redischat.moderation.SpyManager;
import dev.unnm3d.redischat.moderation.StaffChatCommand;
import dev.unnm3d.redischat.permission.LuckPermsProvider;
import dev.unnm3d.redischat.permission.PermissionProvider;
import dev.unnm3d.redischat.permission.VaultPermissionProvider;
import dev.unnm3d.redischat.task.AnnounceManager;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RedisChat extends JavaPlugin {

    private static RedisChat instance;
    public Config config;
    private List<String> registeredCommands;
    public Messages messages;
    public GuiSettings guiSettings;
    private ChatListener chatListener;
    @Getter
    private DataManager dataManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private ChannelManager channelManager;
    @Getter
    private AnnounceManager announceManager;
    @Getter
    private SpyManager spyManager;
    @Getter
    private ComponentProvider componentProvider;
    @Getter
    private AdventureWebuiEditorAPI webEditorAPI;
    @Getter
    private JoinQuitManager joinQuitManager;
    @Getter
    private PermissionProvider permissionProvider;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        registeredCommands = new ArrayList<>();
        instance = this;
        loadYML();

        //Redis section
        switch (config.getDataMedium()) {
            case REDIS -> this.dataManager = RedisDataManager.startup(this);
            case MYSQL -> this.dataManager = new MySQLDataManager(this);
            case H2 -> this.dataManager = new H2SQLDataManager(this);
        }

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

        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(this.chatListener, this);

        loadCommandAPICommand(new StaffChatCommand(this).getCommand());

        this.channelManager = new ChannelManager(this);
        loadCommandAPICommand(new ChannelCommand(this).getCommand());

        if (config.enableQuitJoinMessages) {
            this.joinQuitManager = new JoinQuitManager(this);
            getServer().getPluginManager().registerEvents(this.joinQuitManager, this);
        }


        //Mail section
        if (config.enableMails) {
            loadCommandAPICommand(new MailCommand(new MailManager(this)).getCommand());
        }


        //Announce feature
        this.announceManager = new AnnounceManager(this);
        AnnounceCommand announceCommand = new AnnounceCommand(this, this.announceManager);
        loadCommand("announce", announceCommand, announceCommand);


        this.playerListManager = new PlayerListManager(this);
        this.webEditorAPI = new AdventureWebuiEditorAPI(config.webEditorUrl);

        //Commands section
        //New command API
        loadCommandAPICommand(new MainCommand(this, this.webEditorAPI).getCommand());
        loadCommandAPICommand(new MsgCommand(this).getCommand());
        loadCommandAPICommand(new ReplyCommand(this).getCommand());

        //Old command API
        SetItemCommand setItemCommand = new SetItemCommand(this);
        loadCommand("redischat-setitem", setItemCommand, setItemCommand);

        this.spyManager = new SpyManager(this);
        loadCommand("spychat", new SpyChatCommand(this), null);
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        loadCommand("ignore", ignoreCommand, ignoreCommand);
        loadCommand("broadcast", new BroadcastCommand(this), null);
        loadCommand("clearchat", new ClearChatCommand(this), null);


        //InvShare part
        loadCommand("invshare", new InvShareCommand(this), null);

        new Metrics(this, 17678);

        //Integration section
        if (getServer().getPluginManager().getPlugin("Oraxen") != null) {
            getLogger().info("Oraxen found, enabling integration");
            componentProvider.addResolverIntegration(new OraxenTagResolver());
        }
    }


    public void loadYML() {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.config = YamlConfigurations.update(
                configFile,
                Config.class,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat config")
                        .footer("Authors: Unnm3d")
                        .build()
        );

        Path messagesFile = new File(getDataFolder(), "messages.yml").toPath();
        this.messages = YamlConfigurations.update(
                messagesFile,
                Messages.class,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat messages")
                        .footer("Authors: Unnm3d")
                        .build()
        );
        Path guiSettingsFile = new File(getDataFolder(), "guis.yml").toPath();
        this.guiSettings = YamlConfigurations.update(
                guiSettingsFile,
                GuiSettings.class,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                        .header("RedisChat guis")
                        .footer("Authors: Unnm3d")
                        .build()
        );

    }

    public void saveMessages() {
        YamlConfigurations.save(new File(this.getDataFolder(), "messages.yml").toPath(), Messages.class, messages);
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
        registeredCommands.forEach(CommandAPI::unregister);
        CommandAPI.onDisable();

        if (this.playerListManager != null)
            this.playerListManager.stop();
        if (this.dataManager != null)
            this.dataManager.close();
        if (this.announceManager != null)
            this.announceManager.cancelAll();
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
        registeredCommands.add(commandAPICommand.getName());
        getLogger().info("Command " + commandAPICommand.getName() + " registered on CommandAPI!");
    }

    public static RedisChat getInstance() {
        return instance;
    }


    public ChatListener getChatListener() {
        return chatListener;
    }
}
