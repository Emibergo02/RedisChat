package dev.unnm3d.redischat.moderation;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.List;

@AllArgsConstructor
public class StaffChat {
    private RedisChat plugin;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("staffchat")
                .withPermission(Permission.REDIS_CHAT_ADMIN_STAFF_CHAT.getPermission())
                .withAliases("sc")
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    List<ChatFormat> chatFormatList = plugin.config.getChatFormats(sender);
                    if (chatFormatList.isEmpty()) return;
                    staffChat(sender, chatFormatList.get(0), (String) args.get(0));
                });
    }

    public void staffChat(CommandSender commandSender, ChatFormat chatFormat, String message) {
        plugin.getDataManager().sendChatMessage(new ChatMessageInfo(
                commandSender.getName(),
                MiniMessage.miniMessage().serialize(
                        plugin.getComponentProvider().parse(commandSender, chatFormat.staff_chat_format())
                ),
                message,
                KnownChatEntities.PERMISSION_MULTICAST + Permission.REDIS_CHAT_ADMIN_STAFF_CHAT.getPermission()
        ));
    }
}
