package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class MailReadStatusChangeEvent extends MailEvent {
    private final Player player;

    /**
     * Event that is called when a mail is read by a player
     * @param mail The mail that is being read
     * @param player The player that is reading the mail
     */
    public MailReadStatusChangeEvent(Mail mail, Player player) {
        super(mail);
        this.player = player;
    }
}
