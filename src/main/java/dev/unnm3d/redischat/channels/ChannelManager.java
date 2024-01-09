package dev.unnm3d.redischat.channels;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.AsyncRedisChatMessageEvent;
import dev.unnm3d.redischat.api.RedisChatAPI;
import dev.unnm3d.redischat.api.VanishIntegration;
import dev.unnm3d.redischat.chat.*;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager extends RedisChatAPI {

    private final RedisChat plugin;
    @Getter
    private final ConcurrentHashMap<String, Channel> registeredChannels;
    @Getter
    private final ChannelGUI channelGUI;


    public ChannelManager(RedisChat plugin) {
        INSTANCE = this;
        this.plugin = plugin;
        this.registeredChannels = new ConcurrentHashMap<>();
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
                        Bukkit.getServer().getScheduler().runTask(plugin, () ->
                                Window.single()
                                        .setTitle("Channels")
                                        .setGui(channelGUI.getChannelsGUI(player, playerChannelInfo))
                                        .setCloseHandlers(List.of(() -> new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                player.updateInventory();
                                            }
                                        }.runTaskLater(plugin, 1)))
                                        .open(player)));
    }

    @Override
    protected boolean isRateLimited(CommandSender player, Channel channel) {
        if (!player.hasPermission(Permissions.BYPASS_RATE_LIMIT.getPermission())) {
            if (plugin.getDataManager().isRateLimited(player.getName(), channel)) {
                plugin.messages.sendMessage(player, plugin.messages.rate_limited);
                return true;
            }
        }
        return false;
    }

    private String antiCaps(CommandSender player, String message) {
        if (getComponentProvider().antiCaps(message)) {
            plugin.messages.sendMessage(player, plugin.messages.caps);
            return message.toLowerCase();
        }
        return message;
    }

    @Override
    public void playerChannelMessage(CommandSender player, Channel channel, @NotNull String message) {
        final long init = System.currentTimeMillis();
        if (isRateLimited(player, channel)) return;

        if (plugin.config.debug) {
            plugin.getLogger().info("2) Rate limit (Redis): " + (System.currentTimeMillis() - init) + "ms");
        }

        //Placeholders and purge minimessage tags if player doesn't have permission
        boolean parsePlaceholders = player.hasPermission(Permissions.USE_FORMATTING.getPermission());

        if (!parsePlaceholders) {
            message = getComponentProvider().purgeTags(message);
        }

        if (!player.hasPermission(Permissions.USE_DANGEROUS.getPermission())) {
            message = message
                    .replace("run_command", "copy_to_clipboard")
                    .replace("suggest_command", "copy_to_clipboard");
        }

        if (message.trim().isEmpty()) return;//Check if message is empty after purging tags

        if (plugin.config.debug) {
            plugin.getLogger().info("2) Placeholders or purge tags: " + (System.currentTimeMillis() - init) + "ms");
        }

        if (channel.isFiltered()) {
            message = antiCaps(player, message);

            //Word filter
            message = getComponentProvider().sanitize(message);
        }

        if (plugin.config.debug) {
            plugin.getLogger().info("2) Word filter: " + (System.currentTimeMillis() - init) + "ms");
        }

        //Check inv update
        message = plugin.getComponentProvider().invShareFormatting(player, message);


        final Component formatted = getComponentProvider().parse(player, channel.getFormat(), true, false, false);

        //Parse to MiniMessage component (placeholders, tags and mentions)
        Component toBeReplaced = getComponentProvider().parse(player, message, parsePlaceholders, true, true,
                getComponentProvider().getRedisChatTagResolver(player));

        //Parse customs
        toBeReplaced = getComponentProvider().parseCustomPlaceholders(player, toBeReplaced);

        if (plugin.config.debug) {
            plugin.getLogger().info("2) Format + message parsing: " + (System.currentTimeMillis() - init) + "ms");
        }

        //Call event and check cancellation
        final AsyncRedisChatMessageEvent event = new AsyncRedisChatMessageEvent(player, channel,
                MiniMessage.miniMessage().serialize(formatted),
                MiniMessage.miniMessage().serialize(toBeReplaced));
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;


        final ChatMessageInfo cmi = new ChatMessageInfo(
                new ChatActor(player.getName(), ChatActor.ActorType.PLAYER),
                event.getFormat(),
                event.getMessage(),
                new ChatActor(event.getChannel().getName(), ChatActor.ActorType.CHANNEL));

        if (channel.getProximityDistance() > 0) {// Send to local server
            sendGenericChat(cmi);
            return;
        }

        // Send to other servers
        plugin.getDataManager().sendChatMessage(cmi);


        if (plugin.config.debug) {
            plugin.getLogger().info("2) Send (Redis): " + (System.currentTimeMillis() - init) + "ms, Millis: " + System.currentTimeMillis());
        }
    }

    public void playerChat(Player player, @NotNull final String finalMessage) {
        final long init = System.currentTimeMillis();
        plugin.getDataManager().getActivePlayerChannel(player.getName(), registeredChannels)
                .thenAcceptAsync(channelName -> {
                    String message = finalMessage;
                    Channel chatChannel;
                    if (message.startsWith(plugin.config.staffChatPrefix) &&
                            player.hasPermission(Permissions.ADMIN_STAFF_CHAT.getPermission()) && plugin.config.enableStaffChat) {
                        chatChannel = getStaffChatChannel();
                        message = message.substring(1);
                    } else {
                        chatChannel = getChannel(channelName).orElse(getPublicChannel(player));
                    }
                    if (plugin.config.debug) {
                        plugin.getLogger().info("1) Active channel (Redis) + channel parsing: " + (System.currentTimeMillis() - init) + "ms");
                    }
                    playerChannelMessage(player, chatChannel, message);
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }


    @Override
    public void sendLocalChatMessage(ChatMessageInfo chatMessageInfo) {
        if (chatMessageInfo.getReceiver().isPlayer()) {
            long init = System.currentTimeMillis();

            final Component spyComponent =
                    MiniMessage.miniMessage().deserialize(plugin.messages.spychat_format
                                    .replace("%receiver%", chatMessageInfo.getReceiver().getName())
                                    .replace("%sender%", chatMessageInfo.getSender().getName()))
                            .replaceText(builder -> builder.matchLiteral("%message%").replacement(
                                    MiniMessage.miniMessage().deserialize(chatMessageInfo.getMessage())
                            ));

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().equals(chatMessageInfo.getReceiver().getName())) {//Private message
                    plugin.getDataManager().isIgnoring(chatMessageInfo.getReceiver().getName(), chatMessageInfo.getSender().getName())
                            .thenAccept(ignored -> {
                                if (!ignored)
                                    plugin.getChannelManager().sendPrivateChat(chatMessageInfo);
                                if (plugin.config.debug) {
                                    plugin.getLogger().info("Private message sent to " + chatMessageInfo.getReceiver().getName() + " with ignore: " + ignored + " in " + (System.currentTimeMillis() - init) + "ms");
                                }
                            });
                }

                if (plugin.getSpyManager().isSpying(player.getName())) {//Spychat
                    getComponentProvider().sendComponentOrCache(player, spyComponent);
                }
            }

            if (plugin.config.chatLogging) {
                getComponentProvider().logToHistory(spyComponent); //Send to console for logging purposes
            }

            return;
        }

        plugin.getChannelManager().sendGenericChat(chatMessageInfo);
    }

    @Override
    public void sendGenericChat(@NotNull ChatMessageInfo chatMessageInfo) {
        long init = System.currentTimeMillis();

        Channel channel = getGenericPublic();
        if (chatMessageInfo.getReceiver().isChannel()) {
            channel = plugin.getChannelManager().getChannel(chatMessageInfo.getReceiver().getName()).orElse(getGenericPublic());
        }

        if (plugin.config.debug) {
            plugin.getLogger().info("R2) Permission check");
        }


        Component formattedComponent = MiniMessage.miniMessage().deserialize(chatMessageInfo.getFormatting()).replaceText(
                builder -> builder.matchLiteral("%message%").replacement(
                        MiniMessage.miniMessage().deserialize(chatMessageInfo.getMessage())
                )
        );
        if (plugin.config.debug) {
            plugin.getLogger().info("R3) Componentize message: " + (System.currentTimeMillis() - init) + "ms");
        }

        //Effeciently send to all players with permission
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission(Permissions.CHANNEL_PREFIX.getPermission() + channel.getName()))
                continue;

            if (chatMessageInfo.getReceiver().needPermission())
                if (!onlinePlayer.hasPermission(chatMessageInfo.getReceiver().getName())) {
                    continue;
                }

            if (!checkProximity(onlinePlayer, chatMessageInfo.getSender().getName(), channel))
                continue;

            if (chatMessageInfo.getMessage().contains(onlinePlayer.getName())) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 2.0f);
            }

            getComponentProvider().sendComponentOrCache(onlinePlayer, formattedComponent);
        }

        if (!plugin.config.chatLogging) {
            getComponentProvider().logToConsole(formattedComponent); //Send to console for logging purposes
        } else {
            getComponentProvider().logToHistory(formattedComponent);
        }

        // Send to discord integration
        plugin.getDiscordHook().sendDiscordMessage(channel, chatMessageInfo);

        if (plugin.config.debug) {
            plugin.getLogger().info("R4) Log and send message: " + (System.currentTimeMillis() - init) + "ms");
        }
    }

    private boolean checkProximity(@NotNull Player receiver, String senderName, Channel channel) {
        if (channel.getProximityDistance() <= 0) return true;
        final Player sender = Bukkit.getPlayer(senderName);
        if (sender == null) return false;
        if (!sender.getWorld().equals(receiver.getWorld())) return false;
        return !(sender.getLocation().distance(receiver.getLocation()) > channel.getProximityDistance());
    }

    @Override
    public void sendPrivateChat(@NotNull ChatMessageInfo chatMessageInfo) {
        Player p = Bukkit.getPlayer(chatMessageInfo.getReceiver().getName());
        if (p != null)
            if (p.isOnline()) {
                final ChatFormat chatFormat = plugin.config.getChatFormat(p);

                Component formatted = getComponentProvider().parse(null,
                        chatFormat.receive_private_format()
                                .replace("%receiver%", chatMessageInfo.getReceiver().getName())
                                .replace("%sender%", chatMessageInfo.getSender().getName()),
                        //Parameters disabled: already parsed on sender side
                        false, false, false,
                        getComponentProvider().getStandardTagResolver());

                final Component toBeReplaced = getComponentProvider().parse(p, chatMessageInfo.getMessage(),
                        false, false, false,
                        getComponentProvider().getStandardTagResolver());
                //Put message into format
                formatted = formatted.replaceText(
                        builder -> builder.matchLiteral("%message%").replacement(toBeReplaced)
                );
                getComponentProvider().sendComponentOrCache(p, formatted);
                if (!plugin.config.privateMessageNotificationSound.isEmpty())
                    p.playSound(p.getLocation(), Sound.valueOf(plugin.config.privateMessageNotificationSound), 1, 1.0f);
            }
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
        return new Channel(KnownChatEntities.PUBLIC_CHAT.toString(),
                "",
                plugin.config.rate_limit,
                plugin.config.rate_limit_time_seconds,
                -1,
                plugin.config.publicDiscordWebhook,
                true,
                null);
    }

    @Override
    public Channel getStaffChatChannel() {
        return new Channel(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString(),
                plugin.config.staffChatFormat,
                5,
                1000,
                -1,
                plugin.config.staffChatDiscordWebhook,
                false,
                null);
    }

    public void setActiveChannel(String playerName, String channelName) {
        java.util.HashMap<String, String> playerChannelsMap = new java.util.HashMap<>();
        playerChannelsMap.put(channelName, "1");
        plugin.getDataManager().getActivePlayerChannel(playerName, plugin.getChannelManager().getRegisteredChannels())
                .thenAcceptAsync(channel -> {
                    if (channel != null)
                        playerChannelsMap.put(channel, "0");
                    plugin.getDataManager().setPlayerChannelStatuses(playerName, playerChannelsMap);
                });
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
