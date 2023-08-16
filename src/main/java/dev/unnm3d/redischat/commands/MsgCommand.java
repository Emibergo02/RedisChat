package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@AllArgsConstructor
public class MsgCommand {
    private final RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("msg")
                .withAliases("rmsg", "rpm", "msg", "pm", "rmessage")
                .withPermission(Permission.REDIS_CHAT_MESSAGE.getPermission())
                .withArguments(new StringArgument("player")
                                .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                        plugin.getPlayerListManager().getPlayerList().stream()
                                                .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                                .toArray(String[]::new))),
                        new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        final String receiverName = (String) args.get(0);
                        String message = (String) args.get(1);
                        if (receiverName == null || message == null) {
                            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                            return;
                        }

                        if (receiverName.equalsIgnoreCase(sender.getName())) {
                            plugin.messages.sendMessage(sender, plugin.messages.cannot_message_yourself);
                            return;
                        }

                        if (!plugin.getPlayerListManager().getPlayerList().contains(receiverName)) {
                            plugin.messages.sendMessage(sender, plugin.messages.player_not_online.replace("%player%", receiverName));
                            return;
                        }

                        List<ChatFormat> chatFormatList = plugin.config.getChatFormats(sender);
                        if (chatFormatList.isEmpty()) return;

                        Component formatted = plugin.getComponentProvider().parse(sender,
                                chatFormatList.get(0).private_format()
                                        .replace("%receiver%", receiverName)
                                        .replace("%sender%", sender.getName()));

                        //Check for minimessage tags permission
                        boolean parsePlaceholders = true;
                        if (!sender.hasPermission(Permission.REDIS_CHAT_USE_FORMATTING.getPermission())) {
                            message = plugin.getComponentProvider().purgeTags(message);
                            parsePlaceholders = false;
                        }
                        // remove blacklisted stuff
                        message = plugin.getComponentProvider().sanitize(message);

                        //Check inv update
                        if (sender instanceof Player player) {
                            if (message.contains("<inv>")) {
                                plugin.getDataManager().addInventory(player.getName(), player.getInventory().getContents());
                            }
                            if (message.contains("<item>")) {
                                plugin.getDataManager().addItem(player.getName(), player.getInventory().getItemInMainHand());
                            }
                            if (message.contains("<ec>")) {
                                plugin.getDataManager().addEnderchest(player.getName(), player.getEnderChest().getContents());
                            }
                        }

                        //Parse to minimessage (placeholders, tags and mentions)
                        Component toBeReplaced = plugin.getComponentProvider().parse(sender, message, parsePlaceholders, true, true, plugin.getComponentProvider().getRedisChatTagResolver(sender, chatFormatList.get(0)));

                        //Send to other servers
                        plugin.getDataManager().sendChatMessage(new ChatMessageInfo(sender.getName(),
                                MiniMessage.miniMessage().serialize(formatted),
                                MiniMessage.miniMessage().serialize(toBeReplaced),
                                receiverName));

                        plugin.getChatListener().onSenderPrivateChat(sender, formatted.replaceText(aBuilder -> aBuilder.matchLiteral("%message%").replacement(toBeReplaced)));
                        //Set reply name for /reply
                        plugin.getDataManager().setReplyName(receiverName, sender.getName());


                    });

                });


    }
}
