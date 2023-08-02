package dev.unnm3d.redischat;


import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.configs.Config;

import dev.unnm3d.redischat.configs.Messages;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.datamanagers.LegacyDataManager;
import dev.unnm3d.redischat.datamanagers.RedisDataManager;
import dev.unnm3d.redischat.integrations.OraxenTagResolver;
import dev.unnm3d.redischat.api.VanishIntegration;
import dev.unnm3d.redischat.moderation.SpyChatCommand;
import dev.unnm3d.redischat.moderation.SpyManager;
import dev.unnm3d.redischat.moderation.StaffChat;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import dev.unnm3d.redischat.utils.Metrics;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
        if (config.getDataMedium() == Config.DataType.REDIS) {
            RedisURI.Builder redisURIBuilder = RedisURI.builder()
                    .withHost(config.redis.host())
                    .withPort(config.redis.port())
                    .withDatabase(config.redis.database())
                    .withTimeout(Duration.of(config.redis.timeout(), TimeUnit.MILLISECONDS.toChronoUnit()))
                    .withClientName(config.redis.clientName());
            if (config.redis.user().equals("changecredentials"))
                getLogger().warning("You are using default redis credentials. Please change them in the config.yml file!");
            //Authentication params
            redisURIBuilder = config.redis.password().equals("") ?
                    redisURIBuilder :
                    config.redis.user().equals("") ?
                            redisURIBuilder.withPassword(config.redis.password().toCharArray()) :
                            redisURIBuilder.withAuthentication(config.redis.user(), config.redis.password());

            this.dataManager = new RedisDataManager(RedisClient.create(redisURIBuilder.build()), this);
        } else if (config.getDataMedium() == Config.DataType.MYSQL) {
            this.dataManager = new LegacyDataManager(this);
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

    }

    public void saveMessages() {
        YamlConfigurations.save(new File(this.getDataFolder(), "messages.yml").toPath(), Messages.class, messages);
    }


    @Override
    public void onDisable() {
        getLogger().warning("RedisChat is disabling...");
        this.playerListManager.stop();
        this.dataManager.close();
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
