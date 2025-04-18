package dev.unnm3d.redischat.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryPlaceholderEvent extends Event {
    public enum Type { INVENTORY, ITEM, ENDERCHEST }

    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final Player player;
    @Getter
    private final Type type;
    private ItemStack[] contents;

    public InventoryPlaceholderEvent(Player player, Type type, ItemStack[] defaultContents) {
        super(!player.getServer().isPrimaryThread());
        this.player   = player;
        this.type     = type;
        this.contents = defaultContents;
    }

    public ItemStack[] getContents() {
        return contents.clone();
    }

    /** Replace the inventory that will be shown when someone uses the chat tag */
    public void setContents(ItemStack[] newContents) {
        this.contents = newContents.clone();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
