package dev.unnm3d.kalyachat.commands;

import dev.unnm3d.kalyachat.Config;
import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.Permission;
import dev.unnm3d.kalyachat.chat.TextParser;
import dev.unnm3d.kalyachat.redis.Channel;
import dev.unnm3d.kalyachat.redis.ChatPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ReplyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player))return false;
        if(args.length==0)return false;
        long init=System.currentTimeMillis();
        new BukkitRunnable(){
            @Override
            public void run() {

                Optional<String> receiver = KalyaChat.getInstance().getRedisDataManager().getReplyName(sender.getName());

                if(receiver.isEmpty()){
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(KalyaChat.config.no_reply_found));
                    return;
                }else if(!PlayerListManager.getPlayerList().contains(receiver.get())){
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(KalyaChat.config.reply_not_online.replace("%player%",receiver.get())));
                    return;
                }



                String message=String.join(" ",args);
                List<Config.ChatFormat> chatFormatList = KalyaChat.config.getChatFormats(sender);
                if (chatFormatList.isEmpty()) return;

                Component formatted = TextParser.parse(sender, chatFormatList.get(0).private_format().replace("%receiver%", receiver.get()).replace("%sender%", sender.getName()));

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
                KalyaChat.getInstance().getRedisMessenger().sendObjectPacketAsync(Channel.CHAT.getChannelName(), new ChatPacket( sender.getName(), MiniMessage.miniMessage().serialize(toBeReplaced),receiver.get()));
                KalyaChat.getInstance().getChatListener().onSenderPrivateChat(sender,formatted);
                KalyaChat.getInstance().getRedisDataManager().setReplyName(receiver.get(),sender.getName());
                System.out.println("ReplyCommand: "+(System.currentTimeMillis()-init)+"ms");
            }
        }.runTaskAsynchronously(KalyaChat.getInstance());

        return false;
    }
}
