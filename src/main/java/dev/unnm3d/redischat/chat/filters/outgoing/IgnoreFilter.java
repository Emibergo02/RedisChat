package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.AudienceType;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public class IgnoreFilter extends AbstractFilter<IgnoreFilter.IgnoreFilterProperties> {

    private final RedisChat plugin;

    public IgnoreFilter(RedisChat plugin, IgnoreFilter.IgnoreFilterProperties filterSettings) {
        super("ignore", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        if (plugin.getChannelManager().getMuteManager().isPlayerIgnored(sender.getName(), message.getReceiver().getName())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender, plugin.messages.ignoredMessageSender,
                            true, false, false)));
        }
        if (plugin.getChannelManager().getMuteManager().isPlayerIgnored(message.getReceiver().getName(), sender.getName())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender, plugin.messages.receiverIgnoringSender,
                            true, false, false)));
        }

        return new FilterResult(message, false, Optional.empty());
    }

    @Configuration
    @Getter
    public static class IgnoreFilterProperties extends FiltersConfig.FilterSettings {
        private boolean sendWarnWhenIgnored;
        private boolean sendWarnWhenIgnoring;

        public IgnoreFilterProperties() {
            super(true, 4, Set.of(AudienceType.PLAYER), Set.of());
            this.sendWarnWhenIgnored = false;
            this.sendWarnWhenIgnoring = true;
        }
    }
}
