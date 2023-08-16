package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class ReplyCommand {
    private RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("reply")
                .withPermission(Permission.REDIS_CHAT_MESSAGE.getPermission())
                .withAliases("r")
                .withArguments(new GreedyStringArgument("message"))
                .executesPlayer((sender, args) -> {
                    long init = System.currentTimeMillis();
                    new BukkitRunnable() {
                        @Override
                        public void run() {

                            Optional<String> receiver = plugin.getDataManager().getReplyName(sender.getName());

                            if (receiver.isEmpty()) {
                                plugin.getComponentProvider().sendMessage(sender, plugin.messages.no_reply_found);
                                return;
                            } else if (!plugin.getPlayerListManager().getPlayerList().contains(receiver.get())) {
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
                            boolean parsePlaceholders = true;
                            if (!sender.hasPermission(Permission.REDIS_CHAT_USE_FORMATTING.getPermission())) {
                                message = plugin.getComponentProvider().purgeTags(message);
                                parsePlaceholders = false;
                            }
                            // remove blacklisted stuff
                            message = plugin.getComponentProvider().sanitize(message);


                            //Parse into minimessage (placeholders, tags and mentions)
                            Component toBeReplaced = plugin.getComponentProvider().parse(sender, message, parsePlaceholders, true, true, plugin.getComponentProvider().getRedisChatTagResolver(sender, chatFormatList.get(0)));

                            //Send to other servers
                            plugin.getDataManager().sendChatMessage(new ChatMessageInfo(sender.getName(),
                                    MiniMessage.miniMessage().serialize(formatted),
                                    MiniMessage.miniMessage().serialize(toBeReplaced),
                                    receiver.get()));

                            plugin.getChatListener().onSenderPrivateChat(sender, formatted.replaceText(aBuilder -> aBuilder.matchLiteral("%message%").replacement(toBeReplaced)));
                            if (!plugin.config.replyToLastMessaged) {
                                plugin.getDataManager().setReplyName(receiver.get(), sender.getName());
                            }
                            if (plugin.config.debug)
                                Bukkit.getLogger().info("ReplyCommand: " + (System.currentTimeMillis() - init) + "ms");
                        }
                    }.runTaskAsynchronously(plugin);
                });
    }
}
