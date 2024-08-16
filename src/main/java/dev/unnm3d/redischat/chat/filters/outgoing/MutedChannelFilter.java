package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


public class MutedChannelFilter extends AbstractFilter<FiltersConfig.FilterSettings> {

    private final RedisChat plugin;

    public MutedChannelFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super("muted_channel", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {

        if (plugin.getChannelManager().getMuteManager().isMutedOnChannel(sender.getName(), message.getReceiver().getName())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.muted_on_channel.replace("%channel%", message.getReceiver().getName()),
                            true,
                            false,
                            false)
            ));
        }

        final String permission = Permissions.CHANNEL_PREFIX.getPermission() + message.getReceiver().getName();
        if (!(sender.hasPermission(permission) || sender.hasPermission(permission + ".write"))) {
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
