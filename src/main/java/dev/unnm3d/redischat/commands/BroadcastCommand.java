package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.chat.TextParser;
import dev.unnm3d.redischat.redis.Channel;
import dev.unnm3d.redischat.redis.ChatPacket;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class BroadcastCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.hasPermission(Permission.REDIS_CHAT_BROADCAST.getPermission()))return false;
        new BukkitRunnable(){
            @Override
            public void run() {
            String message=MiniMessage.miniMessage().serialize(TextParser.parse(null, RedisChat.config.broadcast_format.replace("%message%",String.join(" ",args))));
            RedisChat.getInstance().getRedisMessenger().sendObjectPacket(Channel.CHAT.getChannelName(), new ChatPacket(null, message));
        }}.runTaskAsynchronously(RedisChat.getInstance());

        return false;
    }
}
