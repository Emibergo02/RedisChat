package dev.unnm3d.redischat.datamanagers.sqlmanagers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.datamanagers.DataKey;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public abstract class PluginMessageManager {
    private static final Gson gson = new Gson();
    protected final RedisChat plugin;

    protected PluginMessageManager(RedisChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", (channel, player, message) -> {
            if (!channel.equals("BungeeCord")) return;
            receivePluginMessage(message);
        });
    }

    protected void receivePluginMessage(byte[] message) {
        final ByteArrayDataInput in = ByteStreams.newDataInput(message);
        final String subchannel = in.readUTF();
        final String messageString = in.readUTF();

        if (subchannel.equals(DataKey.CHAT_CHANNEL.toString())) {
            if (plugin.config.debug) {
                plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
            }
            plugin.getChannelManager().sendGenericChat(gson.fromJson(messageString, ChatMessage.class));
        } else if (subchannel.equals(DataKey.GLOBAL_CHANNEL.withoutCluster())) {
            if (plugin.config.debug) {
                plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
            }
            plugin.getChannelManager().sendGenericChat(gson.fromJson(messageString, ChatMessage.class));
        } else if (subchannel.equals(DataKey.PLAYERLIST.toString())) {
            if (plugin.getPlayerListManager() != null)
                plugin.getPlayerListManager().updatePlayerList(Arrays.asList(messageString.split("§")));
        }else if (subchannel.equals(DataKey.PLAYER_ACTIVE_CHANNEL_UPDATE.toString())) {
            final String[] splitMsg = messageString.split(";");
            if (splitMsg.length != 2) return;
            if (splitMsg[1].equals(DataKey.DELETE_TAG.toString())) {
                plugin.getChannelManager().updateActiveChannel(splitMsg[0], null);
                return;
            }
            plugin.getChannelManager().updateActiveChannel(splitMsg[0], splitMsg[1]);
        } else if (subchannel.equals(DataKey.CHANNEL_UPDATE.toString())) {
            if (messageString.startsWith(DataKey.DELETE_TAG.toString())) {
                plugin.getChannelManager().updateChannel(messageString.substring(7), null);
            } else {
                final Channel ch = gson.fromJson(messageString, Channel.class);
                plugin.getChannelManager().updateChannel(ch.getName(), ch);
            }
        } else if (subchannel.equals(DataKey.MAIL_UPDATE_CHANNEL.toString())) {
            plugin.getMailGUIManager().receiveMailUpdate(messageString);
        } else if (subchannel.equals(DataKey.MUTED_UPDATE.toString())) {
            plugin.getChannelManager().getMuteManager().serializedUpdate(messageString);
        } else if (subchannel.equals(DataKey.PLAYER_PLACEHOLDERS.toString())) {
            plugin.getPlaceholderManager().updatePlayerPlaceholders(messageString);
        } else if (subchannel.equals(DataKey.WHITELIST_ENABLED_UPDATE.toString())) {
            if (messageString.startsWith("D§")) {
                plugin.getChannelManager().getMuteManager().whitelistEnabledUpdate(messageString.substring(2), false);
            } else {
                plugin.getChannelManager().getMuteManager().whitelistEnabledUpdate(messageString, true);
            }
        }
    }

    public void sendChannelUpdate(String channelName, @Nullable Channel channel) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.CHANNEL_UPDATE.toString());

        if (channel == null) {
            out.writeUTF(DataKey.DELETE_TAG + channelName);
        } else {
            out.writeUTF(gson.toJson(channel));
        }

        sendPluginMessage(out.toByteArray());
    }

    public void sendWhitelistEnabledUpdate(String playerName, boolean enabled) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.WHITELIST_ENABLED_UPDATE.toString());
        out.writeUTF(enabled ? playerName : "D§" + playerName);
        sendPluginMessage(out.toByteArray());
    }

    public void sendPlayerPlaceholdersUpdate(String serializedPlaceholders) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.PLAYER_PLACEHOLDERS.toString());
        out.writeUTF(serializedPlaceholders);
        sendPluginMessage(out.toByteArray());
    }

    public void sendMailUpdate(@NotNull Mail mail) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.MAIL_UPDATE_CHANNEL.toString());
        out.writeUTF(mail.serializeWithId());

        plugin.getMailGUIManager().receiveMailUpdate(mail.serializeWithId());
        sendPluginMessage(out.toByteArray());
    }

    public void sendMutedEntityUpdate(@NotNull String entityKey, @NotNull Set<String> entitiesValue) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.MUTED_UPDATE.toString());
        out.writeUTF(entityKey + ";" + String.join(",", entitiesValue));

        sendPluginMessage(out.toByteArray());
    }

    protected void sendChatPluginMessage(String publishChannel, ChatMessage chatMessageInfo) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(publishChannel);
        out.writeUTF(gson.toJson(chatMessageInfo));
        sendPluginMessage(out.toByteArray());
    }

    protected void sendPluginMessage(byte[] byteArray) {
        final Iterator<? extends Player> iterator = plugin.getServer().getOnlinePlayers().iterator();
        if (!iterator.hasNext()) return;
        iterator.next().sendPluginMessage(plugin, "BungeeCord", byteArray);
    }

    public void updatePlayerChannel(@NotNull String playerName, @Nullable String playerChannel) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.PLAYER_ACTIVE_CHANNEL_UPDATE.toString());
        out.writeUTF(playerName + ";" + (playerChannel == null ? DataKey.DELETE_TAG.toString() : playerChannel));

        sendPluginMessage(out.toByteArray());
    }
}
