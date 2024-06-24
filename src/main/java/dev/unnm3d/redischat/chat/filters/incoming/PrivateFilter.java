package dev.unnm3d.redischat.chat.filters.incoming;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


public class PrivateFilter extends AbstractFilter<FiltersConfig.FilterSettings> {
    public static final String FILTER_NAME = "private";

    public PrivateFilter(FiltersConfig.FilterSettings filterSettings) {
        super(FILTER_NAME, Direction.INCOMING, filterSettings);
    }

    public PrivateFilter() {
        this(new FiltersConfig.FilterSettings(FILTER_NAME,true, 1, Set.of(AudienceType.PLAYER), Set.of()));
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull NewChatMessage chatMessage, NewChatMessage... previousMessages) {
        if (chatMessage.getReceiver().isPlayer()) {
            if (!chatMessage.getReceiver().getName().equals(receiver.getName())) {
                return new FilterResult(chatMessage, true, null);
            }

            //Replace the current format to the private message receiver
            final ChatFormat chatFormat = RedisChat.getInstance().config.getChatFormat(receiver);
            chatMessage.setFormat(chatFormat.receive_private_format()
                    .replace("%sender%", chatMessage.getSender().getName()));
        }

        return new FilterResult(chatMessage, false, null);
    }

}
