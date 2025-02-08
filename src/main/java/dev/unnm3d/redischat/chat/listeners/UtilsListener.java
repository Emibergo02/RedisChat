package dev.unnm3d.redischat.chat.listeners;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.mail.Mail;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AllArgsConstructor
public class UtilsListener implements Listener {
    private final RedisChat plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinSpy(PlayerJoinEvent event) {
        plugin.getSpyManager().onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinOthers(PlayerJoinEvent event) {
        plugin.getDataManager().getActivePlayerChannel(event.getPlayer().getName())
                .thenAccept(channelName ->
                        plugin.getChannelManager().updateActiveChannel(event.getPlayer().getName(), channelName));

        if (!plugin.config.remindMailOnJoin || plugin.getPlayerListManager().getPlayerList(event.getPlayer())
                .contains(event.getPlayer().getName())) return;

        //Send mail reminder
        plugin.getMailGUIManager().getPublicMails(event.getPlayer().getName())
                .thenAccept(mails -> {
                    if (mails.isEmpty()) return;
                    for (Mail mail : mails) {
                        if (mail.isRead()) continue;
                        plugin.messages.sendMessage(event.getPlayer(), plugin.messages.mailReceived
                                .replace("%sender%", mail.getSender())
                                .replace("%title%", mail.getTitle()));
                    }
                });

        //Send mail reminder
        plugin.getMailGUIManager().getPrivateMails(event.getPlayer().getName())
                .thenAccept(mails -> {
                    if (mails.isEmpty()) return;
                    for (Mail mail : mails) {
                        if (mail.isRead()) continue;
                        plugin.messages.sendMessage(event.getPlayer(), plugin.messages.mailReceived
                                .replace("%sender%", mail.getSender())
                                .replace("%title%", mail.getTitle()));
                    }
                });
    }
}
