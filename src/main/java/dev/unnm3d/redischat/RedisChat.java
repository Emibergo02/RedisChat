package dev.unnm3d.redischat;


import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.api.VanishIntegration;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.configs.Config;
import dev.unnm3d.redischat.configs.Messages;
import dev.unnm3d.redischat.datamanagers.RedisDataManager;
import dev.unnm3d.redischat.datamanagers.sqlmanagers.H2SQLDataManager;
import dev.unnm3d.redischat.datamanagers.sqlmanagers.MySQLDataManager;
import dev.unnm3d.redischat.integrations.OraxenTagResolver;
import dev.unnm3d.redischat.moderation.StaffChat;
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

public final class RedisChat extends JavaPlugin {

    private static RedisChat instance;
    public Config config;
    public Messages messages;
    private ChatListener chatListener;
    @Getter
    private DataManager dataManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private ComponentProvider componentProvider;
    @Getter
    private AdventureWebuiEditorAPI webEditorAPI;

    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        //Redis section
        switch (config.getDataMedium()) {
            case REDIS -> this.dataManager = RedisDataManager.startup(this);
            case MYSQL -> this.dataManager = new MySQLDataManager(this);
            case H2 -> this.dataManager = new H2SQLDataManager(this);
        }

        //Chat section
        this.componentProvider = new ComponentProvider(this);
        StaffChat staffChat = new StaffChat(this);
        this.chatListener = new ChatListener(this, staffChat);
        getServer().getPluginManager().registerEvents(this.chatListener, this);


        //Commands section
        this.playerListManager = new PlayerListManager(this);

        this.webEditorAPI = new AdventureWebuiEditorAPI(config.webEditorUrl);

        MainCommand mainCommand = new MainCommand(this, this.webEditorAPI);
        loadCommand("redischat", mainCommand, mainCommand);
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

    }

    public void saveMessages() {
        YamlConfigurations.save(new File(this.getDataFolder(), "messages.yml").toPath(), Messages.class, messages);
    }


    @Override
    public void onDisable() {
        getLogger().warning("RedisChat is disabling...");
        if (this.playerListManager != null)
            this.playerListManager.stop();
        if (this.dataManager != null)
            this.dataManager.close();
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

    public static RedisChat getInstance() {
        return instance;
    }


    public ChatListener getChatListener() {
        return chatListener;
    }
}
