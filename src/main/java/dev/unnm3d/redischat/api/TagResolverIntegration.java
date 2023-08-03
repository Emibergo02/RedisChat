package dev.unnm3d.redischat.api;

import org.jetbrains.annotations.NotNull;

public interface TagResolverIntegration {

    /**
     * Parse tags/placeholders of a message
     * To be used for MiniMessage or other tag/placeholders systems
     * @param message The message to resolve
     * @return The resolved message
     */
    default @NotNull String parseTags(String message){
        return message;
    }

}
