package dev.unnm3d.redischat.chat.filters.incoming;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.AudienceType;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class IgnorePlayerFilter extends AbstractFilter<IgnorePlayerFilter.IgnorePlayerFilterProperties> {

    public IgnorePlayerFilter(IgnorePlayerFilterProperties filterSettings) {
        super("ignore_player", Direction.INCOMING, filterSettings);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull ChatMessage chatMessage, ChatMessage... previousMessages) {
        boolean isIgnored = RedisChat.getInstance().getChannelManager().getMuteManager()
                .isPlayerIgnored(receiver.getName(), chatMessage.getSender().getName());


        if (isIgnored && (chatMessage.getReceiver().isPlayer() || filterSettings.ignoreChannelMessages)) {

            if (filterSettings.sendWarnWhenIgnoring &&
                    Arrays.stream(previousMessages)
                            .filter(message -> message.getSender().getName().equals(chatMessage.getSender().getName()))
                            .filter(message -> message.getReceiver().getName().equals(chatMessage.getReceiver().getName()))
                            .findAny()
                            .isEmpty()) {

                return new FilterResult(chatMessage, true,
                        Optional.of(MiniMessage.miniMessage().deserialize(RedisChat.getInstance().messages.ignoredMessageReceiver
                                .replace("%player%", chatMessage.getSender().getName()))));
            }

            return new FilterResult(chatMessage, true);
        }

        return new FilterResult(chatMessage, false);
    }

    @Configuration
    @Getter
    public static class IgnorePlayerFilterProperties extends FiltersConfig.FilterSettings {
        @Comment("If true, send a ignored_player_receiver message when ignoring a player")
        private boolean sendWarnWhenIgnoring;
        @Comment("If true, sendWarnWhenIgnoring will work on channel messages too")
        private boolean ignoreChannelMessages;

        public IgnorePlayerFilterProperties() {
            super(true, 4, Set.of(AudienceType.PLAYER, AudienceType.CHANNEL), Set.of());
            this.ignoreChannelMessages = true;
            this.sendWarnWhenIgnoring = true;
        }
    }
}
