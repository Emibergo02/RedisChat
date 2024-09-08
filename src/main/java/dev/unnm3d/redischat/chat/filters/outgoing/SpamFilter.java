package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


public class SpamFilter extends AbstractFilter<FiltersConfig.FilterSettings> {
    private final RedisChat plugin;

    public SpamFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super("spam", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        if (!message.getReceiver().isChannel()) return new FilterResult(message, false, Optional.empty());

        if (!sender.hasPermission(Permissions.BYPASS_RATE_LIMIT.getPermission())) {
            Optional<Channel> channel = plugin.getChannelManager().getChannel(message.getReceiver().getName(), null);
            if (channel.isPresent() && plugin.getDataManager().isRateLimited(sender.getName(), channel.get())) {
                return new FilterResult(message, true, Optional.of(
                        plugin.getComponentProvider().parse(sender,
                                plugin.messages.rate_limited,
                                true,
                                false,
                                false)
                ));
            }
        }

        return new FilterResult(message, false, Optional.empty());
    }
}
