package dev.unnm3d.redischat.moderation;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class StaffChatCommand {
    private RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("staffchat")
                .withPermission(Permissions.ADMIN_STAFF_CHAT.getPermission())
                .withAliases("sc")
                .withArguments(new GreedyStringArgument(plugin.messages.staffChatSuggestion))
                .executes((sender, args) -> {
                    String message = (String) args.get(0);
                    if (message == null) return;
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                            plugin.getChannelManager().playerChannelMessage(sender, message, plugin.getChannelManager().getStaffChatChannel()));
                });
    }
}
