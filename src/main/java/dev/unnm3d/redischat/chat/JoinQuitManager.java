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

        if (!joinEvent.getPlayer().hasPlayedBefore() && !redisChat.config.first_join_message.isEmpty()) {
            redisChat.getDataManager().sendChatMessage(new ChatMessageInfo(
                    null,
                    MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                            joinEvent.getPlayer(),
                            redisChat.config.first_join_message,
                            true,
                            false,
                            false)),
                    null));
            return;
        }

        List<ChatFormat> chatFormatList = redisChat.config.getChatFormats(joinEvent.getPlayer());
        if (chatFormatList.isEmpty()) return;
        ChatFormat chatFormat = chatFormatList.get(0);

        //Send join message to everyone
        redisChat.getDataManager().sendChatMessage(new ChatMessageInfo(
                null,
                MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                        joinEvent.getPlayer(),
                        chatFormat.join_format(),
                        true,
                        false,
                        false)),
                null));

        redisChat.getDataManager().publishPlayerList(List.of(joinEvent.getPlayer().getName()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent quitEvent) {
        quitEvent.setQuitMessage(null);
        List<ChatFormat> chatFormatList = redisChat.config.getChatFormats(quitEvent.getPlayer());
        if (chatFormatList.isEmpty()) return;

        String parsedQuitMessage = MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                quitEvent.getPlayer(),
                chatFormatList.get(0).quit_format(),
                true,
                false,
                false));
        String playerName = quitEvent.getPlayer().getName();

        //Wait 1 second before sending quit message to everyone
        //This delay prevents misleading quit messages when a player rejoins quickly
        redisChat.getPlayerListManager().removeLocalPlayerName(playerName);
        redisChat.getServer().getScheduler().runTaskLater(redisChat, () -> {
            if (redisChat.getPlayerListManager().getPlayerList().contains(playerName)) return;
            //Send quit message to everyone. since PluginMessages are based on player connection
            redisChat.getDataManager().sendChatMessage(new ChatMessageInfo(null,
                    parsedQuitMessage,
                    null));
        }, 20);

    }

}
