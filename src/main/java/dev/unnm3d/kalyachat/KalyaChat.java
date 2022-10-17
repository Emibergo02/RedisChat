package dev.unnm3d.kalyachat;

import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.ezredislib.EzRedisMessenger;
import dev.unnm3d.kalyachat.chat.ChatListener;
import dev.unnm3d.kalyachat.commands.*;
import dev.unnm3d.kalyachat.invshare.InvCache;
import dev.unnm3d.kalyachat.invshare.InvGUI;
import dev.unnm3d.kalyachat.invshare.InvShare;
import dev.unnm3d.kalyachat.redis.RedisDataManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class KalyaChat extends JavaPlugin {

    private static KalyaChat instance;
    private EzRedisMessenger ezRedisMessenger;
    public static Config config;
    private ChatListener chatListener;
    private RedisDataManager redisDataManager;

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
        this.getCommand("msg").setTabCompleter(new PlayerListManager());


        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(this.chatListener, this);
        try {
            this.ezRedisMessenger = new EzRedisMessenger(config.redis.host(), config.redis.port(), config.redis.user(), config.redis.password(),config.redis.timeout(),config.redis.database(),"kalyachat");
            this.redisDataManager=new RedisDataManager(this.ezRedisMessenger);
            this.redisDataManager.listenChatPackets();
            Bukkit.getOnlinePlayers().forEach(player -> this.redisDataManager.addPlayerName(player.getName()));


        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        //InvShare part
        getServer().getPluginManager().registerEvents(new InvGUI.GuiListener(), this);
        getCommand("invshare").setExecutor(new InvShare(new InvCache(ezRedisMessenger)));
        getCommand("kalyachat").setExecutor((sender, command, label, args) -> {
            if(args.length == 1){
                if(args[0].equalsIgnoreCase("reload")&&sender.hasPermission(Permission.KALYA_CHAT_ADMIN.getPermission())){
                    loadYML();
                    sender.sendMessage("§aConfig reloaded");
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
                                KalyaChat config
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
        if(rsp==null)return null;
        return rsp.getProvider();
    }
    @Override
    public void onDisable() {
        this.ezRedisMessenger.destroy();
    }

    public static KalyaChat getInstance() {
        return instance;
    }

    public EzRedisMessenger getRedisMessenger() {
        return ezRedisMessenger;
    }

    public RedisDataManager getRedisDataManager() {
        return redisDataManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}
