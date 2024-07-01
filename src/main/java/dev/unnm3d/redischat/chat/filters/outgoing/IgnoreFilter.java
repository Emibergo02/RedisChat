package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public class IgnoreFilter extends AbstractFilter<IgnoreFilter.IgnoreFilterProperties> {

    public static final String FILTER_NAME = "ignore";

    public IgnoreFilter(IgnoreFilterProperties filterSettings) {
        super(FILTER_NAME, Direction.OUTGOING, filterSettings);
    }

    public IgnoreFilter() {
        this(new IgnoreFilterProperties());
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {
        if (RedisChat.getInstance().getChannelManager().getMuteManager().isPlayerIgnored(sender.getName(), message.getSender().getName())) {
            return new FilterResult(message, true, Optional.of(MiniMessage.miniMessage().deserialize(filterSettings.errorMessage)));
        }

        return new FilterResult(message, false, Optional.empty());
    }


    @Configuration
    @Getter
    public static class IgnoreFilterProperties extends FiltersConfig.FilterSettings {
        private String errorMessage;

        public IgnoreFilterProperties() {
            super(true, 4, Set.of(), Set.of());
            this.errorMessage = "<red>You have ignored this player";
        }
    }
}
