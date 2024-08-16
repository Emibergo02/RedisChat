package dev.unnm3d.redischat.moderation;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaffChatCommand {
    private RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("staffchat")
                .withPermission(Permissions.ADMIN_STAFF_CHAT.getPermission())
                .withAliases(plugin.config.getCommandAliases("staffchat"))
                .withArguments(new GreedyStringArgument(plugin.messages.staffChatSuggestion))
                .executes((sender, args) -> {
                    String message = (String) args.get(0);
                    if (message == null) return;
                    RedisChat.getScheduler().runTaskAsynchronously(() ->
                            plugin.getChannelManager().outgoingMessage(sender, plugin.getChannelManager().getStaffChatChannel(), message));
                });
    }
}
