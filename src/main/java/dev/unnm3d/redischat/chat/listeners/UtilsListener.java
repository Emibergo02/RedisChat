package dev.unnm3d.redischat.chat.listeners;

import dev.unnm3d.redischat.Permissions;
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
        //Remove chat color placeholder if player doesn't have permission to edit it
        if (!event.getPlayer().hasPermission(Permissions.CHAT_COLOR.getPermission()) &&
                !plugin.getPlaceholderManager().getPlaceholder(event.getPlayer().getName(), "chat_color").isEmpty()) {
            plugin.getPlaceholderManager().removePlayerPlaceholder(event.getPlayer().getName(), "chat_color");
        }

        if (!plugin.config.enableMails) return;
        if (!plugin.config.remindMailOnJoin) return;
        if (plugin.getPlayerListManager().getPlayerList(event.getPlayer()).contains(event.getPlayer().getName()))
            return;

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
