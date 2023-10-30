package dev.unnm3d.redischat.task;

import com.google.common.base.Strings;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import lombok.Getter;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;


public class AnnounceTask extends BukkitRunnable {

    @Getter
    private final RedisChat plugin;
    private final String message;
    @Getter
    private final String channelName;
    @Getter
    private final int delay;
    @Getter
    private final int interval;

    public AnnounceTask(RedisChat plugin, String message, String channelName, int delay, int interval) {
        this.plugin = plugin;
        this.message = message;
        this.channelName = channelName;
        this.delay = delay;
        this.interval = interval;
    }

    public void start() {
        try {
            runTaskTimerAsynchronously(plugin, delay * 20L, interval * 20L);
        } catch (IllegalStateException alreadyStarted) {
            getPlugin().getLogger().warning("AnnounceTask already started");
        }
    }

    @Override
    public void run() {
        plugin.getDataManager().sendChatMessage(
                ChatMessageInfo.craftChannelChatMessage(
                        KnownChatEntities.SERVER_SENDER.toString(),
                        getMessage(),
                        null,
                        channelName.isEmpty() ? null : channelName));
    }

    public @NotNull String getMessage() {
        return Strings.nullToEmpty(message);
    }

}
