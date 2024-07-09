package dev.unnm3d.redischat.channels;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.RedisChatAPI;
import dev.unnm3d.redischat.api.VanishIntegration;
import dev.unnm3d.redischat.api.events.AsyncRedisChatMessageEvent;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterManager;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.ChannelAudience;
import dev.unnm3d.redischat.chat.objects.Channel;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.mail.MailGUIManager;
import dev.unnm3d.redischat.moderation.MuteManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager extends RedisChatAPI {

    private final RedisChat plugin;
    @Getter
    private final ConcurrentHashMap<String, Channel> registeredChannels;
    @Getter
    private final MuteManager muteManager;
    @Getter
    private final FilterManager filterManager;
    @Getter
    private final ChannelGUI channelGUI;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();


    public ChannelManager(RedisChat plugin) {
        INSTANCE = this;
        this.plugin = plugin;
        this.registeredChannels = new ConcurrentHashMap<>();
        this.muteManager = new MuteManager(plugin);
        this.filterManager = new FilterManager(plugin);
        this.channelGUI = new ChannelGUI(plugin);

        updateChannels();
    }

    public void updateChannels() {
        plugin.getDataManager().getChannels()
                .thenAccept(channels -> {
                    registeredChannels.clear();
                    if (plugin.config.debug)
                        channels.forEach(ch -> plugin.getLogger().info("Channel: " + ch.getName()));
                    channels.forEach(channel -> registeredChannels.put(channel.getName(), channel));
                });
    }

    @Override
    public ComponentProvider getComponentProvider() {
        return plugin.getComponentProvider();
    }

    @Override
    public Optional<MailGUIManager> getMailManager() {
        return Optional.ofNullable(plugin.getMailGUIManager());
    }

    @Override
    public void registerChannel(Channel channel) {
        registeredChannels.put(channel.getName(), channel);
        plugin.getDataManager().registerChannel(channel);
    }

    public void updateChannel(String channelName, @Nullable Channel channel) {
        if (channel == null) {
            registeredChannels.remove(channelName);
            return;
        }
        registeredChannels.put(channelName, channel);
    }

    @Override
    public void unregisterChannel(String channelName) {
        registeredChannels.remove(channelName);
        plugin.getDataManager().unregisterChannel(channelName);
    }

    @Override
    public void openChannelsGUI(Player player) {
        plugin.getDataManager().getPlayerChannelStatuses(player.getName(), registeredChannels)
                .thenAccept(playerChannelInfo ->
                        RedisChat.getScheduler().runTask(() ->
                                Window.single()
                                        .setTitle("Channels")
                                        .setGui(channelGUI.getChannelsGUI(player, playerChannelInfo))
                                        .setCloseHandlers(List.of(() -> new UniversalRunnable() {
                                            @Override
                                            public void run() {
                                                player.updateInventory();
                                            }
                                        }.runTaskLater(plugin, 1)))
                                        .open(player)));
    }


    /**
     * Send a message to a specific ChannelAudience
     *
     * @param player   The player that is sending the message
     * @param receiver The receiver audience of the message
     * @param message  The message to be sent
     */
    public void outgoingMessage(CommandSender player, ChannelAudience receiver, @NotNull String message) {
        //Get channel or public channel by default
        Channel currentChannel = getPublicChannel(player);

        if (receiver.getType() == AudienceType.CHANNEL) {
            if (receiver.getName().equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString())) {
                message = message.substring(1);
                currentChannel = getStaffChatChannel();
            } else {
                currentChannel = plugin.getChannelManager().getChannel(receiver.getName()).orElse(currentChannel);
            }
        }


        ChatMessage chatMessage = new ChatMessage(
                new ChannelAudience(player.getName(), AudienceType.PLAYER),
                currentChannel.getFormat(),
                message,
                receiver
        );

        //Filter and send filter message if present
        final FilterResult result = filterManager.filterMessage(player, chatMessage, AbstractFilter.Direction.OUTGOING);
        chatMessage = result.message();

        if (result.filtered()) {
            result.filteredReason().ifPresent(component ->
                    getComponentProvider().sendComponentOrCache(player, component));
            return;
        }

        final Component formatComponent = getComponentProvider().parse(player, chatMessage.getFormat(),
                true, false, false);

        //Parse to MiniMessage component (placeholders, tags and mentions)
        final Component contentComponent = getComponentProvider().parse(player, chatMessage.getContent(),
                true, true, true,
                getComponentProvider().getRedisChatTagResolver(player));


        //Call event and check cancellation
        final AsyncRedisChatMessageEvent event = new AsyncRedisChatMessageEvent(player,
                result.message().getReceiver(),
                formatComponent,
                contentComponent);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        //Reserialize components
        chatMessage.setFormat(MiniMessage.miniMessage().serialize(event.getFormat()));
        chatMessage.setContent(MiniMessage.miniMessage().serialize(event.getContent()));

        if (currentChannel.getProximityDistance() > 0) {// Send to local server
            sendGenericChat(chatMessage);
            return;
        }

        plugin.getDataManager().sendChatMessage(chatMessage);
    }

    /**
     * Player chat event, called by the chat listener
     *
     * @param player  Player
     * @param finalMessage The message to be sent
     */
    @Override
    public void outgoingMessage(CommandSender player, @NotNull final String finalMessage) {
        plugin.getDataManager().getActivePlayerChannel(player.getName(), registeredChannels)
                .thenAcceptAsync(channelName -> {
                    ChannelAudience audience;
                    String message = finalMessage;

                    if (isStaffChatEnabled(message, player)) {
                        audience = new ChannelAudience(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString());
                        message = message.substring(1);
                    } else if (channelName == null || !registeredChannels.containsKey(channelName)) {
                        audience = new ChannelAudience(KnownChatEntities.PUBLIC_CHAT.toString());
                    } else {
                        audience = new ChannelAudience(channelName);
                    }

                    if(plugin.config.debug) {
                        plugin.getLogger().info("Outgoing message channel: " + audience.getName());
                    }

                    outgoingMessage(player, audience, message);


                }, plugin.getExecutorService())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    /**
     * Send a private message to a player
     *
     * @param sender       The sender of the message
     * @param receiverName The name of the receiver
     * @param message      The message to be sent
     */
    public void outgoingPrivateMessage(@NotNull CommandSender sender, @NotNull String receiverName, @NotNull String message) {
        final ChatMessage privateChatMessage = new ChatMessage(
                new ChannelAudience(sender.getName(), AudienceType.PLAYER),
                plugin.config.getChatFormat(sender).private_format()
                        .replace("%receiver%", receiverName)
                        .replace("%sender%", sender.getName()),
                message,
                new ChannelAudience(receiverName, AudienceType.PLAYER)
        );

        final FilterResult result = filterManager.filterMessage(sender, privateChatMessage, AbstractFilter.Direction.OUTGOING);

        if (result.filtered()) {
            result.filteredReason().ifPresent(component ->
                    getComponentProvider().sendComponentOrCache(sender, component));
            return;
        }

        final Component formatComponent = getComponentProvider()
                //Parse format with placeholders
                .parse(sender, result.message().getFormat(), true, false, false)
                //Replace %message% with the content component
                .replaceText(builder -> builder.matchLiteral("%message%")
                        .replacement(miniMessage.deserialize(result.message().getContent())));

        //Send the message to the sender
        plugin.getComponentProvider().sendMessage(sender, formatComponent);

        //Send the message to the receiver
        plugin.getDataManager().sendChatMessage(privateChatMessage);
    }


    private boolean isStaffChatEnabled(String message, CommandSender player) {
        return plugin.config.enableStaffChat &&
                message.startsWith(plugin.config.staffChatPrefix) &&
                player.hasPermission(Permissions.ADMIN_STAFF_CHAT.getPermission());
    }


    @Override
    public void sendGenericChat(ChatMessage chatMessage) {
        final Set<Player> recipients = chatMessage.getReceiver().isPlayer() ?
                Collections.singleton(Bukkit.getPlayer(chatMessage.getReceiver().getName())) :
                new HashSet<>(plugin.getServer().getOnlinePlayers());


        for (Player recipient : recipients) {
            final FilterResult result = filterManager.filterMessage(recipient, chatMessage, AbstractFilter.Direction.INCOMING);
            if (result.filtered()) {
                result.filteredReason().ifPresent(component ->
                        getComponentProvider().sendComponentOrCache(recipient, component));
                continue;
            }

            //Channel sound
            plugin.getChannelManager().getChannel(chatMessage.getReceiver().getName()).ifPresent(channel1 -> {
                if (channel1.getNotificationSound() != null) {
                    recipient.playSound(recipient.getLocation(), channel1.getNotificationSound(), 1, 1);
                }
            });

            //Mention sound
            if (chatMessage.getContent().contains(recipient.getName())) {
                if (!plugin.config.mentionSound.isEmpty()) {
                    final String[] split = plugin.config.mentionSound.split(":");
                    recipient.playSound(
                            recipient.getLocation(),
                            Sound.valueOf(split[0]).getKey().toString(),
                            Float.parseFloat(split[1]),
                            Float.parseFloat(split[2]));
                }
            }
            final Component formattedComponent = miniMessage.deserialize(result.message().getFormat())
                    .replaceText(builder -> builder.matchLiteral("%message%").replacement(
                            miniMessage.deserialize(result.message().getContent())
                    ));
            if (chatMessage.getReceiver().isChannel()) {
                getComponentProvider().logComponent(formattedComponent);
            }

            //If proximity is enabled, check if player is in range
            if (!checkProximity(recipient, chatMessage)) {
                continue;
            }

            getComponentProvider().sendComponentOrCache(recipient, formattedComponent);
        }

    }

    private boolean checkProximity(Player recipient, ChatMessage chatMessage) {
        if (chatMessage.getReceiver().getProximityDistance() <= 0) return true;
        final Player sender = Bukkit.getPlayer(chatMessage.getSender().getName());
        if (sender == null) return false;
        if (!sender.getWorld().equals(recipient.getWorld())) return false;
        return !(sender.getLocation().distance(recipient.getLocation()) > chatMessage.getReceiver().getProximityDistance());
    }

    @Override
    public void pauseChat(@NotNull Player player) {
        getComponentProvider().pauseChat(player);
    }

    @Override
    public boolean isPaused(@NotNull Player player) {
        return getComponentProvider().isPaused(player);
    }

    @Override
    public void unpauseChat(@NotNull Player player) {
        getComponentProvider().unpauseChat(player);
    }

    @Override
    public Optional<Channel> getChannel(@Nullable String channelName) {
        if (channelName == null) return Optional.empty();
        if (channelName.equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString()))
            return Optional.of(getStaffChatChannel());
        else return Optional.ofNullable(registeredChannels.get(channelName));
    }

    @Override
    public Channel getPublicChannel(@Nullable CommandSender player) {

        final Channel publicChannel = getGenericPublic();
        publicChannel.setFormat(plugin.config.getChatFormat(player).format());

        return publicChannel;
    }

    private Channel getGenericPublic() {
        return Channel.channelBuilder(KnownChatEntities.PUBLIC_CHAT.toString())
                .format(plugin.config.defaultFormat.format())
                .rateLimit(plugin.config.rate_limit)
                .rateLimitPeriod(plugin.config.rate_limit_time_seconds)
                .discordWebhook(plugin.config.publicDiscordWebhook)
                .filtered(true)
                .notificationSound(null)
                .build();
    }

    @Override
    public Channel getStaffChatChannel() {
        return Channel.channelBuilder(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString())
                .format(plugin.config.staffChatFormat)
                .rateLimit(5)
                .rateLimitPeriod(1000)
                .discordWebhook(plugin.config.staffChatDiscordWebhook)
                .filtered(false)
                .notificationSound(null)
                .build();
    }

    public void setActiveChannel(String playerName, String channelName) {
        java.util.HashMap<String, String> playerChannelsMap = new java.util.HashMap<>();
        playerChannelsMap.put(channelName, "1");
        plugin.getDataManager().getActivePlayerChannel(playerName, plugin.getChannelManager().getRegisteredChannels())
                .thenAcceptAsync(channel -> {
                    if (channel != null && !channel.equals(KnownChatEntities.PUBLIC_CHAT.toString())) {
                        playerChannelsMap.put(channel, "0");
                    }
                    plugin.getDataManager().setPlayerChannelStatuses(playerName, playerChannelsMap);
                }, plugin.getExecutorService());
    }

    @Override
    public void addVanishIntegration(VanishIntegration vanishIntegration) {
        plugin.getPlayerListManager().addVanishIntegration(vanishIntegration);
    }

    @Override
    public void removeVanishIntegration(VanishIntegration vanishIntegration) {
        plugin.getPlayerListManager().removeVanishIntegration(vanishIntegration);
    }

}
