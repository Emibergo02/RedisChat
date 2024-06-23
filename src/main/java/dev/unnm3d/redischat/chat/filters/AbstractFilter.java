package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
public abstract class AbstractFilter<T extends FiltersConfig.FilterSettings> implements BiFunction<CommandSender, NewChatMessage, FilterResult> {

    private final String name;

    private final Direction direction;

    @Setter
    private T filterSettings;

    @Override
    public FilterResult apply(CommandSender player, NewChatMessage chatMessage) {
        return applyWithPrevious(player, chatMessage);
    }

    public abstract FilterResult applyWithPrevious(CommandSender receiver, @NotNull NewChatMessage message, NewChatMessage... previousMessages);


    public enum Direction {
        INCOMING,
        OUTGOING
    }
}
