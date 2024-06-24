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
import dev.unnm3d.redischat.chat.objects.NewChannel;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.mail.MailGUIManager;
import dev.unnm3d.redischat.moderation.MuteManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
    private final ConcurrentHashMap<String, NewChannel> registeredChannels;
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
    public void registerChannel(NewChannel channel) {
        registeredChannels.put(channel.getName(), channel);
        plugin.getDataManager().registerChannel(channel);
    }

    public void updateChannel(String channelName, @Nullable NewChannel channel) {
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


    public void outgoingMessage(CommandSender player, NewChannel chatChannel, @NotNull final String message) {

        NewChatMessage chatMessage = new NewChatMessage(
                new ChannelAudience(player.getName(), AudienceType.PLAYER),
                chatChannel.getFormat(),
                chatChannel.getName().equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString()) ?
                        message.substring(1) :
                        message,
                new ChannelAudience(chatChannel.getName())
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
                chatChannel,
                formatComponent,
                contentComponent);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        //Reserialize components
        chatMessage.setFormat(MiniMessage.miniMessage().serialize(event.getFormat()));
        chatMessage.setContent(MiniMessage.miniMessage().serialize(event.getContent()));

        if (chatChannel.getProximityDistance() > 0) {// Send to local server
            sendGenericChat(chatMessage);
            return;
        }

        plugin.getDataManager().sendChatMessage(chatMessage);
    }

    /**
     * Player chat event, called by the chat listener
     *
     * @param player  Player
     * @param message The message to be sent
     */
    @Override
    public void outgoingMessage(CommandSender player, @NotNull final String message) {
        plugin.getDataManager().getActivePlayerChannel(player.getName(), registeredChannels)
                .thenAcceptAsync(channelName -> {
                    final NewChannel chatChannel = isStaffChatEnabled(message, player) ?
                            getStaffChatChannel() :
                            getChannel(channelName).orElse(getPublicChannel(player));

                    outgoingMessage(player, chatChannel, message);

                }, plugin.getExecutorService())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    private boolean isStaffChatEnabled(String message, CommandSender player) {
        return plugin.config.enableStaffChat &&
                message.startsWith(plugin.config.staffChatPrefix) &&
                player.hasPermission(Permissions.ADMIN_STAFF_CHAT.getPermission());
    }


    @Override
    public void sendAndKeepLocal(NewChatMessage chatMessageInfo) {
        sendGenericChat(chatMessageInfo);
    }

    @Override
    public void sendGenericChat(NewChatMessage chatMessage) {
        final Component formattedComponent = miniMessage.deserialize(chatMessage.getFormat())
                .replaceText(builder -> builder.matchLiteral("%message%").replacement(
                        miniMessage.deserialize(chatMessage.getContent())
                ));

        final Set<Player> recipients = chatMessage.getReceiver().isPlayer() ?
                Collections.singleton(Bukkit.getPlayer(chatMessage.getReceiver().getName())) :
                new HashSet<>(plugin.getServer().getOnlinePlayers());


        if (chatMessage.getReceiver().isChannel()) {
            getComponentProvider().logComponent(formattedComponent);
        }

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
                    recipient.playSound(recipient.getLocation(), split[0], Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                }
            }

            //If proximity is enabled, check if player is in range
            if (!checkProximity(recipient, chatMessage)) {
                continue;
            }

            getComponentProvider().sendComponentOrCache(recipient, formattedComponent);
        }

    }

    private boolean checkProximity(Player recipient, NewChatMessage chatMessage) {
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
    public Optional<NewChannel> getChannel(@Nullable String channelName) {
        if (channelName == null) return Optional.empty();
        if (channelName.equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString()))
            return Optional.of(getStaffChatChannel());
        else return Optional.ofNullable(registeredChannels.get(channelName));
    }

    @Override
    public NewChannel getPublicChannel(@Nullable CommandSender player) {

        final NewChannel publicChannel = getGenericPublic();
        publicChannel.setFormat(plugin.config.getChatFormat(player).format());

        return publicChannel;
    }

    private NewChannel getGenericPublic() {
        return NewChannel.channelBuilder(KnownChatEntities.PUBLIC_CHAT.toString())
                .rateLimit(plugin.config.rate_limit)
                .rateLimitPeriod(plugin.config.rate_limit_time_seconds)
                .discordWebhook(plugin.config.publicDiscordWebhook)
                .filtered(true)
                .notificationSound(null)
                .build();
    }

    @Override
    public NewChannel getStaffChatChannel() {
        return NewChannel.channelBuilder(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString())
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
