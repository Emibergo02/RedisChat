package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

        if (redisChat.getPlayerListManager().isVanished(joinEvent.getPlayer())) return;

        //Join event happens at the same time as the quit event in the other server (we need to delay it)
        RedisChat.getScheduler().runTaskLater(() ->
                redisChat.getDataManager().sendRejoin(joinEvent.getPlayer().getName()), redisChat.config.rejoinSendDelay / 50L);

        if (redisChat.getPlayerListManager().getPlayerList(joinEvent.getPlayer())
                .contains(joinEvent.getPlayer().getName())) return;


        if (!joinEvent.getPlayer().hasPlayedBefore() && !redisChat.config.first_join_message.isEmpty()) {
            redisChat.getDataManager().sendChatMessage(new ChatMessage(
                    MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                            joinEvent.getPlayer(),
                            redisChat.config.first_join_message,
                            true,
                            false,
                            false))
            ));
            return;
        }

        final ChatFormat chatFormat = redisChat.config.getChatFormat(joinEvent.getPlayer());

        try {
            if (chatFormat.join_format().isEmpty()) return;
        } catch (NullPointerException e) {
            redisChat.getLogger().severe("You didn't set a join format for the player " + joinEvent.getPlayer().getName() + ". Check formats section inside config.yml file!");
            return;
        }

        //Send join message to everyone
        redisChat.getDataManager().sendChatMessage(new ChatMessage(
                MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                        joinEvent.getPlayer(),
                        chatFormat.join_format(),
                        true,
                        false,
                        false)), Permissions.JOIN_QUIT.getPermission()
        ));

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent quitEvent) {
        quitEvent.setQuitMessage(null);

        if (redisChat.getPlayerListManager().isVanished(quitEvent.getPlayer())) return;

        //Get quit message
        final ChatFormat chatFormat = redisChat.config.getChatFormat(quitEvent.getPlayer());
        try {
            if (chatFormat.quit_format().isEmpty()) return;
        } catch (NullPointerException e) {
            redisChat.getLogger().severe("You didn't set a quit format for the player " + quitEvent.getPlayer().getName() + ". Check formats section inside config.yml file!");
            return;
        }

        final String parsedQuitMessage = MiniMessage.miniMessage().serialize(redisChat.getComponentProvider().parse(
                quitEvent.getPlayer(),
                chatFormat.quit_format(),
                true,
                false,
                false));

        findPlayerRequests.put(quitEvent.getPlayer().getName(),
                craftRejoinFuture(quitEvent.getPlayer().getName(), parsedQuitMessage));

    }

    private CompletableFuture<Void> craftRejoinFuture(String playerName, String parsedQuitMessage) {
        return new CompletableFuture<>()
                .thenAccept(aVoid -> findPlayerRequests.remove(playerName)) //Remove from map, player rejoined
                .orTimeout(redisChat.config.quitSendWaiting, TimeUnit.MILLISECONDS)
                .exceptionally(onTimeout -> {                               //Timeout, player quit
                    redisChat.getDataManager().sendChatMessage(
                            new ChatMessage(parsedQuitMessage, Permissions.JOIN_QUIT.getPermission())
                    );
                    return null;
                });
    }

    public void rejoinRequest(String playerName) {
        CompletableFuture<Void> future = findPlayerRequests.remove(playerName);
        if (future != null) future.complete(null);
    }
}
