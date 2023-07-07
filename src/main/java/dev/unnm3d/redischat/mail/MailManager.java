package dev.unnm3d.redischat.mail;

import dev.unnm3d.redischat.RedisChat;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class MailManager {

    @Getter
    private final RedisChat plugin;
    @Getter
    private List<Mail> publicMailList;
    private final HashMap<UUID, Mail> editorMode;
    @Getter
    private final MailGUI mailGUI;

    public MailManager(RedisChat plugin) {
        this.plugin = plugin;
        this.publicMailList = new ArrayList<>();
        this.editorMode = new HashMap<>();
        this.mailGUI = new MailGUI(plugin);
        plugin.getRedisDataManager().getPublicMails().toCompletableFuture()
                .thenAccept(list -> publicMailList = list);
    }


    public void startEditorMode(Player sender, String target, String title, String token) {
        this.editorMode.put(sender.getUniqueId(), new Mail(sender.getName(), target, title));
        plugin.getComponentProvider().sendMessage(sender,
                plugin.messages.mailEditorStart
                        .replace("%link%", plugin.getWebEditorAPI().getEditorUrl(token))
        );
    }

    public void stopEditorMode(Player sender, String content) {
        editorMode.computeIfPresent(sender.getUniqueId(), (s, mail) -> {
            mail.setContent(content);
            return mail;
        });
    }

    public void confirmSendMail(Player sender, boolean confirm) {
        Mail mail = editorMode.remove(sender.getUniqueId());
        if (mail == null) {
            plugin.getComponentProvider().sendMessage(sender, plugin.messages.mailError);
            return;
        }
        if (!confirm) {
            plugin.getComponentProvider().sendMessage(sender,
                    plugin.messages.mailEditorAbort);
            return;
        }

        sendMail(mail).thenAccept(success ->
                plugin.getComponentProvider().sendMessage(sender,
                        plugin.messages.mailEditorSent));

    }

    /**
     * Send mail to redis
     *
     * @param mail Mail to send
     * @return CompletionStage<Boolean> if mail was sent successfully
     */
    private CompletionStage<Boolean> sendMail(@NotNull Mail mail) {
        if (mail.getCategory().equals(Mail.MailCategory.PUBLIC)) {
            return plugin.getRedisDataManager().setPublicMail(mail);
        }
        return plugin.getRedisDataManager().setPlayerPrivateMail(mail);
    }
}
