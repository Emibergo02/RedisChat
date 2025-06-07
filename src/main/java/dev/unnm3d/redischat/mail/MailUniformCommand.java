package dev.unnm3d.redischat.mail;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.commands.RedisChatCommand;
import lombok.AllArgsConstructor;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.element.ArgumentElement;
import net.william278.uniform.paper.LegacyPaperCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class MailUniformCommand implements RedisChatCommand {
    private final RedisChat plugin;

    public LegacyPaperCommand getCommand() {
        return LegacyPaperCommand.builder("rmail")
                .setAliases(plugin.config.commandAliases.getOrDefault("rmail", List.of()))
                .addSubCommand(getDeleteSubCommand())
                .addSubCommand(getWebUISubCommand())
                .addSubCommand(getUnreadSubCommand())
                .addSubCommand(getSendSubCommand())
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.getComponentProvider().sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    plugin.getMailGUIManager().openPublicMailGui(player);
                })
                .build();
    }

    public LegacyPaperCommand getSendSubCommand() {
        return LegacyPaperCommand.builder("send")
                .setPermission(Permissions.MAIL_WRITE.getPermission())
                .addArgument(getPlayerRecipientsArgument())
                .addArgument("title", StringArgumentType.greedyString(),
                        (commandContext, builder) -> {
                            if (builder.getRemaining().isEmpty()) {
                                builder.suggest("<aqua>Mail Object/Title</aqua>");
                            }
                            return builder.buildFuture();
                        })
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.getComponentProvider().sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    final String recipient = commandContext.getArgument("username", String.class);
                    final String title = commandContext.getArgument("title", String.class);
                    if (recipient.equals("-Public") && !player.hasPermission(Permissions.MAIL_WRITE_PUBLIC.getPermission())) {
                        plugin.getComponentProvider().sendMessage(player, plugin.messages.noPermission);
                        return;
                    }

                    plugin.getWebEditorAPI().startSession("Mail Content", "/rmail webui {token}", "RedisMail")
                            .thenAccept(session -> plugin.getMailGUIManager().startEditorMode(
                                    player, recipient,
                                    title, session
                            )).exceptionally(throwable -> {
                                throwable.printStackTrace();
                                return null;
                            });
                }, "username", "title").build();
    }

    public LegacyPaperCommand getDeleteSubCommand() {
        return LegacyPaperCommand.builder("delete")
                .setPermission(Permissions.MAIL_DELETE.getPermission())
                .addArgument(LegacyPaperCommand.doubleNum("id"))
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.getComponentProvider().sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    double id = commandContext.getArgument("id", Double.class);

                    plugin.getMailGUIManager().getPrivateMails(player.getName()).thenAccept(mails -> mails.stream()
                            .filter(m -> m.getId() == id)
                            .findFirst()
                            .ifPresentOrElse(mail -> plugin.getMailGUIManager().deleteMail(mail, player),
                                    () -> plugin.getComponentProvider().sendMessage(player, plugin.messages.mailNotFound)
                            ));
                }, "id").build();
    }

    public LegacyPaperCommand getUnreadSubCommand() {
        return LegacyPaperCommand.builder("unread")
                .setPermission(Permissions.MAIL_UNREAD.getPermission())
                .addArgument(LegacyPaperCommand.doubleNum("id"))
                .addArgument(new ArgumentElement<>("type", StringArgumentType.word(),
                        (commandContext, builder) -> {
                            builder.suggest("private").suggest("public");
                            return builder.buildFuture();
                        }))
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.getComponentProvider().sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    double id = commandContext.getArgument("id", Double.class);
                    String type = commandContext.getArgument("type", String.class);

                    final CompletableFuture<List<Mail>> mailList = switch (type) {
                        case "private" -> plugin.getMailGUIManager().getPrivateMails(player.getName());
                        case "public" -> plugin.getMailGUIManager().getPublicMails(player.getName());
                        default -> CompletableFuture.completedFuture(new ArrayList<>());
                    };

                    mailList.thenAccept(mails -> mails.stream()
                            .filter(m -> m.getId() == id)
                            .findFirst()
                            .ifPresentOrElse(
                                    mail -> plugin.getMailGUIManager().readMail(mail, player, false).thenApply(aBoolean -> {
                                        if (aBoolean) {
                                            plugin.messages.sendMessage(player, plugin.messages.mailUnRead.replace("%title%", mail.getTitle()));
                                        } else {
                                            plugin.messages.sendMessage(player, plugin.messages.mailError);
                                        }
                                        return aBoolean;
                                    }),
                                    () -> plugin.getComponentProvider().sendMessage(player, plugin.messages.mailNotFound)
                            ));
                }, "id", "type").build();
    }

    public LegacyPaperCommand getWebUISubCommand() {
        return LegacyPaperCommand.builder("webui")
                .setDescription("RedisMail WebUI Command")
                .addArgument(BaseCommand.word("token"))
                .addArgument(new ArgumentElement<>("action", StringArgumentType.word(),
                        (commandContext, builder) ->
                                builder.suggest("confirm").suggest("abort")
                                        .suggest("preview").buildFuture()))
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.getComponentProvider().sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    String token = commandContext.getArgument("token", String.class);
                    String action = commandContext.getArgument("action", String.class);

                    if (token == null || token.isEmpty()) {
                        plugin.messages.sendMessage(player, plugin.messages.missing_arguments);
                        return;
                    }
                    if (action.isEmpty()) {
                        plugin.getWebEditorAPI().retrieveSession(token)
                                .thenAccept(mailContent -> {
                                    plugin.getMailGUIManager().stopEditorMode(player, mailContent);
                                    plugin.getComponentProvider().sendMessage(player,
                                            plugin.messages.mailEditorConfirm
                                                    .replace("%token%", token)
                                    );
                                }).exceptionally(throwable -> {
                                    throwable.printStackTrace();
                                    return null;
                                });
                        return;
                    }

                    switch (action) {
                        case "confirm" -> plugin.getMailGUIManager().confirmSendMail(player, true);
                        case "abort" -> plugin.getMailGUIManager().confirmSendMail(player, false);
                        case "preview" -> plugin.getMailGUIManager().previewMail(player);
                    }
                }, "token", "action").build();
    }

    public ArgumentElement<CommandSender, String> getPlayerRecipientsArgument() {
        return new ArgumentElement<>("username", StringArgumentType.word(),
                (context, builder) -> {
                    plugin.getPlayerListManager().getPlayerList(context.getSource()).stream()
                            .filter(s -> s.toLowerCase().contains(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                    if (context.getSource().hasPermission(Permissions.MAIL_WRITE_PUBLIC.getPermission())) {
                        builder.suggest("-Public");
                    }
                    return builder.buildFuture();
                });
    }
}
