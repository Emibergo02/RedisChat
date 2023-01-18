package dev.unnm3d.redischat;

import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redischat.chat.ChatListener;
import dev.unnm3d.redischat.commands.*;
import dev.unnm3d.redischat.invshare.InvGUI;
import dev.unnm3d.redischat.invshare.InvShare;
import dev.unnm3d.redischat.redis.RedisDataManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.ThreadFactoryProvider;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;

public final class RedisChat extends JavaPlugin {

    private static RedisChat instance;
    public static Config config;
    private ChatListener chatListener;
    private RedisDataManager redisDataManager;
    private PlayerListManager playerListManager;

    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        this.getCommand("msg").setExecutor(new MsgCommand());
        this.getCommand("ignore").setExecutor(new IgnoreCommand());
        this.getCommand("spychat").setExecutor(new SpyChatCommand());
        this.getCommand("reply").setExecutor(new ReplyCommand());
        this.getCommand("broadcast").setExecutor(new BroadcastCommand());
        this.getCommand("clearchat").setExecutor(new ClearChatCommand());
        this.playerListManager = new PlayerListManager(this);
        this.getCommand("msg").setTabCompleter(this.playerListManager);
        this.getCommand("ignore").setTabCompleter(this.playerListManager);


        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(this.chatListener, this);
        this.redisDataManager = new RedisDataManager(RedisClient.create(config.redis.redisUri()), this);
        getLogger().info("Redis URI: " + config.redis.redisUri());
        this.redisDataManager.listenChatPackets();
        Bukkit.getOnlinePlayers().forEach(player -> this.redisDataManager.addPlayerName(player.getName()));


        //InvShare part
        getServer().getPluginManager().registerEvents(new InvGUI.GuiListener(), this);
        getCommand("invshare").setExecutor(new InvShare(this));
        getCommand("redischat").setExecutor((sender, command, label, args) -> {
            if (args.length == 1) {
                if (sender.hasPermission(Permission.REDIS_CHAT_ADMIN.getPermission()))
                    if (args[0].equalsIgnoreCase("reload")) {
                        loadYML();
                        RedisChat.config.sendMessage(sender, "<green>Config reloaded");
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

        config = YamlConfigurations.update(
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
