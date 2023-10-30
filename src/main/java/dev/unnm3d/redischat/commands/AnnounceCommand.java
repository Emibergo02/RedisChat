package dev.unnm3d.redischat.commands;

import com.google.common.base.Strings;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.settings.Config;
import dev.unnm3d.redischat.task.AnnounceManager;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AnnounceCommand {
    private final RedisChat plugin;
    private final AnnounceManager announceManager;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("announce")
                .withPermission(Permissions.ANNOUNCE.getPermission())
                .withArguments(new MultiLiteralArgument("action", "stop", "start"))
                .withArguments(new StringArgument("announceName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.config.announces.stream()
                                        .map(Config.Announce::announceName)
                                        .filter(announceName -> announceName.startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new))
                        ))
                .executes((sender, args) -> {
                    final String announceName = Strings.nullToEmpty((String) args.get(1));
                    switch (Strings.nullToEmpty((String) args.get(0))) {
                        case "stop" -> {
                            if (announceManager.cancelAnnounce(announceName) == null) {
                                plugin.messages.sendMessage(sender, plugin.messages.announce_not_found.replace("%name%", announceName));
                                return;
                            }
                            plugin.messages.sendMessage(sender, plugin.messages.action_completed_successfully);
                        }
                        case "start" -> {
                            if (announceManager.startAnnounce(announceName) == null) {
                                plugin.messages.sendMessage(sender, plugin.messages.announce_not_found.replace("%name%", announceName));
                                return;
                            }
                            plugin.messages.sendMessage(sender, plugin.messages.action_completed_successfully);
                        }
                        default -> plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                    }
                });
    }
}
