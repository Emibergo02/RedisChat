package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class MailReadStatusChangeEvent extends MailEvent {
    private final Player player;

    public MailReadStatusChangeEvent(Mail mail, Player player) {
        super(mail);
        this.player = player;
    }
}
