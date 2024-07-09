package dev.unnm3d.redischat.chat.filters.incoming;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


public class PrivateFilter extends AbstractFilter<FiltersConfig.FilterSettings> {

    public PrivateFilter(FiltersConfig.FilterSettings filterSettings) {
        super("private_in", Direction.INCOMING, filterSettings);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender senderOrReceiver, @NotNull ChatMessage chatMessage, ChatMessage... previousMessages) {
        final ChatFormat chatFormat = RedisChat.getInstance().config.getChatFormat(senderOrReceiver);

        //If the CommandSender is the receiver
        if (chatMessage.getReceiver().getName().equals(senderOrReceiver.getName())) {
            chatMessage.setFormat(chatFormat.receive_private_format().replace("%sender%", chatMessage.getSender().getName()));
            return new FilterResult(chatMessage, false);
        }

        //If the CommandSender is not the receiver or the sender, block the message
        return new FilterResult(chatMessage, true);
    }

}
