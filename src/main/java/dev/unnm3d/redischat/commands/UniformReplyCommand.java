package dev.unnm3d.redischat.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.Permission;
import net.william278.uniform.paper.LegacyPaperCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

@AllArgsConstructor
public class UniformReplyCommand implements RedisChatCommand {

    @Override
    public LegacyPaperCommand getCommand() {
        RedisChat plugin = RedisChat.getInstance();
        return LegacyPaperCommand.builder("reply")
                .setAliases(plugin.config.commandAliases.getOrDefault("reply", List.of()))
                .setPermission(new Permission(Permissions.MESSAGE.getPermission(), Permission.Default.TRUE))
                .setDescription("Send a message to another player")
                .addArgument("content", StringArgumentType.greedyString(),
                        (commandContext, builder) -> {
                            if (builder.getRemaining().isEmpty()) {
                                builder.suggest(plugin.messages.replySuggestion);
                            }
                            return builder.buildFuture();
                        })
                .execute(commandContext -> RedisChat.getScheduler().runTaskAsynchronously(() -> {
                    CommandSender sender = commandContext.getSource();
                    plugin.getDataManager().getReplyName(sender.getName())
                            .thenAcceptAsync(receiverOpt -> {
                                if (receiverOpt.isEmpty()) {
                                    plugin.getComponentProvider().sendMessage(sender, plugin.messages.no_reply_found);
                                    return;
                                }

                                String receiverName = receiverOpt.get();
                                if (!plugin.getPlayerListManager().getPlayerList(sender).contains(receiverName)) {
                                    plugin.getComponentProvider().sendMessage(sender,
                                            plugin.messages.reply_not_online.replace("%player%", receiverName));
                                    return;
                                }

                                String content = commandContext.getArgument("content", String.class);
                                plugin.getChannelManager().outgoingPrivateMessage(sender, receiverName, content);

                                plugin.getDataManager().setReplyName(receiverName, sender.getName());

                            }, plugin.getExecutorService());
                }), "content")
                .build();
    }
}
