package dev.unnm3d.redischat.moderation;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.configs.Config;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AllArgsConstructor
public class StaffChat implements CommandExecutor {
    private RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<Config.ChatFormat> chatFormatList = plugin.config.getChatFormats(sender);
        if (chatFormatList.isEmpty()) return true;
        staffChat(sender, chatFormatList.get(0), String.join(" ", args));
        return true;
    }

    public void staffChat(CommandSender commandSender, Config.ChatFormat chatFormat, String message) {
        Component formatted = plugin.getComponentProvider().parse(commandSender,
                chatFormat.staff_chat_format().replace("%message%", message));
        plugin.getRedisDataManager().sendObjectPacket(new ChatMessageInfo(
                commandSender.getName(),
                MiniMessage.miniMessage().serialize(formatted),
                KnownChatEntities.PERMISSION_MULTICAST + Permission.REDIS_CHAT_ADMIN_STAFF_CHAT.getPermission()
        ));
    }
}
