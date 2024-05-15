package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.Getter;

@Getter
public class MailEditorEvent extends MailEvent {
    private final MailEditorState state;

    public MailEditorEvent(Mail mail, MailEditorState state) {
        super(mail);
        this.state = state;
    }

    public enum MailEditorState {
        STARTED,
        COMPLETED
    }
}
