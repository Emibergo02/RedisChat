package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class TagFilter extends AbstractFilter<TagFilter.TagFilterProperties> {
    private final RedisChat plugin;

    public TagFilter(RedisChat plugin, TagFilterProperties filterSettings) {
        super("tag", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    public TagFilter() {
        this(RedisChat.getInstance(), new TagFilterProperties());
    }


    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage chatMessage, NewChatMessage... previousMessages) {
        if (!sender.hasPermission(Permissions.USE_DANGEROUS.getPermission())) {
            chatMessage.setContent(chatMessage.getContent()
                    .replace("run_command", "copy_to_clipboard")
                    .replace("suggest_command", "copy_to_clipboard"));
        }

        if (sender.hasPermission(Permissions.USE_FORMATTING.getPermission()))
            return new FilterResult(chatMessage, false, Optional.empty());

        if (PlaceholderAPI.containsPlaceholders(chatMessage.getContent())) {
            return new FilterResult(chatMessage, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.messageContainsBadWords,
                            true,
                            false,
                            false)
            ));
        }

        chatMessage.setContent(plugin.getComponentProvider().purgeTags(chatMessage.getContent()));

        if (chatMessage.getContent().trim().isEmpty()) {
            return new FilterResult(chatMessage, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.empty_message,
                            true,
                            false,
                            false)
            ));
        }
        return new FilterResult(chatMessage, false, Optional.empty());
    }

    public static TagFilterProperties getDefaultFilterSettings() {
        return new TagFilterProperties();
    }

    public static class TagFilterProperties extends FiltersConfig.FilterSettings {
        public TagFilterProperties() {
            super(true, 1, Set.of(), Set.of());
        }
    }
}
