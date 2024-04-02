package dev.unnm3d.redischat.channels;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class ChannelCommand {
    private final RedisChat plugin;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("channel")
                .withAliases(plugin.config.getCommandAliases("channel"))
                .withSubcommand(getCreateSubCommand())
                .withSubcommand(getSetFormatSubCommand())
                .withSubcommand(getDeleteSubCommand())
                .withSubcommand(getEnableSubCommand())
                .withSubcommand(getListenSubCommand())
                .withSubcommand(getDisableSubCommand())
                .withSubcommand(getListSubCommand())
                .withSubcommand(getDiscordLinkSubCommand())
                .executesPlayer((sender, args) -> {
                    try {
                        plugin.getChannelManager().openChannelsGUI(sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public CommandAPICommand getCreateSubCommand() {
        return new CommandAPICommand("create")
                .withPermission(Permissions.CHANNEL_CREATE.getPermission())
                .withArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .withArguments(new IntegerArgument("rate-limit"))
                .withArguments(new IntegerArgument("rate-limit-period"))
                .withArguments(new BooleanArgument("filtered"))
                .withOptionalArguments(new IntegerArgument("proximity-distance")
                        .replaceSuggestions(ArgumentSuggestions.strings("-1", "100")))
                .withOptionalArguments(new TextArgument("discord-webhook")
                        .replaceSuggestions(ArgumentSuggestions.strings("\"https://discord.com/api/webhooks/...\"")))
                .executesPlayer((sender, args) -> {
                    Optional<Object> discordWebhook = args.getOptional("discord-webhook");
                    Optional<Object> proximityDistance = args.getOptional("proximity-distance");
                    if (args.count() < 4) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }

                    int rateLimit = (int) args.get(1);
                    int rateLimitPeriod = (int) args.get(2);
                    boolean filtered = (boolean) args.get(3);

                    plugin.getChannelManager().registerChannel(new Channel(
                            (String) args.get(0),
                            "No format, use /channel setformat <name> <format> -> %message%",
                            rateLimit,
                            rateLimitPeriod,
                            proximityDistance.map(o -> (int) o).orElse(-1),
                            discordWebhook.map(o -> (String) o).orElse(""),
                            filtered,
                            null
                    ));

                    plugin.messages.sendMessage(sender, plugin.messages.channelCreated);
                });
    }

    public CommandAPICommand getDiscordLinkSubCommand() {
        return new CommandAPICommand("link-discord-webhook")
                .withPermission(Permissions.CHANNEL_CREATE.getPermission())
                .withArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .withArguments(new GreedyStringArgument("discord-webhook")
                        .replaceSuggestions(ArgumentSuggestions.strings("https://discord.com/api/webhooks/...")))
                .executes((sender, args) -> {
                    if (args.count() < 2) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }

                    plugin.getChannelManager().getChannel((String) args.get(0)).ifPresentOrElse(channel -> {
                        channel.setDiscordWebhook((String) args.get(1));
                        plugin.getChannelManager().registerChannel(channel);
                        plugin.messages.sendMessage(sender, plugin.messages.channelCreated);
                    }, () -> plugin.messages.sendMessage(sender, plugin.messages.channelNotFound));
                });
    }

    public CommandAPICommand getSetFormatSubCommand() {
        return new CommandAPICommand("setformat")
                .withPermission(Permissions.CHANNEL_CREATE.getPermission())
                .withArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .withArguments(new GreedyStringArgument("format"))
                .executesPlayer((sender, args) -> {
                    Channel channel = plugin.getChannelManager().getRegisteredChannels().get((String) args.get(0));
                    channel.setFormat((String) args.get(1));
                    plugin.getChannelManager().registerChannel(channel);
                    plugin.messages.sendMessage(sender, plugin.messages.channelCreated);
                });
    }

    public CommandAPICommand getEnableSubCommand() {
        return new CommandAPICommand("enable")
                .withPermission(Permissions.CHANNEL_TOGGLE_PLAYER.getPermission())
                .withArguments(new StringArgument("playerName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getPlayerListManager().getPlayerList(commandSenderSuggestionInfo.sender()).stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg().toLowerCase()))
                                        .toArray(String[]::new))))
                .withArguments(new StringArgument("channelName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((sender, args) -> {
                    if (args.count() < 2) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }

                    plugin.getDataManager().setPlayerChannelStatuses((String) args.get(0), Map.of((String) args.get(1), "0"));
                    plugin.messages.sendMessage(sender, plugin.messages.channelEnabled
                            .replace("%channel%", (String) args.get(1))
                            .replace("%player%", (String) args.get(0))
                    );
                });
    }

    public CommandAPICommand getListenSubCommand() {
        return new CommandAPICommand("force-listen")
                .withPermission(Permissions.CHANNEL_TOGGLE_PLAYER.getPermission())
                .withArguments(new StringArgument("playerName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getPlayerListManager().getPlayerList(commandSenderSuggestionInfo.sender()).stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg().toLowerCase()))
                                        .toArray(String[]::new))))
                .withArguments(new StringArgument("channelName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((sender, args) -> {
                    if (args.count() < 2) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    plugin.getChannelManager().setActiveChannel((String) args.get(0), (String) args.get(1));
                    plugin.messages.sendMessage(sender, plugin.messages.channelForceListen
                            .replace("%channel%", (String) args.get(1))
                            .replace("%player%", (String) args.get(0))
                    );
                });
    }

    public CommandAPICommand getListSubCommand() {
        return new CommandAPICommand("list")
                .withPermission(Permissions.CHANNEL_LIST.getPermission())
                .executesPlayer((sender, args) -> {
                    plugin.getDataManager().getPlayerChannelStatuses(sender.getName(), plugin.getChannelManager().getRegisteredChannels())
                            .thenAccept(channels -> {
                                plugin.getComponentProvider().sendMessage(sender, plugin.messages.channelListHeader);

                                for (PlayerChannel channel : channels) {
                                    final String channelMsg =
                                            channel.isListening() ?
                                                    plugin.messages.channelListTransmitting :
                                                    channel.isMuted() ?
                                                            plugin.messages.channelListMuted :
                                                            plugin.messages.channelListReceiving;
                                    plugin.getComponentProvider().sendMessage(sender,
                                            channelMsg.replace("%channel%", channel.getChannel().getName())
                                    );
                                }
                            }).exceptionally(throwable -> {
                                throwable.printStackTrace();
                                return null;
                            });
                });
    }

    public CommandAPICommand getDisableSubCommand() {
        return new CommandAPICommand("disable")
                .withPermission(Permissions.CHANNEL_TOGGLE_PLAYER.getPermission())
                .withArguments(new StringArgument("playerName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getPlayerListManager().getPlayerList(commandSenderSuggestionInfo.sender()).stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg().toLowerCase()))
                                        .toArray(String[]::new))))
                .withArguments(new StringArgument("channelName")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((sender, args) -> {
                    if (args.count() < 2) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    plugin.getDataManager().removePlayerChannelStatus((String) args.get(0), (String) args.get(1));
                    plugin.messages.sendMessage(sender, plugin.messages.channelDisabled
                            .replace("%channel%", (String) args.get(1))
                            .replace("%player%", (String) args.get(0))
                    );
                });
    }

    public CommandAPICommand getDeleteSubCommand() {
        return new CommandAPICommand("delete")
                .withPermission(Permissions.CHANNEL_DELETE.getPermission())
                .withArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((sender, args) -> {
                    plugin.getChannelManager().unregisterChannel((String) args.get(0));
                    plugin.messages.sendMessage(sender, plugin.messages.channelRemoved);
                });
    }


}
