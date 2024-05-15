package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.event.Cancellable;

public class MailSendEvent extends MailEvent implements Cancellable {
    private boolean cancelled = false;

    public MailSendEvent(Mail mail) {
        super(mail);
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
