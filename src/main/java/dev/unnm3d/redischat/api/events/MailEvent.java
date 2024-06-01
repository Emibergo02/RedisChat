package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.mail.Mail;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public abstract class MailEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private Mail mail;

    public MailEvent(Mail mail) {
        super(!Bukkit.isPrimaryThread());
        this.mail = mail;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
