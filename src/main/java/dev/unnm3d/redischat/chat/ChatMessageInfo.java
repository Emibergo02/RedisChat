package dev.unnm3d.redischat.chat;

import java.io.Serializable;

public class ChatMessageInfo implements Serializable {
    private final String senderName;
    private final String message;
    private final String receiverName;


    public ChatMessageInfo(String senderName, String message) {
        this(senderName, message, KnownChatEntities.BROADCAST.toString());
    }

    public ChatMessageInfo(String senderName, String message, String receiverName) {
        this.senderName = senderName;
        this.message = message;
        this.receiverName = receiverName;
    }

    public ChatMessageInfo(String serialized) {
        String[] splitted = serialized.split("§§§");
        this.senderName = splitted[0];
        this.message = splitted[1];
        this.receiverName = splitted[2];
    }


    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isPrivate() {
        return !(receiverName.equals(KnownChatEntities.BROADCAST.toString()) || receiverName.startsWith(KnownChatEntities.PERMISSION_MULTICAST.toString()));
    }

    public String getReceiverName() {
        return receiverName;
    }


    public String serialize() {
        return senderName + "§§§" + message + "§§§" + receiverName;
    }
}
