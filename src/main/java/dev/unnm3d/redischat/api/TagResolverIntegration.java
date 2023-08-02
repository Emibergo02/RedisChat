package dev.unnm3d.redischat.api;

public interface TagResolverIntegration {

    /**
     * Parse tags/placeholders of a message
     * To be used for MiniMessage or other tag/placeholders systems
     * @param message The message to resolve
     * @return The resolved message
     */
    default String parseTags(String message){
        return message;
    }

}
