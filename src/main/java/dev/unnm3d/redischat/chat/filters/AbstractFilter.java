package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
public abstract class AbstractFilter<T extends FiltersConfig.FilterSettings> implements BiFunction<CommandSender, ChatMessage, FilterResult> {

    protected final String name;
    protected final Direction direction;
    @Setter
    protected T filterSettings;

    @Override
    public FilterResult apply(CommandSender player, ChatMessage chatMessage) {
        return applyWithPrevious(player, chatMessage);
    }

    public abstract FilterResult applyWithPrevious(CommandSender receiver, @NotNull ChatMessage message, ChatMessage... previousMessages);


    public enum Direction {
        INCOMING,
        OUTGOING,
        BOTH
    }
}
