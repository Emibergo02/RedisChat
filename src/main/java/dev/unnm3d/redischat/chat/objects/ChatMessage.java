package dev.unnm3d.redischat.chat.objects;


import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@EqualsAndHashCode
@ToString
public class ChatMessage {
    private final ChannelAudience sender;
    private final long timestamp;
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
    public ChatMessage(String content) {
        this(new ChannelAudience(), System.currentTimeMillis(), "{message}", content, ChannelAudience.publicChannelAudience());
    }

    /**
     * Creates a ChatMessageInfo as "Server"
     *
     * @param content The message content
     */
    public ChatMessage(String content, String permissionToSee) {
        this(new ChannelAudience(), System.currentTimeMillis(), "{message}", content, ChannelAudience.publicChannelAudience(permissionToSee));
    }

    /**
     * Used for deserialization
     *
     * @param sender   The sender of the message
     * @param format   The formatting of the message
     * @param content  The message content
     * @param receiver The receiver of the message
     */
    public ChatMessage(@NotNull ChannelAudience sender, long timestamp, @Nullable String format, @Nullable String content, @NotNull ChannelAudience receiver) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.format = Strings.nullToEmpty(format);
        this.content = Strings.nullToEmpty(content);
        this.receiver = receiver;
    }


    /**
     * Creates a ChatMessageInfo from a sender, formatting, message and receiver
     *
     * @param sender   The sender of the message
     * @param format   The formatting of the message
     * @param content  The message content
     * @param receiver The receiver of the message
     */
    public ChatMessage(@NotNull ChannelAudience sender, @Nullable String format, @Nullable String content, @NotNull ChannelAudience receiver) {
        this(sender, System.currentTimeMillis(), format, content, receiver);
    }
}
