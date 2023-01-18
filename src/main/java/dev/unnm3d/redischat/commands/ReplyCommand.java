package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Config;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.TextParser;
import dev.unnm3d.redischat.redis.ChatPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
        if (!(sender instanceof Player)) return false;
        if (args.length == 0) return false;
        long init = System.currentTimeMillis();
        new BukkitRunnable() {
            @Override
            public void run() {

                Optional<String> receiver = RedisChat.getInstance().getRedisDataManager().getReplyName(sender.getName());

                if (receiver.isEmpty()) {
                    RedisChat.config.sendMessage(sender, RedisChat.config.no_reply_found);
                    return;
                } else if (!PlayerListManager.getPlayerList().contains(receiver.get())) {
                    RedisChat.config.sendMessage(sender, RedisChat.config.reply_not_online.replace("%player%", receiver.get()));
                    return;
                }
                if (RedisChat.config.debug)
                    Bukkit.getLogger().info("ReplyCommand redis: " + (System.currentTimeMillis() - init) + "ms");


                String message = String.join(" ", args);
                List<Config.ChatFormat> chatFormatList = RedisChat.config.getChatFormats(sender);
                if (chatFormatList.isEmpty()) return;

                Component formatted = TextParser.parse(sender, chatFormatList.get(0).private_format().replace("%receiver%", receiver.get()).replace("%sender%", sender.getName()));

                //Check for minimessage tags permission
                boolean parsePlaceholders = true;
                if (!sender.hasPermission(Permission.REDIS_CHAT_USE_FORMATTING.getPermission())) {
                    message = TextParser.purgeTags(message);
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
                RedisChat.getInstance().getRedisDataManager().sendObjectPacket(new ChatPacket(sender.getName(), MiniMessage.miniMessage().serialize(toBeReplaced), receiver.get()));
                RedisChat.getInstance().getChatListener().onSenderPrivateChat(sender, formatted);
                RedisChat.getInstance().getRedisDataManager().setReplyName(receiver.get(), sender.getName());
                if (RedisChat.config.debug)
                    Bukkit.getLogger().info("ReplyCommand: " + (System.currentTimeMillis() - init) + "ms");
            }
        }.runTaskAsynchronously(RedisChat.getInstance());

        return false;
    }
}
