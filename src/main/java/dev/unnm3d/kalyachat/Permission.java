package dev.unnm3d.kalyachat;

public enum Permission {
    KALYA_CHAT_MESSAGE("kalyachat.message"),
    KALYA_CHAT_USE_FORMATTING("kalyachat.useformatting"),
    KALYA_CHAT_BROADCAST("kalyachat.broadcast"),
    KALYA_CHAT_CLEARCHAT("kalyachat.clearchat"),
    KALYA_CHAT_ADMIN("kalyachat.admin"),
    KALYA_CHAT_SPYCHAT("kalyachat.spychat"),
    ;

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}