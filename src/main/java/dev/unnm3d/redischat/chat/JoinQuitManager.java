package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.RedisChat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class JoinQuitManager implements Listener {
    private final RedisChat redisChat;
    private final ConcurrentHashMap<String, CompletableFuture<Void>> findPlayerRequests;

    public JoinQuitManager(RedisChat redisChat) {
        this.redisChat = redisChat;
        this.findPlayerRequests = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent joinEvent) {
        joinEvent.setJoinMessage(null);

        //Join event happens at the same time as the quit event in the other server (we need to delay it)
        redisChat.getServer().getScheduler().runTaskLater(redisChat, () ->
                redisChat.getDataManager().sendRejoin(joinEvent.getPlayer().getName()), 5);

        if (redisChat.getPlayerListManager().getPlayerList(joinEvent.getPlayer())
                .contains(joinEvent.getPlayer().getName())) return;

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


    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent quitEvent) {
        quitEvent.setQuitMessage(null);

        //Get quit message
        List<ChatFormat> chatFormatList = redisChat.config.getChatFormats(quitEvent.getPlayer());
        if (chatFormatList.isEmpty()) return;
        String parsedQuitMessage = MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                quitEvent.getPlayer(),
                chatFormatList.get(0).quit_format(),
                true,
                false,
                false));

        findPlayerRequests.put(quitEvent.getPlayer().getName(),
                craftRejoinFuture(quitEvent.getPlayer().getName(), parsedQuitMessage));

    }

    private CompletableFuture<Void> craftRejoinFuture(String playerName, String parsedQuitMessage) {
        return new CompletableFuture<>()
                .thenAccept(aVoid -> findPlayerRequests.remove(playerName)) //Remove from map, player rejoined
                .orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(onTimeout -> {                               //Timeout, player quit
                    redisChat.getDataManager().sendChatMessage(
                            new ChatMessageInfo(null,
                                    parsedQuitMessage,
                                    null));
                    return null;
                });
    }

    public void rejoinRequest(String playerName) {
        CompletableFuture<Void> future = findPlayerRequests.remove(playerName);
        if (future != null) future.complete(null);
    }
}
