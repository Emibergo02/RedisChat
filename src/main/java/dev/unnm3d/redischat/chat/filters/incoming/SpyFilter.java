package dev.unnm3d.redischat.chat.filters.incoming;

import de.exlll.configlib.Configuration;
import de.exlll.configlib.PolymorphicTypes;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


public class SpyFilter extends AbstractFilter<SpyFilter.SpyFilterProperties> {

    public static final String FILTER_NAME = "spy";
    private final RedisChat plugin;

    public SpyFilter(RedisChat plugin, SpyFilterProperties filterSettings) {
        super(FILTER_NAME, Direction.INCOMING, filterSettings);
        this.plugin = plugin;
    }

    public SpyFilter() {
        this(RedisChat.getInstance(), new SpyFilterProperties());
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull NewChatMessage chatMessage, NewChatMessage... previousMessages) {
        if (chatMessage.getReceiver().isPlayer()) {
            final Component spyComponent = MiniMessage.miniMessage().deserialize(plugin.messages.spychat_format
                            .replace("%receiver%", chatMessage.getReceiver().getName())
                            .replace("%sender%", chatMessage.getSender().getName()))
                    .replaceText(builder -> builder.matchLiteral("%message%")
                            .replacement(MiniMessage.miniMessage().deserialize(chatMessage.getContent())
                            ));

            //Send to spies
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(player -> plugin.getSpyManager().isSpying(player.getName()))
                    .forEach(player -> plugin.getComponentProvider().sendComponentOrCache(player, spyComponent));

            //Log spy component
            if (filterSettings.logSpyMessages)
                plugin.getComponentProvider().logComponent(spyComponent);
        }
        return new FilterResult(chatMessage, false, null);
    }


    @Getter
    @PolymorphicTypes({
            @PolymorphicTypes.Type(type = FiltersConfig.FilterSettings.class, alias = "filter"),
            @PolymorphicTypes.Type(type = SpyFilterProperties.class, alias = "spy")
    })
    public static class SpyFilterProperties extends FiltersConfig.FilterSettings {
        private boolean logSpyMessages = true;

        public SpyFilterProperties() {
            super(FILTER_NAME,true, 1, Set.of(AudienceType.PLAYER), Set.of());
        }
    }
}
