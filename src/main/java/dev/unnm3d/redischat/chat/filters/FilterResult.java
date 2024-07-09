package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.chat.objects.ChatMessage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The result of a filter
 *
 * @param message  The resulting message (it can be the same as the filter input message or a modified version)
 * @param filtered Whether the message was filtered (if true, the message will not be sent)
 */
public record FilterResult(@NotNull ChatMessage message, boolean filtered, @NotNull Optional<Component> filteredReason) {

    public FilterResult(ChatMessage message, boolean filtered) {
        this(message, filtered, Optional.empty());
    }
}
