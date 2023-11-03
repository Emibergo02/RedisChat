package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatActor;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

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
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        final String allArgs = (String) args.get(0);
                        if (allArgs == null) return;

                        final String[] argsArr = allArgs.split(" ");
                        final String receiverName = argsArr[0];
                        if (argsArr.length == 1) {
                            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                            return;
                        }

                        String message = String.join(" ", Arrays.copyOfRange(argsArr, 1, argsArr.length));

                        if (receiverName.equalsIgnoreCase(sender.getName())) {
                            plugin.messages.sendMessage(sender, plugin.messages.cannot_message_yourself);
                            return;
                        }

                        if (!plugin.getPlayerListManager().getPlayerList(sender).contains(receiverName)) {
                            plugin.messages.sendMessage(sender, plugin.messages.player_not_online.replace("%player%", receiverName));
                            return;
                        }

                        final List<ChatFormat> chatFormatList = plugin.config.getChatFormats(sender);
                        if (chatFormatList.isEmpty()) return;

                        final Component formatted = plugin.getComponentProvider().parse(sender,
                                chatFormatList.get(0).private_format()
                                        .replace("%receiver%", receiverName)
                                        .replace("%sender%", sender.getName()));

                        //Check for minimessage tags permission
                        boolean parsePlaceholders = sender.hasPermission(Permissions.USE_FORMATTING.getPermission());
                        if (!parsePlaceholders) {
                            message = plugin.getComponentProvider().purgeTags(message);
                        }

                        // remove blacklisted stuff
                        message = plugin.getComponentProvider().sanitize(message);

                        //Check inv update
                        message = plugin.getComponentProvider().invShareFormatting(sender, message);

                        //Parse to minimessage (placeholders, tags and mentions)
                        final Component temp = plugin.getComponentProvider().parse(sender, message, parsePlaceholders,
                                true,
                                true,
                                plugin.getComponentProvider().getRedisChatTagResolver(sender));

                        //Parse customs
                        final Component toBeReplaced = plugin.getComponentProvider().parseCustomPlaceholders(sender, temp);

                        //Send to other servers
                        plugin.getDataManager().sendChatMessage(new ChatMessageInfo(new ChatActor(sender.getName(), ChatActor.ActorType.PLAYER),
                                MiniMessage.miniMessage().serialize(formatted),
                                MiniMessage.miniMessage().serialize(toBeReplaced),
                                new ChatActor(receiverName, ChatActor.ActorType.PLAYER)));

                        plugin.getComponentProvider().sendMessage(sender, formatted.replaceText(aBuilder -> aBuilder.matchLiteral("%message%").replacement(toBeReplaced)));
                        //Set reply name for /reply
                        plugin.getDataManager().setReplyName(receiverName, sender.getName());

                    });
                });


    }
}
