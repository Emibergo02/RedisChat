package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChannel;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class SpamFilter extends AbstractFilter<SpamFilter.SpamFilterProperties> {
    private final RedisChat plugin;

    public SpamFilter(RedisChat plugin, SpamFilterProperties filterSettings) {
        super("spam", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    public SpamFilter() {
        this(RedisChat.getInstance(), new SpamFilterProperties());
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {
        if (!message.getReceiver().isChannel()) return new FilterResult(message, false, Optional.empty());

        if (!sender.hasPermission(Permissions.BYPASS_RATE_LIMIT.getPermission())) {
            Optional<NewChannel> channel = plugin.getChannelManager().getChannel(message.getReceiver().getName());
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


    public static SpamFilterProperties getDefaultFilterSettings() {
        return new SpamFilterProperties();
    }

    public static class SpamFilterProperties extends FiltersConfig.FilterSettings {
        public SpamFilterProperties() {
            super(true, 1, Set.of(), Set.of());
        }
    }
}
