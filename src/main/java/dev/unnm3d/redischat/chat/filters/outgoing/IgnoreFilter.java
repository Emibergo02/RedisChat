package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class IgnoreFilter extends AbstractFilter<FiltersConfig.FilterSettings> {

    private final RedisChat plugin;

    public IgnoreFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super("ignore", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        if (plugin.getChannelManager().getMuteManager().isPlayerIgnored(sender.getName(), message.getSender().getName())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender, plugin.messages.ignored_player,
                            true, false, false)));
        }

        return new FilterResult(message, false, Optional.empty());
    }
}
