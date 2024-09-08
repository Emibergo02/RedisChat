package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


public class TagFilter extends AbstractFilter<FiltersConfig.FilterSettings> {

    private final RedisChat plugin;

    public TagFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super("tag", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage chatMessage, ChatMessage... previousMessages) {
        if (!sender.hasPermission(Permissions.USE_DANGEROUS.getPermission())) {
            chatMessage.setContent(chatMessage.getContent()
                    .replace("run_command", "copy_to_clipboard")
                    .replace("suggest_command", "copy_to_clipboard"));
        }

        if (sender.hasPermission(Permissions.USE_FORMATTING.getPermission()))
            return new FilterResult(chatMessage, false);

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
                    MiniMessage.miniMessage().deserialize(plugin.messages.empty_message)
            ));
        }
        return new FilterResult(chatMessage, false);
    }

}
