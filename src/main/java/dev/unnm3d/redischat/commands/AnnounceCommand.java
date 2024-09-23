package dev.unnm3d.redischat.commands;

import com.google.common.base.Strings;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.settings.Config;
import dev.unnm3d.redischat.task.AnnouncerManager;
import dev.unnm3d.redischat.task.AnnouncerTask;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AnnounceCommand {
    private final RedisChat plugin;
    private final AnnouncerManager announcerManager;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("announcer")
                .withAliases(plugin.config.getCommandAliases("announcer"))
                .withPermission(Permissions.ANNOUNCER.getPermission())
                .withArguments(new MultiLiteralArgument("action", "stop", "start"))
                .withArguments(new StringArgument("announcementName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.config.announcer.stream()
                                        .map(Config.Announcement::announcementName)
                                        .filter(announceName -> announceName.startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new))
                        ))
                .executes((sender, args) -> {
                    final String announceName = Strings.nullToEmpty((String) args.get(1));
                    switch (Strings.nullToEmpty((String) args.get(0))) {
                        case "stop" -> {
                            if (announcerManager.cancelAnnounce(announceName) == null) {
                                plugin.messages.sendMessage(sender, plugin.messages.announce_not_found.replace("%name%", announceName));
                                return;
                            }
                            plugin.messages.sendMessage(sender, plugin.messages.action_completed_successfully);
                        }
                        case "start" -> {
                            final AnnouncerTask at = announcerManager.startAnnounce(announceName);
                            if (at == null) {
                                plugin.messages.sendMessage(sender, plugin.messages.announce_not_found.replace("%name%", announceName));
                                return;
                            }
                            plugin.messages.sendMessage(sender, plugin.messages.action_completed_successfully);
                            at.run();
                        }
                        default -> plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                    }
                });
    }
}
