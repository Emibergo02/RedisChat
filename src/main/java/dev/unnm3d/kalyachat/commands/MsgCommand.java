package dev.unnm3d.kalyachat.commands;

import dev.unnm3d.kalyachat.Config;
import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.Permission;
import dev.unnm3d.kalyachat.chat.TextParser;
import dev.unnm3d.kalyachat.redis.Channel;
import dev.unnm3d.kalyachat.redis.ChatPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MsgCommand implements CommandExecutor {

    public static void sendMsg(String[] args, CommandSender sender, String receiverName) {

        if (!PlayerListManager.getPlayerList().contains(receiverName)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(KalyaChat.config.player_not_online.replace("%player%", receiverName)));
            return;
        }


        Bukkit.getScheduler().runTaskAsynchronously(KalyaChat.getInstance(), () ->
        {
            String message = String.join(" ", args);
            List<Config.ChatFormat> chatFormatList = KalyaChat.config.getChatFormats(sender);
            if (chatFormatList.isEmpty()) return;

            Component formatted = TextParser.parse(sender, chatFormatList.get(0).private_format().replace("%receiver%", receiverName).replace("%sender%", sender.getName()));

            //Check for minimessage tags permission
            boolean parsePlaceholders = true;
            if (!sender.hasPermission(Permission.KALYA_CHAT_USE_FORMATTING.getPermission())) {
                message = TextParser.purify(message);
                parsePlaceholders = false;
            }
            // remove blacklisted stuff
            message = TextParser.sanitize(message);


            //Parse into minimessage (placeholders, tags and mentions)
            Component toBeReplaced = TextParser.parse(sender, message, parsePlaceholders, TextParser.getCustomTagResolver(sender, chatFormatList.get(0)));
            //Put message into format
            formatted = formatted.replaceText(
                    builder -> builder.match("%message%").replacement(toBeReplaced)
            );
            //Send to other servers
            KalyaChat.getInstance().getRedisMessenger().sendObjectPacketAsync(Channel.CHAT.getChannelName(), new ChatPacket( sender.getName(), MiniMessage.miniMessage().serialize(toBeReplaced),receiverName));
            KalyaChat.getInstance().getChatListener().onSenderPrivateChat(sender,formatted);
            KalyaChat.getInstance().getRedisDataManager().setReplyName(receiverName,sender.getName());


        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Permission.KALYA_CHAT_MESSAGE.getPermission())) return true;


        if (args.length < 2) {
            return true;
        }

        String receiverName = args[0];

        // remove first arg[0], since it's the player name and we don't want to include it in the msg
        args = Arrays.copyOfRange(args, 1, args.length);
        // do some stuff to send the message
        sendMsg(args, sender, receiverName);

        return true;
    }
}
