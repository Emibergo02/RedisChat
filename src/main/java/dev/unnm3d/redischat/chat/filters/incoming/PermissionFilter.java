package dev.unnm3d.redischat.chat.filters.incoming;

import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PermissionFilter extends AbstractFilter<PermissionFilter.PermissionFilterProperties> {


    public PermissionFilter(PermissionFilterProperties filterSettings) {
        super("permission", Direction.INCOMING, filterSettings);
    }

    public PermissionFilter() {
        this(new PermissionFilterProperties());
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

    public static PermissionFilterProperties getDefaultFilterSettings() {
        return new PermissionFilterProperties();
    }

    public static class PermissionFilterProperties extends FiltersConfig.FilterSettings {
        public PermissionFilterProperties() {
            super(true, 1, Set.of(AudienceType.CHANNEL), Set.of());
        }
    }
}
