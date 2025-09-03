package dev.unnm3d.redischat.task;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.google.common.base.Strings;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChannelAudience;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;


public class AnnouncerTask extends UniversalRunnable {

    @Getter
    private final RedisChat plugin;
    private final String message;
    @Getter
    private final String channelName;
    @Getter
    private final int delay;
    @Getter
    private final int interval;

    public AnnouncerTask(RedisChat plugin, String message, String channelName, int delay, int interval) {
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
        final Channel receiverChannel = plugin.getChannelManager().getChannel(channelName, null)
                .orElse(plugin.getChannelManager().getPublicChannel(null));

        final ChatMessage chMsg = new ChatMessage(
                new ChannelAudience(),
                "{message}",
                MiniMessage.miniMessage().serialize(plugin.getComponentProvider().parse(null,
                        getMessage(), true, false, false)),
                receiverChannel
        );

        if (receiverChannel.getProximityDistance() >= 0) {// Send to local server
            plugin.getChannelManager().sendGenericChat(chMsg);
            return;
        }
        plugin.getDataManager().sendChatMessage(chMsg);
    }

    public @NotNull String getMessage() {
        return Strings.nullToEmpty(message);
    }

}
