package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


public class ParseContentFilter extends AbstractFilter<ParseContentFilter.ContentProperties> {
    public static final String FILTER_NAME = "content";
    private final RedisChat plugin;

    public ParseContentFilter(ContentProperties filterSettings) {
        super(FILTER_NAME, Direction.OUTGOING, filterSettings);
        this.plugin = RedisChat.getInstance();
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


    @Getter
    public static class ContentProperties extends FiltersConfig.FilterSettings {

        private boolean parseMentions = true;
        private boolean parseLinks = true;
        private boolean parseCustomPlaceholders = true;

        public ContentProperties() {
            super(FILTER_NAME,true, 1, Set.of(), Set.of());
        }
    }
}
