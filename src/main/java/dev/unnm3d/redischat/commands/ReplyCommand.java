package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
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
                                assert message != null;

                                final ChatFormat chatFormat = plugin.config.getChatFormat(sender);

                                final Component formatted = plugin.getComponentProvider().parse(sender,
                                        chatFormat.private_format()
                                                .replace("%receiver%", receiver.get())
                                                .replace("%sender%", sender.getName()),
                                        true,
                                        false,
                                        false);

                                //Check for minimessage tags permission
                                boolean parsePlaceholders = sender.hasPermission(Permissions.USE_FORMATTING.getPermission());
                                if (!parsePlaceholders) {
                                    message = plugin.getComponentProvider().purgeTags(message);
                                }

                                // remove blacklisted stuff
                                message = plugin.getComponentProvider().sanitize(message);

                                //Check inv update
                                message = plugin.getComponentProvider().invShareFormatting(sender, message);

                                //Parse into minimessage (placeholders, tags and mentions)
                                final Component temp = plugin.getComponentProvider().parse(sender, message, parsePlaceholders, true, true, plugin.getComponentProvider().getRedisChatTagResolver(sender));

                                //Parse customs
                                final Component toBeReplaced = plugin.getComponentProvider().parseCustomPlaceholders(sender, temp);

                                //Send to other servers
                                plugin.getDataManager().sendChatMessage(new ChatMessageInfo(new ChatActor(sender.getName(), ChatActor.ActorType.PLAYER),
                                        MiniMessage.miniMessage().serialize(formatted),
                                        MiniMessage.miniMessage().serialize(toBeReplaced),
                                        new ChatActor(receiver.get(), ChatActor.ActorType.PLAYER)
                                ));

                                plugin.getComponentProvider().sendMessage(sender, formatted.replaceText(aBuilder -> aBuilder.matchLiteral("%message%").replacement(toBeReplaced)));

                                plugin.getDataManager().setReplyName(receiver.get(), sender.getName());

                                if (plugin.config.debug)
                                    Bukkit.getLogger().info("ReplyCommand: " + (System.currentTimeMillis() - init) + "ms");
                            }, plugin.getExecutorService());
                });
    }
}
