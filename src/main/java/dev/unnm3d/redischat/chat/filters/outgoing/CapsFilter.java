package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class CapsFilter extends AbstractFilter<CapsFilter.CapsFilterProperties> {
    private final RedisChat plugin;

    public CapsFilter(RedisChat plugin) {
        super("caps", Direction.OUTGOING, plugin.filterSettings.caps);
        this.plugin = plugin;
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
        return capsCount > message.length() * (filterSettings.percentageCaps / 100.0);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {

        //Remove usernames from the message so players can send UPPERCASE USERNAMES
        String mentionRemoved = message.getContent();
        for (String playerName : plugin.getPlayerListManager().getPlayerList(sender)) {
            mentionRemoved = mentionRemoved.replace(playerName, "");
        }

        //Then check for the presence of caps
        if (antiCaps(mentionRemoved)) {
            message.setContent(message.getContent().toLowerCase());
            return new FilterResult(message, filterSettings.shouldBlock, Optional.of(
                    RedisChat.getInstance().getComponentProvider().parse(sender,
                            RedisChat.getInstance().messages.caps,
                            true,
                            false,
                            false)
            ));
        }

        return new FilterResult(message, false, Optional.empty());
    }


    @Configuration
    @Getter
    public static class CapsFilterProperties extends FiltersConfig.FilterSettings {
        private int percentageCaps;
        @Comment("Should the message be blocked")
        private boolean shouldBlock;

        public CapsFilterProperties() {
            super(true, 8, Set.of(), Set.of());
            this.percentageCaps = 50;
            this.shouldBlock = false;
        }
    }
}
