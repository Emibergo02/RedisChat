package dev.unnm3d.redischat.chat;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class ChatMessageInfo implements Serializable {
    private final String senderName;
    private final String formatting;
    private final String message;
    private final String receiverName;

    /**
     * Creates a ChatMessageInfo
     * Assumes that the message is a broadcast
     *
     * @param senderName The name of the sender
     * @param formatting The formatting of the message
     * @param message    The message content
     */
    public ChatMessageInfo(String senderName, String formatting, String message) {
        this(senderName, formatting, message, KnownChatEntities.CHANNEL_PREFIX + KnownChatEntities.PUBLIC_CHAT.toString());
    }

    /**
     * Creates a ChatMessageInfo
     *
     * @param senderName   The name of the sender
     * @param formatting   The formatting of the message
     * @param message      The message content
     * @param receiverName The name of the receiver
     */
    public ChatMessageInfo(String senderName, String formatting, String message, String receiverName) {
        this.senderName = senderName;
        this.formatting = Strings.nullToEmpty(formatting);
        this.message = Strings.nullToEmpty(message);
        this.receiverName = receiverName;
    }

    /**
     * Creates a ChatMessageInfo from a serialized string
     *
     * @param serialized The serialized string
     */
    public ChatMessageInfo(String serialized) {
        String[] splitted = serialized.split("§§§");
        this.senderName = splitted[0];
        this.formatting = splitted[1];
        this.message = splitted[2];
        this.receiverName = splitted[3];
    }

    public static ChatMessageInfo craftChannelChatMessage(String senderName, String formatting, String message, @Nullable String channelName) {
        if (channelName == null) return new ChatMessageInfo(senderName, formatting, message);
        return new ChatMessageInfo(senderName, formatting, message, KnownChatEntities.CHANNEL_PREFIX + channelName);
    }


    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public String getFormatting() {
        return formatting;
    }

    /**
     * Returns if the message is private
     *
     * @return true if the message isn't a multicast or broadcast
     */
    public boolean isPrivate() {
        return !isChannel();
    }

    public boolean isChannel() {
        return receiverName.startsWith(KnownChatEntities.CHANNEL_PREFIX.toString());
    }

    public String getReceiverName() {
        return receiverName;
    }


    public String serialize() {
        return senderName + "§§§" + formatting + "§§§" + message + "§§§" + receiverName;
    }
}
