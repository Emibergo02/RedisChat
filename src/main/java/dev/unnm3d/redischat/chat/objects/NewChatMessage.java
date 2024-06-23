package dev.unnm3d.redischat.chat.objects;


import com.google.common.base.Strings;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@EqualsAndHashCode
@ToString
public class NewChatMessage implements DataSerializable {
    private final ChannelAudience sender;
    @Setter
    private String format;
    @Setter
    private String content;
    private final ChannelAudience receiver;


    /**
     * Creates a ChatMessageInfo as "Server"
     *
     * @param content The message content
     */
    public NewChatMessage(String content) {
        this(new ChannelAudience(), "%message%", content, ChannelAudience.publicChannelAudience());
    }

    /**
     * Creates a ChatMessageInfo as "Server"
     *
     * @param content The message content
     */
    public NewChatMessage(String content, String permissionToSee) {
        this(new ChannelAudience(), "%message%", content, ChannelAudience.publicChannelAudience(permissionToSee));
    }

    /**
     * Creates a ChatMessageInfo from a sender, formatting, message and receiver
     *
     * @param sender     The sender of the message
     * @param format The formatting of the message
     * @param content    The message content
     * @param receiver   The receiver of the message
     */
    public NewChatMessage(@NotNull ChannelAudience sender, @Nullable String format, @Nullable String content, @NotNull ChannelAudience receiver) {
        this.sender = sender;
        this.format = Strings.nullToEmpty(format);
        this.content = Strings.nullToEmpty(content);
        this.receiver = receiver;
    }

    public static NewChatMessage deserialize(String serializedMessage) {
        String[] parts = serializedMessage.split("§§;");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid message serialization");
        }

        return new NewChatMessage(
                ChannelAudience.deserialize(parts[0]),
                parts[1],
                parts[2],
                ChannelAudience.deserialize(parts[3])
        );
    }

    @Override
    public String serialize() {
        return sender.serialize() + "§§;" + format + "§§;" + content + "§§;" + receiver.serialize();
    }
}
