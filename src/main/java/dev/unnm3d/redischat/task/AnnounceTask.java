package dev.unnm3d.redischat.task;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import org.bukkit.scheduler.BukkitRunnable;


public class AnnounceTask extends BukkitRunnable {

    private final RedisChat plugin;
    private final String message;
    private final String permission;
    private final int delay;
    private final int interval;

    public AnnounceTask(RedisChat plugin, String message, String permission, int delay, int interval) {
        this.plugin = plugin;
        this.message = message;
        this.permission = permission;
        this.delay = delay;
        this.interval = interval;
    }

    public void start() {
        runTaskTimerAsynchronously(plugin, delay * 20L, interval * 20L);
    }

    @Override
    public void run() {
        plugin.getRedisDataManager().sendObjectPacket(
                new ChatMessageInfo(KnownChatEntities.SERVER_SENDER.toString(),
                        message,
                        permission.isEmpty() ? KnownChatEntities.BROADCAST.toString() : KnownChatEntities.PERMISSION_MULTICAST + permission));
    }
}
