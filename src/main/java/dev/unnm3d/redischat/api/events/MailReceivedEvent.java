package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;

public class MailReceivedEvent extends MailEvent {
    public MailReceivedEvent(Mail mail) {
        super(mail);
    }
}
