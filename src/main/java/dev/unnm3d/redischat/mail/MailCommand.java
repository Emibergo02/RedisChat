package dev.unnm3d.redischat.mail;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.Permissions;
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
                .executesPlayer((sender, args) -> {
                    mailGUIManager.openPublicMailGui(sender);
                });

    }

    public CommandAPICommand getSendSubCommand() {
        return new CommandAPICommand("send")
                .withPermission(Permissions.MAIL_WRITE.getPermission())
                .withArguments(
                        new StringArgument(mailGUIManager.getPlugin().messages.mailStringPlayer)
                                .replaceSuggestions(ArgumentSuggestions.stringsAsync(getPlayerRecipients())),
                        new GreedyStringArgument(mailGUIManager.getPlugin().messages.mailTitleSuggestion)
                                .replaceSuggestions(ArgumentSuggestions.strings("<aqua>Mail Object/Title</aqua>")))
                .executesPlayer((sender, args) -> {
                    String recipient = (String) args.get(0);
                    assert recipient != null;
                    if (recipient.equals("-Public") && !sender.hasPermission(Permissions.MAIL_WRITE_PUBLIC.getPermission())) {
                        mailGUIManager.getPlugin().getComponentProvider().sendMessage(sender, mailGUIManager.getPlugin().messages.noPermission);
                        return;
                    }

                    mailGUIManager.getPlugin().getWebEditorAPI().startSession("Mail Content", "/rmail webui {token}", "RedisMail")
                            .thenAccept(session -> mailGUIManager.startEditorMode(
                                    sender,
                                    recipient,
                                    (String) args.get(1),
                                    session));
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
