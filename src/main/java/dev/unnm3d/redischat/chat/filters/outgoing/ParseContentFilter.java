package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class ParseContentFilter extends AbstractFilter<ParseContentFilter.ContentProperties> {
    private RedisChat plugin;

    public ParseContentFilter(ContentProperties filterSettings) {
        super("content", Direction.OUTGOING, filterSettings);
    }

    public ParseContentFilter() {
        this(new ParseContentFilter.ContentProperties());
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull NewChatMessage chatMessage, NewChatMessage... previousMessages) {
        chatMessage.setContent(plugin.getComponentProvider().invShareFormatting(sender, chatMessage.getContent()));

        Component contentComponent = plugin.getComponentProvider().parse(
                sender,
                chatMessage.getContent(),
                sender.hasPermission(Permissions.USE_FORMATTING.getPermission()),
                true, true,
                plugin.getComponentProvider().getRedisChatTagResolver(sender));

        //Parse customs
        contentComponent = plugin.getComponentProvider().parseCustomPlaceholders(sender, contentComponent);

        if (PlainTextComponentSerializer.plainText().serialize(contentComponent).trim().isEmpty()) {
            return new FilterResult(chatMessage, true, Optional.of(
                    plugin.getComponentProvider().parse(sender,
                            plugin.messages.empty_message,
                            true,
                            false,
                            false)
            ));
        }
        chatMessage.setContent(MiniMessage.miniMessage().serialize(contentComponent));

        return new FilterResult(chatMessage, false, Optional.empty());
    }


    public static ContentProperties getDefaultFilterSettings() {
        return new ContentProperties();
    }

    public static class ContentProperties extends FiltersConfig.FilterSettings {
        public ContentProperties() {
            super(true, 1, Set.of(), Set.of());
        }
    }
}
