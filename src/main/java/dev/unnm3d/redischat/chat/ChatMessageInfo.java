package dev.unnm3d.redischat.chat;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class ChatMessageInfo implements Serializable {
    private final ChatActor sender;
    private final String formatting;
    private final String message;
    private final ChatActor receiver;

    /**
     * Creates a ChatMessageInfo
     * Assumes that the message is a broadcast
     *
     * @param sender     The name of the sender
     * @param formatting The formatting of the message
     * @param message    The message content
     */
    public ChatMessageInfo(@NotNull ChatActor sender, String formatting, String message) {
        this(sender, formatting, message, new ChatActor(KnownChatEntities.PUBLIC_CHAT.toString(), ChatActor.ActorType.CHANNEL));
    }

    /**
     * Creates a ChatMessageInfo as "Server"
     * @param message The message content
     */
    public ChatMessageInfo(String message) {
        this(new ChatActor(), "%message%", message, new ChatActor(KnownChatEntities.PUBLIC_CHAT.toString(), ChatActor.ActorType.CHANNEL));
    }

    /**
     * Creates a ChatMessageInfo from a sender, formatting, message and receiver
     *
     * @param sender     The sender of the message
     * @param formatting The formatting of the message
     * @param message    The message content
     * @param receiver   The receiver of the message
     */
    public ChatMessageInfo(@NotNull ChatActor sender, @Nullable String formatting, @Nullable String message, @NotNull ChatActor receiver) {
        this.sender = sender;
        this.formatting = Strings.nullToEmpty(formatting);
        this.message = Strings.nullToEmpty(message);
        this.receiver = receiver;
    }


    public ChatActor getSender() {
        return sender;
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
        return receiver.isChannel();
    }

    public ChatActor getReceiver() {
        return receiver;
    }

    public String serialize() {
        return sender.serialize() + "§§§" + formatting + "§§§" + message + "§§§" + receiver.serialize();
    }

    public static ChatMessageInfo deserialize(String serialized) {
        final String[] splitted = serialized.split("§§§");
        return new ChatMessageInfo(new ChatActor(splitted[0]), splitted[1], splitted[2], new ChatActor(splitted[3]));
    }

}
