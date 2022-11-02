package dev.unnm3d.redischat;

public enum Permission {
    REDIS_CHAT_MESSAGE("redischat.message"),
    REDIS_CHAT_USE_FORMATTING("redischat.useformatting"),
    REDIS_CHAT_BROADCAST("redischat.broadcast"),
    REDIS_CHAT_CLEARCHAT("redischat.clearchat"),
    REDIS_CHAT_ADMIN("redischat.admin"),
    REDIS_CHAT_SPYCHAT("redischat.spychat"),
    ;

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}