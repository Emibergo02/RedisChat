package dev.unnm3d.redischat.task;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import org.bukkit.scheduler.BukkitRunnable;


public class AnnounceTask extends BukkitRunnable {

    private final RedisChat plugin;
    private final String message;
    private final String channelName;
    private final int delay;
    private final int interval;

    public AnnounceTask(RedisChat plugin, String message, String channelName, int delay, int interval) {
        this.plugin = plugin;
        this.message = message;
        this.channelName = channelName;
        this.delay = delay;
        this.interval = interval;
    }

    public void start() {
        runTaskTimerAsynchronously(plugin, delay * 20L, interval * 20L);
    }

    @Override
    public void run() {
        plugin.getDataManager().sendChatMessage(
                ChatMessageInfo.craftChannelChatMessage(
                        KnownChatEntities.SERVER_SENDER.toString(),
                        message,
                        null,
                        channelName.isEmpty() ? null : channelName));
    }
}
