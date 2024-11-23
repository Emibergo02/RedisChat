package dev.unnm3d.redischat.channels;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.gui.PlayerChannel;
import dev.unnm3d.redischat.api.objects.KnownChatEntities;
import dev.unnm3d.redischat.api.objects.Channel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class ChannelCommand {
    private final RedisChat plugin;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("channel")
                .withAliases(plugin.config.getCommandAliases("channel"))
                .withSubcommand(getCreateSubCommand())
                .withSubcommand(getInfoSubCommand())
                .withSubcommand(getSetDisplayNameCommand())
                .withSubcommand(getSetFormatSubCommand())
                .withSubcommand(getDeleteSubCommand())
                .withSubcommand(getListenSubCommand())
                .withSubcommand(getListSubCommand())
                .withSubcommand(getDiscordLinkSubCommand())
                .executesPlayer((sender, args) -> {
                    if(!sender.hasPermission(Permissions.CHANNEL_GUI.getPermission())){
                        plugin.messages.sendMessage(sender, plugin.messages.noPermission);
                        return;
                    }
                    try {
                        plugin.getChannelManager().openChannelsGUI(sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private CommandAPICommand getInfoSubCommand() {
        return new CommandAPICommand("info")
                .withPermission(Permissions.CHANNEL_INFO.getPermission())
                .withArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .executes((sender, args) -> {
                    final String channelName = (String) args.get(0);
                    if (channelName == null) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    plugin.getChannelManager().getChannel(channelName,null).ifPresentOrElse(channel -> {
                        plugin.messages.sendMessage(sender, plugin.messages.channelInfo
                                .replace("%channel%", channel.getName())
                                .replace("%displayname%", channel.getDisplayName())
                                .replace("%rate_limit%", String.valueOf(channel.getRateLimit()))
                                .replace("%rate_limit_period%", String.valueOf(channel.getRateLimitPeriod()))
                                .replace("%proximity_distance%", String.valueOf(channel.getProximityDistance()))
                                .replace("%discord_webhook%", channel.getDiscordWebhook())
                                .replace("%shown_by_default%", String.valueOf(channel.isShownByDefault()))
                                .replace("%permission_required%", String.valueOf(channel.isPermissionEnabled()))
                        );
                    }, () -> plugin.messages.sendMessage(sender, plugin.messages.channelNotFound));
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
                .withOptionalArguments(new BooleanArgument("shown-by-default"))
                .withOptionalArguments(new BooleanArgument("permission-required"))
                .executesPlayer((sender, args) -> {
                    Optional<Object> proximityDistance = args.getOptional("proximity-distance");
                    Optional<Object> discordWebhook = args.getOptional("discord-webhook");
                    Optional<Object> shownByDefault = args.getOptional("shown-by-default");
                    Optional<Object> needsPermission = args.getOptional("permission-required");

                    if (args.count() < 4) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }

                    final String channelName = (String) args.get(0);
                    int rateLimit = (int) args.get(1);
                    int rateLimitPeriod = (int) args.get(2);
                    boolean filtered = (boolean) args.get(3);

                    if (channelName == null) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }

                    plugin.getChannelManager().registerChannel(Channel.builder(channelName)
                            .rateLimit(rateLimit)
                            .rateLimitPeriod(rateLimitPeriod)
                            .format(plugin.messages.channelNoFormat.replace("%channel%", channelName))
                            .proximityDistance(proximityDistance.map(o -> (int) o).orElse(-1))
                            .discordWebhook(discordWebhook.map(o -> (String) o).orElse(""))
                            .filtered(filtered)
                            .shownByDefault(shownByDefault.map(o -> (boolean) o).orElse(true))
                            .permissionEnabled(needsPermission.map(o -> (boolean) o).orElse(true))
                            .build()
                    );

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
                    final String channelName = (String) args.get(0);
                    if (channelName == null) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    if (channelName.equals(KnownChatEntities.GENERAL_CHANNEL.toString())) {
                        plugin.messages.sendMessage(sender, plugin.messages.configuration_parameter.replace("%parameter%", "discord-webhook"));
                        return;
                    }

                    plugin.getChannelManager().getRegisteredChannel((String) args.get(0)).ifPresentOrElse(channel -> {
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

    public CommandAPICommand getSetDisplayNameCommand() {
        return new CommandAPICommand("setdisplayname")
                .withPermission(Permissions.CHANNEL_CHANGE_DISPLAYNAME.getPermission())
                .withArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(commandSenderSuggestionInfo ->
                                plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                                        .filter(s -> s.toLowerCase().startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toArray(String[]::new)
                        )))
                .withArguments(new GreedyStringArgument("displayname"))
                .executesPlayer((sender, args) -> {
                    Channel channel = plugin.getChannelManager().getRegisteredChannels().get((String) args.get(0));
                    channel.setDisplayName((String) args.get(1));
                    plugin.getChannelManager().registerChannel(channel);
                    plugin.messages.sendMessage(sender, plugin.messages.channelChangedDisplayName.replace("%displayname%", (String) args.get(1)));
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
                    plugin.getComponentProvider().sendMessage(sender, plugin.messages.channelListHeader);
                    final String activeChannelName = plugin.getChannelManager().getActiveChannel(sender.getName());

                    final List<PlayerChannel> availableChannels = plugin.getChannelManager().getRegisteredChannels().values().stream()
                            .filter(channel -> channel.isShownByDefault() || sender.hasPermission(Permissions.CHANNEL_SHOW_PREFIX.getPermission() + channel.getName()))
                            .map(channel -> new PlayerChannel(channel, sender, channel.getName().equals(activeChannelName)))
                            .filter(playerChannel -> !playerChannel.isHidden())
                            .toList();

                    for (PlayerChannel channel : availableChannels) {
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
