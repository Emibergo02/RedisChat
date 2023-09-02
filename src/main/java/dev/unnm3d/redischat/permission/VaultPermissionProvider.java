package dev.unnm3d.redischat.permission;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultPermissionProvider implements PermissionProvider {
    private Permission perms;

    public VaultPermissionProvider() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp != null)
            perms = rsp.getProvider();
    }

    public void setPermission(Player player, String permission) {
        if (perms != null)
            perms.playerAdd(player, permission);
    }

    public void unsetPermission(Player player, String permission) {
        if (perms != null)
            perms.playerRemove(player, permission);
    }
}
