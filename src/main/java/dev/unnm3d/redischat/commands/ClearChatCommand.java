package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.TextParser;
import dev.unnm3d.redischat.redis.ChatPacket;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ClearChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Permission.REDIS_CHAT_CLEARCHAT.getPermission())) return false;
        new BukkitRunnable() {
            @Override
            public void run() {
                String message = MiniMessage.miniMessage().serialize(TextParser.parse(null, RedisChat.config.clear_chat_message.replace("%message%", String.join(" ", args))));
                RedisChat.getInstance().getRedisDataManager().sendObjectPacket(new ChatPacket(null, message));
            }
        }.runTaskAsynchronously(RedisChat.getInstance());
        return false;
    }
}
