package dev.unnm3d.redischat.chat;

import com.google.common.base.Strings;

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
        this(senderName, formatting, message, KnownChatEntities.BROADCAST.toString());
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
        return !(receiverName.equals(KnownChatEntities.BROADCAST.toString()) || receiverName.startsWith(KnownChatEntities.PERMISSION_MULTICAST.toString()));
    }

    public String getReceiverName() {
        return receiverName;
    }


    public String serialize() {
        return senderName + "§§§" + formatting + "§§§" + message + "§§§" + receiverName;
    }
}
