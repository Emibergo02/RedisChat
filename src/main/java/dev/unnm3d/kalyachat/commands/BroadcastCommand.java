package dev.unnm3d.kalyachat.commands;

import dev.unnm3d.kalyachat.KalyaChat;
import dev.unnm3d.kalyachat.Permission;
import dev.unnm3d.kalyachat.chat.TextParser;
import dev.unnm3d.kalyachat.redis.Channel;
import dev.unnm3d.kalyachat.redis.ChatPacket;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class BroadcastCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.hasPermission(Permission.KALYA_CHAT_BROADCAST.getPermission()))return false;
        new BukkitRunnable(){
            @Override
            public void run() {
            String message=MiniMessage.miniMessage().serialize(TextParser.parse(null,KalyaChat.config.broadcast_format.replace("%message%",String.join(" ",args))));
            KalyaChat.getInstance().getRedisMessenger().sendObjectPacket(Channel.CHAT.getChannelName(), new ChatPacket(null, message));
        }}.runTaskAsynchronously(KalyaChat.getInstance());

        return false;
    }
}
