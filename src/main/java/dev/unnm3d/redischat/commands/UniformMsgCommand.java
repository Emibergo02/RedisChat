package dev.unnm3d.redischat.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import net.william278.uniform.Permission;
import net.william278.uniform.paper.LegacyPaperCommand;

import java.util.List;

@AllArgsConstructor
public class UniformMsgCommand implements RedisChatCommand {

    @Override
    public LegacyPaperCommand getCommand() {
        RedisChat plugin = RedisChat.getInstance();
        return LegacyPaperCommand.builder("msg")
                .setDescription("Send a message to another player")
                .setAliases(plugin.config.commandAliases.getOrDefault("msg", List.of()))
                .setPermission(new Permission(Permissions.MESSAGE.getPermission(), Permission.Default.TRUE))
                .addArgument("target", StringArgumentType.word(), (context, builder) -> {
                    RedisChat.getInstance().getPlayerListManager().getPlayerList(context.getSource())
                            .stream().filter(playerName -> playerName.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .addArgument("content", StringArgumentType.greedyString(),
                        (commandContext, builder) -> {
                            if (builder.getRemaining().isEmpty()) {
                                builder.suggest(plugin.messages.msgMessageSuggestion);
                            }
                            return builder.buildFuture();
                        })
                .execute(commandContext -> RedisChat.getScheduler().runTaskAsynchronously(() -> {
                    String target = commandContext.getArgument("target", String.class);
                    String content = commandContext.getArgument("content", String.class);
                    if (content.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    if (target.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }

                    if (!plugin.config.debug)
                        if (target.equalsIgnoreCase(commandContext.getSource().getName())) {
                            plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.cannot_message_yourself);
                            return;
                        }

                    if (!plugin.getPlayerListManager().getPlayerList(commandContext.getSource()).contains(target)) {
                        plugin.messages.sendMessage(commandContext.getSource(),
                                plugin.messages.player_not_online.replace("%player%", target));
                        return;
                    }

                    plugin.getChannelManager().outgoingPrivateMessage(commandContext.getSource(), target, content);

                    //Set reply name for /reply
                    plugin.getDataManager().setReplyName(target, commandContext.getSource().getName());

                }), "target", "content")
                .build();
    }
}
