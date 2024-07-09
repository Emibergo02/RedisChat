package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public class MsgCommand {
    private final RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("msg")
                .withAliases(plugin.config.getCommandAliases("msg"))
                .withPermission(Permissions.MESSAGE.getPermission())
                .withArguments(new GreedyStringArgument(plugin.messages.msgPlayerSuggestion + " " + plugin.messages.msgMessageSuggestion)
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getPlayerListManager().getPlayerList(commandSenderSuggestionInfo.sender()).stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg().toLowerCase()))
                                        .toArray(String[]::new))))
                .executes((sender, args) -> {
                    RedisChat.getScheduler().runTaskAsynchronously(() -> {
                        final String allArgs = (String) args.get(0);
                        if (allArgs == null) return;

                        final String[] argsArr = allArgs.split(" ");
                        final String receiverName = argsArr[0];
                        if (argsArr.length == 1) {
                            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                            return;
                        }

                        //Remove receiver name from message
                        final String message = String.join(" ", Arrays.copyOfRange(argsArr, 1, argsArr.length));

                        if (!plugin.config.debug)
                            if (receiverName.equalsIgnoreCase(sender.getName())) {
                                plugin.messages.sendMessage(sender, plugin.messages.cannot_message_yourself);
                                return;
                            }

                        if (!plugin.getPlayerListManager().getPlayerList(sender).contains(receiverName)) {
                            plugin.messages.sendMessage(sender, plugin.messages.player_not_online.replace("%player%", receiverName));
                            return;
                        }

                        plugin.getChannelManager().outgoingPrivateMessage(sender, receiverName, message);

                        //Set reply name for /reply
                        plugin.getDataManager().setReplyName(receiverName, sender.getName());

                    });
                });


    }
}
