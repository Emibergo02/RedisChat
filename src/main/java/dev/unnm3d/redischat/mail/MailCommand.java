package dev.unnm3d.redischat.mail;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.*;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
public class MailCommand {

    private MailGUIManager mailGUIManager;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("rmail")
                .withPermission(Permissions.MAIL_READ.getPermission())
                .withAliases(mailGUIManager.getPlugin().config.getCommandAliases("rmail"))
                .withSubcommand(getSendSubCommand())
                .withSubcommand(getWebUISubCommand())
                .withSubcommand(getDeleteCommand())
                .withSubcommand(getUnreadCommand())
                .executesPlayer((sender, args) -> {
                    mailGUIManager.openPublicMailGui(sender);
                });

    }

    public CommandAPICommand getSendSubCommand() {
        return new CommandAPICommand("send")
                .withPermission(Permissions.MAIL_WRITE.getPermission())
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions(ArgumentSuggestions.stringsAsync(getPlayerRecipients())),
                        new GreedyStringArgument("title")
                                .replaceSuggestions(ArgumentSuggestions.strings("<aqua>Mail Object/Title</aqua>")))
                .executesPlayer((sender, args) -> {
                    String recipient = (String) args.get(0);
                    if (recipient.equals("-Public") && !sender.hasPermission(Permissions.MAIL_WRITE_PUBLIC.getPermission())) {
                        mailGUIManager.getPlugin().getComponentProvider().sendMessage(sender, mailGUIManager.getPlugin().messages.noPermission);
                        return;
                    }

                    mailGUIManager.getPlugin().getWebEditorAPI().startSession("Mail Content", "/rmail webui {token}", "RedisMail")
                            .thenAccept(session -> mailGUIManager.startEditorMode(
                                    sender, recipient,
                                    (String) args.get(1), session
                            )).exceptionally(throwable -> {
                                throwable.printStackTrace();
                                return null;
                            });
                });
    }

    public CommandAPICommand getDeleteCommand() {
        return new CommandAPICommand("delete")
                .withPermission(Permissions.MAIL_DELETE.getPermission())
                .withArguments(new DoubleArgument("id"))
                .executesPlayer((sender, args) -> {
                    if (args.count() == 0) return;

                    double id = (Double) args.get(0);

                    mailGUIManager.getPrivateMails(sender.getName()).thenAccept(mails -> mails.stream()
                            .filter(m -> m.getId() == id)
                            .findFirst()
                            .ifPresentOrElse(mail -> mailGUIManager.deleteMail(mail, sender),
                                    () -> mailGUIManager.getPlugin().getComponentProvider().sendMessage(sender, mailGUIManager.getPlugin().messages.mailNotFound)
                            ));
                });
    }

    public CommandAPICommand getUnreadCommand() {
        return new CommandAPICommand("unread")
                .withPermission(Permissions.MAIL_UNREAD.getPermission())
                .withArguments(new DoubleArgument("id"))
                .withArguments(new MultiLiteralArgument("type", "private", "public"))
                .executesPlayer((sender, args) -> {
                    if (args.count() < 2) return;

                    double id = (Double) args.get(0);
                    assert args.get(1) != null;
                    final CompletableFuture<List<Mail>> mailList = args.get(1).equals("private") ?
                            mailGUIManager.getPrivateMails(sender.getName()) :
                            mailGUIManager.getPublicMails(sender.getName());

                    mailList.thenAccept(mails -> mails.stream()
                            .filter(m -> m.getId() == id)
                            .findFirst()
                            .ifPresentOrElse(
                                    mail -> mailGUIManager.readMail(mail, sender, false).thenApply(aBoolean -> {
                                        if (aBoolean) {
                                            RedisChat.getInstance().messages.sendMessage(sender, RedisChat.getInstance().messages.mailUnRead.replace("%title%", mail.getTitle()));
                                        } else {
                                            RedisChat.getInstance().messages.sendMessage(sender, RedisChat.getInstance().messages.mailError);
                                        }
                                        return aBoolean;
                                    }),
                                    () -> mailGUIManager.getPlugin().getComponentProvider().sendMessage(sender, mailGUIManager.getPlugin().messages.mailNotFound)
                            ));
                });
    }

    public CommandAPICommand getWebUISubCommand() {
        return new CommandAPICommand("webui")
                .withArguments(new StringArgument("token"))
                .withOptionalArguments(new MultiLiteralArgument("action", "confirm", "abort", "preview"))
                .executesPlayer((sender, args) -> {
                    String token = (String) args.get(0);
                    Optional<Object> action = args.getOptional(1);

                    if (token == null) {
                        mailGUIManager.getPlugin().messages.sendMessage(sender, mailGUIManager.getPlugin().messages.missing_arguments);
                        return;
                    }
                    if (action.isEmpty()) {
                        mailGUIManager.getPlugin().getWebEditorAPI().retrieveSession(token)
                                .thenAccept(mailContent -> {
                                            mailGUIManager.stopEditorMode(sender, mailContent);
                                            mailGUIManager.getPlugin().getComponentProvider().sendMessage(sender,
                                                    mailGUIManager.getPlugin().messages.mailEditorConfirm
                                                            .replace("%token%", token)
                                            );
                                        }
                                ).exceptionally(throwable -> {
                                    throwable.printStackTrace();
                                    return null;
                                });
                        return;
                    }

                    switch ((String) action.get()) {
                        case "confirm" -> mailGUIManager.confirmSendMail(sender, true);
                        case "abort" -> mailGUIManager.confirmSendMail(sender, false);
                        case "preview" -> mailGUIManager.previewMail(sender);
                    }

                });
    }

    public Function<SuggestionInfo<CommandSender>, CompletableFuture<String[]>> getPlayerRecipients() {
        return commandSenderSuggestionInfo ->
                CompletableFuture.supplyAsync(() -> {
                    List<String> list = new ArrayList<>(
                            mailGUIManager.getPlugin().getPlayerListManager().getPlayerList(commandSenderSuggestionInfo.sender()).stream()
                                    .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg().toLowerCase()))
                                    .toList()
                    );
                    if (commandSenderSuggestionInfo.sender().hasPermission(Permissions.MAIL_WRITE_PUBLIC.getPermission()))
                        list.add("-Public");
                    return list.toArray(new String[0]);
                }, mailGUIManager.getPlugin().getExecutorService());
    }

}
