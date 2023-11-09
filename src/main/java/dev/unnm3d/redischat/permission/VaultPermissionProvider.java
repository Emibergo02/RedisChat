package dev.unnm3d.redischat.permission;

import dev.unnm3d.redischat.RedisChat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultPermissionProvider implements PermissionProvider {
    private Permission perms;

    public VaultPermissionProvider() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp != null)
            perms = rsp.getProvider();
    }

    @Override
    public void setPermission(OfflinePlayer player, String permission) {
        if (!(player instanceof Player onlinePlayer)) {
            RedisChat.getInstance().getLogger().warning("Player " + player.getName() + " is not online, can't set permission " + permission);
            return;
        }

        if (perms != null)
            perms.playerAdd(onlinePlayer, permission);
    }

    @Override
    public void unsetPermission(OfflinePlayer player, String permission) {
        if (!(player instanceof Player onlinePlayer)) {
            RedisChat.getInstance().getLogger().warning("Player " + player.getName() + " is not online, can't unset permission " + permission);
            return;
        }

        if (perms != null)
            perms.playerRemove(onlinePlayer, permission);
    }
}
