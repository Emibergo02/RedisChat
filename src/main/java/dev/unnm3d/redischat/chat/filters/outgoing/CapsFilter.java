package dev.unnm3d.redischat.chat.filters.outgoing;

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


public class CapsFilter extends AbstractFilter<CapsFilter.CapsFilterProperties> {

    public CapsFilter(CapsFilterProperties filterSettings) {
        super("caps", Direction.OUTGOING, filterSettings);
    }

    public CapsFilter() {
        this(new CapsFilterProperties());
    }


    /**
     * Transform uppercase messages into lowercase
     *
     * @param message The message to transform
     * @return The transformed message
     */
    public boolean antiCaps(@NotNull String message) {
        int capsCount = 0;
        for (char c : message.toCharArray())
            if (Character.isUpperCase(c))
                capsCount++;
        return capsCount > message.length() / 2 && message.length() > 20;//50% of the message is caps and the message is longer than 20 chars
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {
        Optional<Boolean> isFiltered = RedisChat.getInstance().getChannelManager().getChannel(message.getReceiver().getName())
                .map(NewChannel::isFiltered);

        //If the channel is filtered or the receiver is not a channel
        if ((isFiltered.isPresent() && isFiltered.get()) || isFiltered.isEmpty()) {
            if (antiCaps(message.getContent())) {
                message.setContent(message.getContent().toLowerCase());
                return new FilterResult(message, true, Optional.of(
                        RedisChat.getInstance().getComponentProvider().parse(sender,
                                RedisChat.getInstance().messages.caps,
                                true,
                                false,
                                false)
                ));
            }
        }
        return new FilterResult(message, false, Optional.empty());
    }

    public static CapsFilterProperties getDefaultFilterSettings() {
        return new CapsFilterProperties();
    }

    public static class CapsFilterProperties extends FiltersConfig.FilterSettings {
        public CapsFilterProperties() {
            super(true, 1, Set.of(), Set.of());
        }
    }
}
