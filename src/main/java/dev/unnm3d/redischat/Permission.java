package dev.unnm3d.redischat;

public enum Permission {
    REDIS_CHAT_MESSAGE("redischat.message"),
    REDIS_CHAT_MAIL_WRITE("redischat.mail.write"),
    REDIS_CHAT_MAIL_READ("redischat.mail.read"),
    REDIS_CHAT_IGNORE("redischat.ignore"),
    REDIS_CHAT_USE_FORMATTING("redischat.useformatting"),
    REDIS_CHAT_BROADCAST("redischat.broadcast"),
    REDIS_CHAT_CLEARCHAT("redischat.clearchat"),
    REDIS_CHAT_ADMIN("redischat.admin"),
    REDIS_CHAT_ADMIN_EDIT("redischat.editmessage"),
    REDIS_CHAT_BYPASS_RATE_LIMIT("redischat.bypass_rate_limit"),
    ;

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}