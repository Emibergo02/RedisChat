package dev.unnm3d.redischat;

import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.invshare.InvGUI;
import dev.unnm3d.redischat.invshare.InvShare;
import dev.unnm3d.redischat.redis.RedisDataManager;
import dev.unnm3d.redischat.task.AnnounceManager;
import io.lettuce.core.RedisClient;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class RedisChat extends JavaPlugin {

    private static RedisChat instance;
    public Config config;
    private ChatListener chatListener;
    private RedisDataManager redisDataManager;
    private PlayerListManager playerListManager;
    private AnnounceManager announceManager;
    @Getter
    private ComponentProvider componentProvider;

    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        this.getCommand("msg").setExecutor(new MsgCommand(this));
        this.getCommand("ignore").setExecutor(new IgnoreCommand(this));
        this.getCommand("spychat").setExecutor(new SpyChatCommand(this));
        this.getCommand("reply").setExecutor(new ReplyCommand(this));
        this.getCommand("broadcast").setExecutor(new BroadcastCommand(this));
        this.getCommand("clearchat").setExecutor(new ClearChatCommand(this));
        this.playerListManager = new PlayerListManager(this);
        this.getCommand("msg").setTabCompleter(this.playerListManager);
        this.getCommand("ignore").setTabCompleter(this.playerListManager);

        this.componentProvider = new ComponentProvider(this);
        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(this.chatListener, this);
        this.redisDataManager = new RedisDataManager(RedisClient.create(config.redis.redisUri()), this);
        getLogger().info("Redis URI: " + config.redis.redisUri());
        this.redisDataManager.listenChatPackets();
        Bukkit.getOnlinePlayers().forEach(player -> this.redisDataManager.addPlayerName(player.getName()));

        this.announceManager = new AnnounceManager(this);
        AnnounceCommand announceCommand = new AnnounceCommand(this, this.announceManager);
        this.getCommand("announce").setExecutor(announceCommand);
        this.getCommand("announce").setTabCompleter(announceCommand);

        //InvShare part
        getServer().getPluginManager().registerEvents(new InvGUI.GuiListener(), this);
        getCommand("invshare").setExecutor(new InvShare(this));
        getCommand("redischat").setExecutor((sender, command, label, args) -> {
            if (args.length == 1) {
                if (sender.hasPermission(Permission.REDIS_CHAT_ADMIN.getPermission()))
                    if (args[0].equalsIgnoreCase("reload")) {
                        loadYML();
                        announceManager.reload();
                        config.sendMessage(sender, "<green>Config reloaded");
                        return true;
                    }
                return true;
            }

            return false;
        });

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

    public net.milkbowl.vault.permission.Permission getPermissionProvider() {
        @Nullable RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (rsp == null) return null;
        return rsp.getProvider();
    }

    @Override
    public void onDisable() {
        getLogger().warning("RedisChat is disabling...");
        this.playerListManager.getTask().cancel();
        this.redisDataManager.removePlayerNames(getServer().getOnlinePlayers().stream().map(HumanEntity::getName).toArray(String[]::new));
        this.redisDataManager.close();
        this.announceManager.cancelAll();
    }

    public static RedisChat getInstance() {
        return instance;
    }


    public RedisDataManager getRedisDataManager() {
        return redisDataManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}
