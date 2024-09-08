package dev.unnm3d.redischat.moderation;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.KnownChatEntities;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MuteCommand {

    private RedisChat plugin;


    public CommandAPICommand getMuteCommand() {
        return new CommandAPICommand("rmutechat")
                .withPermission(Permissions.CHANNEL_MUTE.getPermission())
                .withAliases(plugin.config.getCommandAliases("rmutechat"))
                .withArguments(getPlayerArgument())
                .withOptionalArguments(getChannelArgument())
                .executes((sender, args) -> {
                    final String playerName = (String) args.get("player");
                    if (playerName == null) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    final String channelName = (String) args.getOptional("channel").orElse(KnownChatEntities.GENERAL_CHANNEL.toString());

                    plugin.getChannelManager().getMuteManager().toggleMuteOnChannel(playerName, channelName, true);
                    plugin.messages.sendMessage(sender, plugin.messages.muted_player
                            .replace("%player%", playerName)
                            .replace("%channel%", channelName)
                    );
                });
    }

    public CommandAPICommand getUnMuteCommand() {
        return new CommandAPICommand("runmutechat")
                .withPermission(Permissions.CHANNEL_MUTE.getPermission())
                .withAliases(plugin.config.getCommandAliases("runmutechat"))
                .withArguments(getPlayerArgument())
                .withOptionalArguments(getChannelArgument())
                .executes((sender, args) -> {
                    final String playerName = (String) args.get(0);
                    if (playerName == null) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    String channelName = (String) args.getOptional("channel").orElse(KnownChatEntities.GENERAL_CHANNEL.toString());

                    plugin.getChannelManager().getMuteManager().toggleMuteOnChannel(playerName, channelName, false);
                    plugin.messages.sendMessage(sender, plugin.messages.unmuted_player
                            .replace("%player%", playerName)
                            .replace("%channel%", channelName)
                    );
                });
    }

    private Argument<String> getPlayerArgument() {
        return new StringArgument("player")
                .replaceSuggestions(ArgumentSuggestions.stringCollection(functi1 -> {
                    final List<String> suggestions = plugin.getPlayerListManager().getPlayerList(functi1.sender())
                            .stream().filter(s -> s.toLowerCase().startsWith(functi1.currentArg().toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                    suggestions.add(KnownChatEntities.ALL_PLAYERS.toString());
                    return suggestions;
                }));
    }

    private Argument<String> getChannelArgument() {
        return new StringArgument("channel")
                .replaceSuggestions(ArgumentSuggestions.stringCollection(functi1 -> {
                    final List<String> suggestions = plugin.getChannelManager().getRegisteredChannels().keySet()
                            .stream().filter(s -> s.toLowerCase().startsWith(functi1.currentArg().toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                    suggestions.add(KnownChatEntities.GENERAL_CHANNEL.toString());
                    return suggestions;
                }));
    }
}
