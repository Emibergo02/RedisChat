package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.Getter;

@Getter
public class MailEditorEvent extends MailEvent {
    private final MailEditorState state;

    /**
     * Event that is called when a mail is being created via the web editor
     * The event is called when the editor is opened and closed
     *
     * @param mail The mail that is being created
     * @param state The state of the editor
     */
    public MailEditorEvent(Mail mail, MailEditorState state) {
        super(mail);
        this.state = state;
    }

    public enum MailEditorState {
        STARTED,
        COMPLETED
    }
}
