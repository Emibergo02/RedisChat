package dev.unnm3d.redischat.chat.filters.incoming;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PermissionFilter extends AbstractFilter<FiltersConfig.FilterSettings> {

    public PermissionFilter(FiltersConfig.FilterSettings filterSettings) {
        super("permission", Direction.INCOMING, filterSettings);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender receiver, @NotNull ChatMessage chatMessage, ChatMessage... previousMessages) {
        for (String permission : chatMessage.getReceiver().getPermissions()) {
            if (!receiver.hasPermission(permission)) {
                return new FilterResult(chatMessage, true, null);
            }
        }

        //Default read permission check
        final String permission = Permissions.CHANNEL_PREFIX.getPermission() + chatMessage.getReceiver().getName();
        if (!(receiver.hasPermission(permission) || receiver.hasPermission(permission + ".read"))) {
            return new FilterResult(chatMessage, true, null);
        }

        return new FilterResult(chatMessage, false, null);
    }
}
