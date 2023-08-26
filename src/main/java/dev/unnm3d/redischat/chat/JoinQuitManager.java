package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

@AllArgsConstructor
public class JoinQuitManager implements Listener {
    private RedisChat redisChat;

    @EventHandler
    public void onJoin(PlayerJoinEvent joinEvent) {
        joinEvent.setJoinMessage(null);
        if (redisChat.getPlayerListManager().getPlayerList().contains(joinEvent.getPlayer().getName())) return;

        List<ChatFormat> chatFormatList = redisChat.config.getChatFormats(joinEvent.getPlayer());
        if (chatFormatList.isEmpty()) return;
        ChatFormat chatFormat = chatFormatList.get(0);

        //Send join message to everyone
        redisChat.getDataManager().sendChatMessage(new ChatMessageInfo(null,
                MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                        joinEvent.getPlayer(),
                        chatFormat.join_format(),
                        true,
                        false,
                        false)),
                null));

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent quitEvent) {
        List<ChatFormat> chatFormatList = redisChat.config.getChatFormats(quitEvent.getPlayer());
        if (chatFormatList.isEmpty()) return;
        String parsedSerializedQuit=MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                quitEvent.getPlayer(),
                chatFormatList.get(0).quit_format(),
                true,
                false,
                false));

        redisChat.getServer().getScheduler().runTaskLaterAsynchronously(redisChat, () -> {
            // If the player is still online, they are probably reconnecting
            if (redisChat.getPlayerListManager().getPlayerList().contains(quitEvent.getPlayer().getName())) return;

            redisChat.getDataManager().sendChatMessage(new ChatMessageInfo(null,
                    parsedSerializedQuit,
                    null));
        }, 100L);

    }

}
