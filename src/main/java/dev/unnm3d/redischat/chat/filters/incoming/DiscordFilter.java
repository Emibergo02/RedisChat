package dev.unnm3d.redischat.chat.filters.incoming;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


public class DiscordFilter extends AbstractFilter<FiltersConfig.FilterSettings> {

    public DiscordFilter(FiltersConfig.FilterSettings filterSettings) {
        super("discord", Direction.INCOMING, filterSettings);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        if (message.getReceiver().isDiscord()) {
            RedisChat.getInstance().getDiscordHook().sendDiscordMessage(message.getReceiver().getName(), message);
            return new FilterResult(message, true);
        }

        return new FilterResult(message, false);
    }
}
