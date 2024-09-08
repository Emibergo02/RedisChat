package dev.unnm3d.redischat.chat.filters.outgoing;

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


public class WordBlacklistFilter extends AbstractFilter<WordBlacklistFilter.WordBlacklistFilterProperties> {
    private final RedisChat plugin;

    public WordBlacklistFilter(RedisChat plugin, WordBlacklistFilterProperties filterSettings) {
        super("word_blacklist", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        String sanitized = message.getContent();
        for (String regex : plugin.config.regex_blacklist) {
            sanitized = sanitized.replaceAll(regex, filterSettings.replacement);
        }

        if (filterSettings.blockCensoredMessage && !sanitized.equals(message.getContent())) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.messageContainsBadWords,
                            true,
                            false,
                            false)
            ));
        }
        //Set the sanitized message
        message.setContent(sanitized);
        return new FilterResult(message, false, Optional.empty());
    }


    @Configuration
    @Getter
    public static class WordBlacklistFilterProperties extends FiltersConfig.FilterSettings {
        private boolean blockCensoredMessage;
        private String replacement;

        public WordBlacklistFilterProperties() {
            super(true, 1, Set.of(), Set.of());
            this.blockCensoredMessage = true;
            this.replacement = "<obf>*****</obf>";
        }
    }
}
