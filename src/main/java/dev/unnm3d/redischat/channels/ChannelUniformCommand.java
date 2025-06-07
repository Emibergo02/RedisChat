package dev.unnm3d.redischat.channels;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.KnownChatEntities;
import dev.unnm3d.redischat.channels.gui.PlayerChannel;
import dev.unnm3d.redischat.commands.RedisChatCommand;
import lombok.AllArgsConstructor;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.element.ArgumentElement;
import net.william278.uniform.paper.LegacyPaperCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class ChannelUniformCommand implements RedisChatCommand {
    private final RedisChat plugin;

    @Override
    public LegacyPaperCommand getCommand() {
        return LegacyPaperCommand.builder("channel")
                .setAliases(plugin.config.commandAliases.getOrDefault("channel", List.of()))
                .addSubCommand(getCreateSubCommand())
                .addSubCommand(getInfoSubCommand())
                .addSubCommand(getSetDisplayNameCommand())
                .addSubCommand(getSetFormatCommand())
                .addSubCommand(getDeleteSubCommand())
                .addSubCommand(getForceListenSubCommand())
                .addSubCommand(getListSubCommand())
                .addSubCommand(getDiscordLinkSubCommand())
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    if (!player.hasPermission(Permissions.CHANNEL_GUI.getPermission())) {
                        plugin.messages.sendMessage(player, plugin.messages.noPermission);
                        return;
                    }
                    try {
                        plugin.getChannelManager().openChannelsGUI(player);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).build();
    }

    public LegacyPaperCommand getCreateSubCommand() {
        return LegacyPaperCommand.builder("create")
                .setPermission(Permissions.CHANNEL_CREATE.getPermission())
                .addArgument(BaseCommand.word("name"))
                .addArgument(BaseCommand.intNum("rate-limit", 0))
                .addArgument(BaseCommand.intNum("rate-limit-period", 0))
                .addArgument(BaseCommand.bool("filtered"))
                .addArgument(BaseCommand.intNum("proximity-distance", -1, 100))
                .addArgument(BaseCommand.string("discord-webhook"))
                .addArgument(BaseCommand.bool("shown-by-default"))
                .addArgument(BaseCommand.bool("permission-required"))
                .execute(commandContext -> {
                    final String channelName = commandContext.getArgument("name", String.class);
                    final Integer rateLimit = commandContext.getArgument("rate-limit", Integer.class);
                    final Integer rateLimitPeriod = commandContext.getArgument("rate-limit-period", Integer.class);
                    final Boolean filtered = commandContext.getArgument("filtered", Boolean.class);
                    Optional<Integer> proximityDistance = Optional.ofNullable(commandContext.getArgument("proximity-distance", Integer.class));
                    Optional<String> discordWebhook = Optional.ofNullable(commandContext.getArgument("discord-webhook", String.class));
                    Optional<Boolean> shownByDefault = Optional.ofNullable(commandContext.getArgument("shown-by-default", Boolean.class));
                    Optional<Boolean> needsPermission = Optional.ofNullable(commandContext.getArgument("permission-required", Boolean.class));

                    if (channelName == null || channelName.isEmpty() || rateLimit == null || rateLimitPeriod == null || filtered == null) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }

                    plugin.getChannelManager().registerChannel(Channel.builder(channelName)
                            .rateLimit(rateLimit)
                            .rateLimitPeriod(rateLimitPeriod)
                            .format(plugin.messages.channelNoFormat.replace("%channel%", channelName))
                            .proximityDistance(proximityDistance.orElse(-1))
                            .discordWebhook(discordWebhook.orElse(""))
                            .filtered(filtered)
                            .shownByDefault(shownByDefault.orElse(true))
                            .permissionEnabled(needsPermission.orElse(true))
                            .build()
                    );

                    plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelCreated);
                }).build();
    }

    public LegacyPaperCommand getInfoSubCommand() {
        return LegacyPaperCommand.builder("info")
                .setPermission(Permissions.CHANNEL_INFO.getPermission())
                .addArgument(getChannelNameArgument())
                .execute(commandContext -> {
                    final String channelName = commandContext.getArgument("channel_name", String.class);
                    if (channelName == null || channelName.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    plugin.getChannelManager().getChannel(channelName, commandContext.getSource()).ifPresentOrElse(channel -> {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelInfo
                                .replace("%channel%", channel.getName())
                                .replace("%displayname%", channel.getDisplayName())
                                .replace("%rate_limit%", String.valueOf(channel.getRateLimit()))
                                .replace("%rate_limit_period%", String.valueOf(channel.getRateLimitPeriod()))
                                .replace("%proximity_distance%", String.valueOf(channel.getProximityDistance()))
                                .replace("%discord_webhook%", channel.getDiscordWebhook())
                                .replace("%shown_by_default%", String.valueOf(channel.isShownByDefault()))
                                .replace("%permission_required%", String.valueOf(channel.isPermissionEnabled()))
                        );
                    }, () -> plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelNotFound));

                }, "channel_name").build();
    }

    public LegacyPaperCommand getSetDisplayNameCommand() {
        return LegacyPaperCommand.builder("setdisplayname")
                .setPermission(Permissions.CHANNEL_CHANGE_DISPLAYNAME.getPermission())
                .addArgument(getChannelNameArgument())
                .addArgument(BaseCommand.greedyString("displayname"))
                .execute(commandContext -> {
                    final String channelName = commandContext.getArgument("channel_name", String.class);
                    final String displayName = commandContext.getArgument("displayname", String.class);
                    if (channelName == null || channelName.isEmpty() || displayName == null || displayName.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    Channel channel = plugin.getChannelManager().getRegisteredChannels().get(channelName);
                    if (channel == null) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelNotFound);
                        return;
                    }
                    channel.setDisplayName(displayName);
                    plugin.getChannelManager().registerChannel(channel);
                    plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelChangedDisplayName
                            .replace("%displayname%", displayName));

                }, "channel_name", "displayname").build();
    }

    public LegacyPaperCommand getSetFormatCommand() {
        return LegacyPaperCommand.builder("setformat")
                .setPermission(Permissions.CHANNEL_CREATE.getPermission())
                .addArgument(getChannelNameArgument())
                .addArgument(BaseCommand.greedyString("format"))
                .execute(commandContext -> {
                    final String channelName = commandContext.getArgument("channel_name", String.class);
                    final String format = commandContext.getArgument("format", String.class);
                    if (channelName == null || channelName.isEmpty() || format == null || format.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    Channel channel = plugin.getChannelManager().getRegisteredChannels().get(channelName);
                    if (channel == null) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelNotFound);
                        return;
                    }
                    channel.setFormat(format);
                    plugin.getChannelManager().registerChannel(channel);
                    plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelCreated);
                }, "channel_name", "format").build();
    }

    public LegacyPaperCommand getDeleteSubCommand() {
        return LegacyPaperCommand.builder("delete")
                .setPermission(Permissions.CHANNEL_DELETE.getPermission())
                .addArgument(getChannelNameArgument())
                .execute(commandContext -> {
                    final String channelName = commandContext.getArgument("channel_name", String.class);
                    if (channelName == null || channelName.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    plugin.getChannelManager().unregisterChannel(channelName);
                    plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelRemoved);
                }, "channel_name").build();
    }

    public LegacyPaperCommand getForceListenSubCommand() {
        return LegacyPaperCommand.builder("force-listen")
                .setPermission(Permissions.CHANNEL_TOGGLE_PLAYER.getPermission())
                .addArgument(new ArgumentElement<>("username", StringArgumentType.word(), (context, builder) -> {
                    plugin.getPlayerListManager().getPlayerList(context.getSource()).stream()
                            .filter(s -> s.toLowerCase().contains(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                }))
                .addArgument(getChannelNameArgument())
                .execute(commandContext -> {
                    final String playerName = commandContext.getArgument("username", String.class);
                    final String channelName = commandContext.getArgument("channel_name", String.class);
                    if (playerName == null || playerName.isEmpty() || channelName == null || channelName.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    plugin.getChannelManager().setActiveChannel(playerName, channelName);
                    plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelForceListen
                            .replace("%channel%", channelName)
                            .replace("%player%", playerName));
                }, "username", "channel_name").build();
    }

    public LegacyPaperCommand getListSubCommand() {
        return LegacyPaperCommand.builder("list")
                .setPermission(Permissions.CHANNEL_LIST.getPermission())
                .execute(commandContext -> {
                    if (!(commandContext.getSource() instanceof Player player)) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.noConsole);
                        return;
                    }
                    plugin.messages.sendMessage(player, plugin.messages.channelListHeader);
                    final String activeChannelName = plugin.getChannelManager().getActiveChannel(player.getName());

                    final List<PlayerChannel> availableChannels = plugin.getChannelManager().getRegisteredChannels().values().stream()
                            .filter(channel -> channel.isShownByDefault() || player.hasPermission(Permissions.CHANNEL_SHOW_PREFIX.getPermission() + channel.getName()))
                            .map(channel -> new PlayerChannel(channel, player, channel.getName().equals(activeChannelName)))
                            .filter(playerChannel -> !playerChannel.isHidden())
                            .toList();

                    for (PlayerChannel channel : availableChannels) {
                        final String channelMsg =
                                channel.isListening() ?
                                        plugin.messages.channelListTransmitting :
                                        channel.isMuted() ?
                                                plugin.messages.channelListMuted :
                                                plugin.messages.channelListReceiving;
                        plugin.getComponentProvider().sendMessage(commandContext.getSource(),
                                channelMsg.replace("%channel%", channel.getChannel().getName())
                        );
                    }
                }).build();
    }

    public ArgumentElement<CommandSender, String> getChannelNameArgument() {
        return new ArgumentElement<>("channel_name", StringArgumentType.word(), (context, builder) -> {
            plugin.getChannelManager().getRegisteredChannels().keySet().stream()
                    .filter(s -> s.toLowerCase().contains(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    public LegacyPaperCommand getDiscordLinkSubCommand() {
        return LegacyPaperCommand.builder("link-discord-webhook")
                .setPermission(Permissions.CHANNEL_CREATE.getPermission())
                .addArgument(getChannelNameArgument())
                .addArgument(BaseCommand.greedyString("discord_webhook"))
                .execute(commandContext -> {
                    final String channelName = commandContext.getArgument("channel_name", String.class);
                    if (channelName == null || channelName.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    final String discordWebhook = commandContext.getArgument("discord_webhook", String.class);
                    if (discordWebhook == null || discordWebhook.isEmpty()) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.missing_arguments);
                        return;
                    }
                    if (channelName.equals(KnownChatEntities.GENERAL_CHANNEL.toString())) {
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.configuration_parameter.replace("%parameter%", "discord-webhook"));
                        return;
                    }

                    plugin.getChannelManager().getRegisteredChannel(channelName).ifPresentOrElse(channel -> {
                        channel.setDiscordWebhook(discordWebhook);
                        plugin.getChannelManager().registerChannel(channel);
                        plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelCreated);
                    }, () -> plugin.messages.sendMessage(commandContext.getSource(), plugin.messages.channelNotFound));
                }, "channel_name", "discord_webhook").build();
    }


}
