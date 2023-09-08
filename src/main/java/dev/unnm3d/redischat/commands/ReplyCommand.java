package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.List;

@AllArgsConstructor
public class ReplyCommand {
    private RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("reply")
                .withPermission(Permissions.MESSAGE.getPermission())
                .withAliases("r")
                .withArguments(new GreedyStringArgument(plugin.messages.replySuggestion))
                .executesPlayer((sender, args) -> {
                    long init = System.currentTimeMillis();
                    plugin.getDataManager().getReplyName(sender.getName())
                            .thenAccept(receiver -> {
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
                                assert message != null;

                                List<ChatFormat> chatFormatList = plugin.config.getChatFormats(sender);
                                if (chatFormatList.isEmpty()) return;

                                Component formatted = plugin.getComponentProvider().parse(sender, chatFormatList.get(0).private_format().replace("%receiver%", receiver.get()).replace("%sender%", sender.getName()));

                                //Check for minimessage tags permission
                                boolean parsePlaceholders = sender.hasPermission(Permissions.USE_FORMATTING.getPermission());
                                if (!parsePlaceholders) {
                                    message = plugin.getComponentProvider().purgeTags(message);
                                }

                                // remove blacklisted stuff
                                message = plugin.getComponentProvider().sanitize(message);

                                //Parse into minimessage (placeholders, tags and mentions)
                                Component toBeReplaced = plugin.getComponentProvider().parse(sender, message, parsePlaceholders, true, true, plugin.getComponentProvider().getRedisChatTagResolver(sender));

                                //Send to other servers
                                plugin.getDataManager().sendChatMessage(new ChatMessageInfo(sender.getName(),
                                        MiniMessage.miniMessage().serialize(formatted),
                                        MiniMessage.miniMessage().serialize(toBeReplaced),
                                        receiver.get()));

                                plugin.getComponentProvider().sendMessage(sender, formatted.replaceText(aBuilder -> aBuilder.matchLiteral("%message%").replacement(toBeReplaced)));

                                if (!plugin.config.replyToLastMessaged) {
                                    plugin.getDataManager().setReplyName(receiver.get(), sender.getName());
                                }
                                if (plugin.config.debug)
                                    Bukkit.getLogger().info("ReplyCommand: " + (System.currentTimeMillis() - init) + "ms");
                            });
                });
    }
}
