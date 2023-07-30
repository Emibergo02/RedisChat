package dev.unnm3d.redischat;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.configs.Config;
import dev.unnm3d.redischat.configs.GuiSettings;
import dev.unnm3d.redischat.configs.Messages;
import dev.unnm3d.redischat.integrations.OraxenTagResolver;
import dev.unnm3d.redischat.integrations.VanishIntegration;
import dev.unnm3d.redischat.mail.MailCommand;
import dev.unnm3d.redischat.mail.MailManager;
import dev.unnm3d.redischat.moderation.SpyChatCommand;
import dev.unnm3d.redischat.moderation.SpyManager;
import dev.unnm3d.redischat.moderation.StaffChat;
import dev.unnm3d.redischat.redis.DataManager;
import dev.unnm3d.redischat.redis.RedisDataManager;
import dev.unnm3d.redischat.task.AnnounceManager;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import dev.unnm3d.redischat.utils.Metrics;
import io.lettuce.core.RedisClient;
import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class RedisChat extends JavaPlugin {

    private static RedisChat instance;
    public Config config;
    public Messages messages;
    public GuiSettings guiSettings;
    private ChatListener chatListener;
    @Getter
    private DataManager redisDataManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private AnnounceManager announceManager;
    @Getter
    private SpyManager spyManager;
    @Getter
    private ComponentProvider componentProvider;
    @Getter
    private AdventureWebuiEditorAPI webEditorAPI;

    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        //Redis section
        this.redisDataManager = new RedisDataManager(RedisClient.create(config.redis.redisUri()), this);
        getLogger().info("Redis URI: " + config.redis.redisUri());

        //Chat section
        this.componentProvider = new ComponentProvider(this);
        StaffChat staffChat = new StaffChat(this);
        this.chatListener = new ChatListener(this, staffChat);
        getServer().getPluginManager().registerEvents(this.chatListener, this);

        //Mail section
        if (config.enableMails) {
            MailCommand mailCommand = new MailCommand(new MailManager(this));
            loadCommand("rmail", mailCommand, mailCommand);
        }


        //Announce feature
        this.announceManager = new AnnounceManager(this);
        AnnounceCommand announceCommand = new AnnounceCommand(this, this.announceManager);
        loadCommand("announce", announceCommand, announceCommand);

        //Commands section
        this.playerListManager = new PlayerListManager(this);

        this.webEditorAPI = new AdventureWebuiEditorAPI(config.webEditorUrl);

        MainCommand mainCommand = new MainCommand(this, this.webEditorAPI);
        loadCommand("redischat", mainCommand, mainCommand);
        SetItemCommand setItemCommand = new SetItemCommand(this);
        loadCommand("redischat-setitem", setItemCommand, setItemCommand);

        this.spyManager = new SpyManager(this);
        loadCommand("spychat", new SpyChatCommand(this), null);
        MsgCommand msgCommand = new MsgCommand(this);
        loadCommand("msg", msgCommand, msgCommand);
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        loadCommand("ignore", ignoreCommand, ignoreCommand);
        loadCommand("reply", new ReplyCommand(this), null);
        loadCommand("broadcast", new BroadcastCommand(this), null);
        loadCommand("clearchat", new ClearChatCommand(this), null);
        loadCommand("staffchat", staffChat, null);


        //InvShare part
        loadCommand("invshare", new InvShareCommand(this), null);

        new Metrics(this, 17678);

        //Integration section
        if (getServer().getPluginManager().getPlugin("Oraxen") != null) {
            getLogger().info("Oraxen found, enabling integration");
            componentProvider.addResolverIntegration(new OraxenTagResolver());
        }
        playerListManager.addVanishIntegration(new VanishIntegration() {
        }); //PremiumVanish standard
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
        this.playerListManager.stop();
        this.redisDataManager.close();
        this.announceManager.cancelAll();
    }

    private void loadCommand(@NotNull String cmdName, @NotNull CommandExecutor executor, @Nullable TabCompleter tabCompleter) {
        PluginCommand cmd = getServer().getPluginCommand(cmdName);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(tabCompleter);
        } else {
            getLogger().warning("Command " + cmdName + " not found!");
        }
    }

    public static RedisChat getInstance() {
        return instance;
    }


    public ChatListener getChatListener() {
        return chatListener;
    }
}
