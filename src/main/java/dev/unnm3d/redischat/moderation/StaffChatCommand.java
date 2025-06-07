package dev.unnm3d.redischat.moderation;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.commands.RedisChatCommand;
import lombok.AllArgsConstructor;
import net.william278.uniform.Permission;
import net.william278.uniform.paper.LegacyPaperCommand;

import java.util.List;

@AllArgsConstructor
public class StaffChatCommand implements RedisChatCommand {

    @Override
    public LegacyPaperCommand getCommand() {
        return LegacyPaperCommand.builder("staffchat")
                .addPermissions(new Permission(Permissions.ADMIN_STAFF_CHAT.getPermission(), Permission.Default.IF_OP))
                .setAliases(RedisChat.getInstance().config.commandAliases.getOrDefault("staffchat", List.of()))
                .addArgument("content", StringArgumentType.greedyString(),
                        (commandContext, builder) -> {
                            if (builder.getRemaining().isEmpty()) {
                                builder.suggest(RedisChat.getInstance().messages.staffChatSuggestion);
                            }
                            return builder.buildFuture();
                        })
                .execute(commandContext -> {
                    String message = commandContext.getArgument("content", String.class);
                    if (message == null) return;
                    if (message.isEmpty()) {
                        RedisChat.getInstance().messages.sendMessage(commandContext.getSource(), RedisChat.getInstance().messages.missing_arguments);
                        return;
                    }
                    RedisChat.getScheduler().runTaskAsynchronously(() ->
                            RedisChat.getInstance().getChannelManager().outgoingMessage(commandContext.getSource(), RedisChat.getInstance().getChannelManager().getStaffChatChannel(), message));
                }, "content").build();
    }
}
