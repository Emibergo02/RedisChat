package dev.unnm3d.redischat.permission;

import org.bukkit.OfflinePlayer;

public interface PermissionProvider {

    default void setPermission(OfflinePlayer player, String permission) {
    }

    default void unsetPermission(OfflinePlayer player, String permission) {
    }

}
