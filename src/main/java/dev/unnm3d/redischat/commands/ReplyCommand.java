package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;

@AllArgsConstructor
public class ReplyCommand {
    private RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("reply")
                .withPermission(Permissions.MESSAGE.getPermission())
                .withAliases(plugin.config.getCommandAliases("reply"))
                .withArguments(new GreedyStringArgument(plugin.messages.replySuggestion))
                .executesPlayer((sender, args) -> {
                    long init = System.currentTimeMillis();
                    plugin.getDataManager().getReplyName(sender.getName())
                            .thenAcceptAsync(receiver -> {
                                if (receiver.isEmpty()) {
                                    plugin.getComponentProvider().sendMessage(sender, plugin.messages.no_reply_found);
                                    return;
                                } else if (!plugin.getPlayerListManager().getPlayerList(sender).contains(receiver.get())) {
                                    plugin.getComponentProvider().sendMessage(sender, plugin.messages.reply_not_online.replace("%player%", receiver.get()));
                                    return;
                                }
                                if (plugin.config.debug)
                                    Bukkit.getLogger().info("ReplyCommand redis: " + (System.currentTimeMillis() - init) + "ms");

                                String message = (String) args.get(0);

                                plugin.getChannelManager().outgoingPrivateMessage(sender, receiver.get(), message);

                                //Set reply name for /reply
                                plugin.getDataManager().setReplyName(receiver.get(), sender.getName());

                                if (plugin.config.debug)
                                    Bukkit.getLogger().info("ReplyCommand: " + (System.currentTimeMillis() - init) + "ms");
                            }, plugin.getExecutorService());
                });
    }
}
