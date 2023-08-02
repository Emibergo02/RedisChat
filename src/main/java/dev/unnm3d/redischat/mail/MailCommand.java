package dev.unnm3d.redischat.mail;

import dev.unnm3d.redischat.Permission;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class MailCommand implements CommandExecutor, TabCompleter {

    private MailManager mailManager;

    /**
     * /mail send <player>
     * /mail delete
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if the command was handled correctly. Returning false, or not
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        if (!sender.hasPermission(Permission.REDIS_CHAT_MAIL_READ.getPermission())) {
            mailManager.getPlugin().getComponentProvider().sendMessage(sender, mailManager.getPlugin().messages.noPermission);
            return true;
        }
        if (args.length == 0) {
            mailManager.getMailGUI().openPublicMailGui((Player) sender);
            return true;
        }


        if (!sender.hasPermission(Permission.REDIS_CHAT_MAIL_WRITE.getPermission())) {
            mailManager.getPlugin().getComponentProvider().sendMessage(sender, mailManager.getPlugin().messages.noPermission);
            return true;
        }
        if (args[0].equalsIgnoreCase("send") && args.length >= 3) {
            mailManager.getPlugin().getWebEditorAPI().startSession("Mail Content", "/rmail webui {token}", "RedisMail")
                    .thenAccept(session -> mailManager.startEditorMode(
                            (Player) sender,
                            args[1],
                            String.join(" ", Arrays.copyOfRange(args, 2, args.length)),
                            session));

            return true;
        } else if (args[0].equalsIgnoreCase("webui") && args.length > 1) {
            if (args.length == 3) {
                if (args[2].equalsIgnoreCase("confirm")) {
                    mailManager.confirmSendMail((Player) sender, true);
                } else if (args[2].equalsIgnoreCase("abort")) {
                    mailManager.confirmSendMail((Player) sender, false);
                } else if (args[2].equalsIgnoreCase("preview")) {
                    mailManager.previewMail((Player) sender);
                }
                return true;
            }
            mailManager.getPlugin().getWebEditorAPI().retrieveSession(args[1])
                    .thenAccept(mailContent -> {
                                mailManager.stopEditorMode((Player) sender, mailContent);
                                mailManager.getPlugin().getComponentProvider().sendMessage(sender,
                                        mailManager.getPlugin().messages.mailEditorConfirm
                                                .replace("%token%", args[1])
                                );
                            }
                    );
            return true;
        }

        mailManager.getPlugin().getComponentProvider().sendMessage(sender, mailManager.getPlugin().messages.missing_arguments);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permission.REDIS_CHAT_MAIL_WRITE.getPermission())) return List.of();
        if (args.length == 1) return List.of("send", "delete");
        if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
            ArrayList<String> list = new ArrayList<>();
            list.add("*public");
            list.addAll(mailManager.getPlugin().getPlayerListManager().getPlayerList().stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1])).toList());
            return list;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("send")) return List.of("<aqua>Mail Object/Title</aqua>");
        return List.of();
    }
}
