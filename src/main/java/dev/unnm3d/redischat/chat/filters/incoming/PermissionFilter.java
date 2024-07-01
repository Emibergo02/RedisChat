package dev.unnm3d.redischat.chat.filters.incoming;

import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PermissionFilter extends AbstractFilter<FiltersConfig.FilterSettings> {
    public static final String FILTER_NAME = "permission";

    public PermissionFilter(FiltersConfig.FilterSettings filterSettings) {
        super(FILTER_NAME, Direction.INCOMING, filterSettings);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull NewChatMessage chatMessage, NewChatMessage... previousMessages) {
        for (String permission : chatMessage.getReceiver().getPermissions()) {
            if (!receiver.hasPermission(permission)) {
                return new FilterResult(chatMessage, true, null);
            }
        }
        return new FilterResult(chatMessage, false, null);
    }
}
