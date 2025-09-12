package dev.unnm3d.redischat.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class ItemNameProvider {
    private Method getItemNameMethod;
    private Method hasItemNameMethod;
    private final boolean useItemName;

    public ItemNameProvider(boolean useItemName) {
        try {
            this.getItemNameMethod = ItemMeta.class.getDeclaredMethod("getItemName");
            this.hasItemNameMethod = ItemMeta.class.getDeclaredMethod("hasItemName");
        } catch (NoSuchMethodException ignored) {
            this.useItemName = false;
            Bukkit.getLogger().warning("Failed to find ItemMeta#getItemName() method. Falling back to display name.");
            return;
        }
        this.useItemName = useItemName;
    }

    public String getItemName(ItemStack itemStack) {
        if (getItemNameMethod == null || !useItemName)
            return itemStack.getItemMeta().getDisplayName();
        try {
            return (String) getItemNameMethod.invoke(itemStack.getItemMeta());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasItemName(ItemStack itemMeta) {
        if (hasItemNameMethod == null || !useItemName)
            return itemMeta.getItemMeta().hasDisplayName();
        try {
            return (boolean) hasItemNameMethod.invoke(itemMeta.getItemMeta());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
