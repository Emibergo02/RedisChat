package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Configuration;
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


public class SpamFilter extends AbstractFilter<FiltersConfig.FilterSettings> {
    public static final String FILTER_NAME = "spam";
    private final RedisChat plugin;

    public SpamFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super(FILTER_NAME, Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    public SpamFilter() {
        this(RedisChat.getInstance(), new FiltersConfig.FilterSettings(FILTER_NAME,true,1, Set.of(), Set.of()));
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
}
