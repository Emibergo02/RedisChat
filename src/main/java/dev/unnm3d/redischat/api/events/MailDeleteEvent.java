package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;


public class MailDeleteEvent extends MailEvent implements Cancellable {
    @Getter
    private final Player deleter;
    private boolean cancelled = false;

    /**
     * Event that is called when a mail is deleted
     * @param mail The mail that is being deleted
     * @param deleter The player that is deleting the mail
     */
    public MailDeleteEvent(Mail mail, Player deleter) {
        super(mail);
        this.deleter = deleter;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
