package dev.unnm3d.redischat.redis;

import java.io.Serializable;

public class ChatPacket implements Serializable {
    private final String message;
    private final String senderName;
    private final String receiverName;


    public ChatPacket(String senderName, String message, String receiverName) {
        this.message = message;
        this.senderName = senderName;
        this.receiverName = receiverName;
    }

    public ChatPacket(String senderName, String message) {
        this.message = message;
        this.senderName = senderName;
        this.receiverName = null;
    }


    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public boolean byServer() {
        return senderName == null;
    }

    public boolean isPrivate() {
        return receiverName != null;
    }

    public String getReceiverName() {
        return receiverName;
    }
}
