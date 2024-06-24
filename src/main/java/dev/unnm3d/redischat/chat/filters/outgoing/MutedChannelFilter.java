package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class MutedChannelFilter extends AbstractFilter<FiltersConfig.FilterSettings> {
    public static final String FILTER_NAME = "muted_channel";
    private final RedisChat plugin;

    public MutedChannelFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super(FILTER_NAME, Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    public MutedChannelFilter() {
        this(RedisChat.getInstance(), new FiltersConfig.FilterSettings(FILTER_NAME,true, 1, Set.of(AudienceType.CHANNEL), Set.of()));
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {

        if (plugin.getChannelManager().getMuteManager().isMutedOnChannel(sender.getName(), message.getReceiver().getName())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.muted_on_channel.replace("%channel%", message.getReceiver().getName()),
                            true,
                            false,
                            false)
            ));
        }
        if (!sender.hasPermission(Permissions.CHANNEL_PREFIX.getPermission() + message.getReceiver().getName())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.channelNoPermission.replace("%channel%", message.getReceiver().getName()),
                            true,
                            false,
                            false)
            ));
        }

        return new FilterResult(message, false, Optional.empty());
    }
}
