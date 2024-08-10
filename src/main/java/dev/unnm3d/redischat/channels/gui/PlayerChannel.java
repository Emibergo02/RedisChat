package dev.unnm3d.redischat.channels.gui;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.chat.objects.Channel;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

@Getter
public class PlayerChannel extends AbstractItem {
    private final Channel channel;
    private final Player player;
    private Status status;


    public PlayerChannel(Channel channel, Player player, boolean isActive) {
        this.channel = channel;
        this.player = player;
        final String channelPermission = Permissions.CHANNEL_PREFIX.getPermission() + channel.getName();
        if (isActive) {
            status = Status.LISTENING;
        } else if (!player.isOp()&&player.hasPermission(Permissions.CHANNEL_HIDE_PREFIX.getPermission() + channel.getName())) {
            status = Status.HIDDEN;
        } else if (channel.isPermissionEnabled() && !player.hasPermission(channelPermission) && !player.hasPermission(channelPermission + ".read")) {
            status = Status.MUTED;
        } else {
            status = Status.IDLE;
        }
    }


    public boolean isListening() {
        return status == Status.LISTENING;
    }

    public boolean isMuted() {
        return status == Status.MUTED;
    }

    public boolean isHidden() {
        return status == Status.HIDDEN;
    }


    @Override
    public ItemProvider getItemProvider() {
        ItemStack item;
        if (status == Status.MUTED) {
            item = RedisChat.getInstance().guiSettings.mutedChannel;
        } else if (status == Status.LISTENING) {
            item = RedisChat.getInstance().guiSettings.activeChannelButton;
        } else if (status == Status.HIDDEN) {
            return new ItemBuilder(Material.AIR);
        } else {
            item = RedisChat.getInstance().guiSettings.idleChannel;
        }

        final ItemMeta im = item.getItemMeta();
        if (im != null)
            im.setDisplayName("§r" + channel.getDisplayName());
        item.setItemMeta(im);
        return new ItemBuilder(item);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {
            if (status == Status.IDLE) {
                status = Status.LISTENING;
                RedisChat.getInstance().getChannelManager().setActiveChannel(player.getName(), channel.getName());

                player.closeInventory();
                RedisChat.getInstance().getChannelManager().openChannelsGUI(player);
            } else if (status == Status.LISTENING) {
                status = Status.IDLE;
                final String nextChannel = this.channel.getName().equals(KnownChatEntities.GENERAL_CHANNEL.toString()) ?
                        KnownChatEntities.VOID_CHAT.toString() : KnownChatEntities.GENERAL_CHANNEL.toString();
                RedisChat.getInstance().getChannelManager().setActiveChannel(player.getName(), nextChannel);
            }
        } else if (clickType.isRightClick()) {
            if (status == Status.IDLE) {
                RedisChat.getInstance().getPermissionProvider().unsetPermission(player, Permissions.CHANNEL_PREFIX.getPermission() + channel.getName());
                RedisChat.getInstance().getPermissionProvider().unsetPermission(player, Permissions.CHANNEL_PREFIX.getPermission() + channel.getName() + ".read");
                status = Status.MUTED;
            } else if (status == Status.MUTED) {
                RedisChat.getInstance().getPermissionProvider().setPermission(player, Permissions.CHANNEL_PREFIX.getPermission() + channel.getName());
                RedisChat.getInstance().getPermissionProvider().setPermission(player, Permissions.CHANNEL_PREFIX.getPermission() + channel.getName() + ".read");
                status = Status.IDLE;
            }
        }
        notifyWindows();
    }

    public enum Status {
        IDLE,
        HIDDEN,
        LISTENING,
        MUTED
    }
}
