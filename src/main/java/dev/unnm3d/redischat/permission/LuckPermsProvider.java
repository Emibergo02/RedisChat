package dev.unnm3d.redischat.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsProvider implements PermissionProvider {
    private LuckPerms perms;

    public LuckPermsProvider() {
        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (rsp != null)
            perms = rsp.getProvider();
    }

    public void setPermission(Player player, String permission) {
        if (perms == null) return;
        perms.getUserManager().modifyUser(player.getUniqueId(), user ->
                user.data().add(
                        Node.builder(permission)
                                .context(perms.getContextManager().getStaticContext())
                                .value(true)
                                .build()));
    }

    public void unsetPermission(Player player, String permission) {
        if (perms == null) return;
        perms.getUserManager().modifyUser(player.getUniqueId(), user ->
                user.data().add(
                        Node.builder(permission)
                                .context(perms.getContextManager().getStaticContext())
                                .value(false)
                                .build()));
    }
}
