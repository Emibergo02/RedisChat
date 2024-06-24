package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.PolymorphicTypes;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class WordBlacklistFilter extends AbstractFilter<WordBlacklistFilter.WordBlacklistFilterProperties> {
    public static final String FILTER_NAME = "word_blacklist";
    private final RedisChat plugin;

    public WordBlacklistFilter(RedisChat plugin, WordBlacklistFilterProperties filterSettings) {
        super(FILTER_NAME, Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    public WordBlacklistFilter() {
        this(RedisChat.getInstance(), new WordBlacklistFilterProperties());
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage message, NewChatMessage... previousMessages) {
        String sanitized = message.getContent();
        for (String regex : plugin.config.regex_blacklist) {
            sanitized = sanitized.replaceAll(regex, plugin.config.blacklistReplacement);
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


    public static WordBlacklistFilter.WordBlacklistFilterProperties getDefaultFilterSettings() {
        return new WordBlacklistFilterProperties();
    }


    public static class WordBlacklistFilterProperties extends FiltersConfig.FilterSettings {
        private boolean blockCensoredMessage = true;

        public WordBlacklistFilterProperties() {
            super(FILTER_NAME,true, 1, Set.of(), Set.of());
        }
    }
}
