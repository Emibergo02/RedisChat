package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;

public class MailReceivedEvent extends MailEvent {

    /**
     * Event that is called when a mail is received by the server
     * @param mail The mail that is being received
     */
    public MailReceivedEvent(Mail mail) {
        super(mail);
    }
}
