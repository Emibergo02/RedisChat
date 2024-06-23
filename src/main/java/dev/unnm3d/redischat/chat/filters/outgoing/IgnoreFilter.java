package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.moderation.MuteManager;
import dev.unnm3d.redischat.settings.FiltersConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public class IgnoreFilter extends AbstractFilter<IgnoreFilter.IgnoreFilterProperties> {
    private final MuteManager muteManager;

    public IgnoreFilter(MuteManager muteManager, IgnoreFilterProperties filterSettings) {
        super("ignore", Direction.OUTGOING, filterSettings);
        this.muteManager = muteManager;
    }

    public IgnoreFilter() {
        this(RedisChat.getInstance().getChannelManager().getMuteManager(), new IgnoreFilterProperties());
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {
        if (muteManager.isPlayerIgnored(sender.getName(), message.getSender().getName())) {
            return new FilterResult(message, true, Optional.of(MiniMessage.miniMessage().deserialize("<red>You have ignored this player")));
        }

        return new FilterResult(message, false, Optional.empty());
    }


    public static IgnoreFilterProperties getDefaultFilterSettings() {
        return new IgnoreFilterProperties();
    }


    public static class IgnoreFilterProperties extends FiltersConfig.FilterSettings {
        public IgnoreFilterProperties() {
            super(true, 1, Set.of(), Set.of());
        }
    }
}
