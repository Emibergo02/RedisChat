package dev.unnm3d.redischat.chat.filters.incoming;

import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


public class DiscordFilter extends AbstractFilter<DiscordFilter.DiscordFilterProperties> {


    public DiscordFilter(DiscordFilterProperties filterSettings) {
        super("discord", Direction.INCOMING, filterSettings);
    }

    public DiscordFilter() {
        this(new DiscordFilterProperties());
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {
        if (message.getReceiver().isDiscord()) {

            //RedisChat.getInstance().getDiscordHook().sendDiscordMessage(channel, message);
            return new FilterResult(message, true, null);
        }

        return new FilterResult(message, false, null);
    }


    public static DiscordFilter.DiscordFilterProperties getDefaultFilterSettings() {
        return new DiscordFilterProperties();
    }

    public static class DiscordFilterProperties extends FiltersConfig.FilterSettings {
        public DiscordFilterProperties() {
            super(true, 1, Set.of(AudienceType.DISCORD), Set.of());
        }
    }
}
