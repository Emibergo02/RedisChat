package dev.unnm3d.redischat;

import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.invshare.InvGUI;
import dev.unnm3d.redischat.invshare.InvShare;
import dev.unnm3d.redischat.moderation.SpyChatCommand;
import dev.unnm3d.redischat.moderation.SpyManager;
import dev.unnm3d.redischat.redis.RedisDataManager;
import dev.unnm3d.redischat.task.AnnounceManager;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import dev.unnm3d.redischat.utils.Metrics;
import io.lettuce.core.RedisClient;
import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class RedisChat extends JavaPlugin {

    private static RedisChat instance;
    public Config config;
    private ChatListener chatListener;
    @Getter
    private RedisDataManager redisDataManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private AnnounceManager announceManager;
    @Getter
    private SpyManager spyManager;
    @Getter
    private ComponentProvider componentProvider;

    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        //Redis section
        this.redisDataManager = new RedisDataManager(RedisClient.create(config.redis.redisUri()), this);
        getLogger().info("Redis URI: " + config.redis.redisUri());
        this.redisDataManager.listenChatPackets();
        this.getServer().getOnlinePlayers().forEach(player -> this.redisDataManager.addPlayerName(player.getName()));

        //Chat section
        this.componentProvider = new ComponentProvider(this);
        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(this.chatListener, this);

        //Announce feature
        this.announceManager = new AnnounceManager(this);
        AnnounceCommand announceCommand = new AnnounceCommand(this, this.announceManager);
        loadCommand("announce", announceCommand, announceCommand);

        //Commands section
        this.playerListManager = new PlayerListManager(this);
        MainCommand mainCommand = new MainCommand(this, new AdventureWebuiEditorAPI());
        loadCommand("redischat", mainCommand, mainCommand);
        this.spyManager = new SpyManager(this);
        loadCommand("spychat", new SpyChatCommand(this), null);
        loadCommand("msg", new MsgCommand(this), this.playerListManager);
        loadCommand("ignore", new IgnoreCommand(this), this.playerListManager);
        loadCommand("reply", new ReplyCommand(this), null);
        loadCommand("broadcast", new BroadcastCommand(this), null);
        loadCommand("clearchat", new ClearChatCommand(this), null);

        //InvShare part
        getServer().getPluginManager().registerEvents(new InvGUI.GuiListener(), this);
        loadCommand("invshare", new InvShare(this), null);

        new Metrics(this, 17678);
    }


    public void loadYML() {
        YamlConfigurationProperties properties = YamlConfigurationProperties.newBuilder()
                .header(
                        """
                                RedisChat config
                                """
                )
                .footer("Authors: Unnm3d")
                .build();

        Path configFile = new File(getDataFolder(), "config.yml").toPath();

        this.config = YamlConfigurations.update(
                configFile,
                Config.class,
                properties
        );
    }

    public void saveYML() {
        YamlConfigurations.save(new File(this.getDataFolder(), "config.yml").toPath(), Config.class, config);
    }


    @Override
    public void onDisable() {
        getLogger().warning("RedisChat is disabling...");
        this.playerListManager.getTask().cancel();
        this.redisDataManager.removePlayerNames(getServer().getOnlinePlayers().stream().map(HumanEntity::getName).toArray(String[]::new));
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
