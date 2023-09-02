package dev.unnm3d.redischat.permission;

import org.bukkit.entity.Player;

public interface PermissionProvider {

    default void setPermission(Player player, String permission) {
    }

    default void unsetPermission(Player player, String permission) {
    }

}
