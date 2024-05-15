package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Cancellable;

public class MailReceivedEvent extends MailEvent  {
    public MailReceivedEvent(Mail mail) {
        super(mail);
    }
}
