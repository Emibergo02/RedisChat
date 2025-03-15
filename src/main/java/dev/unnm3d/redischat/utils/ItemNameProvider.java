package dev.unnm3d.redischat.utils;

import org.bukkit.Bukkit;
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

    public String getItemName(ItemMeta itemMeta) {
        if (getItemNameMethod == null || !useItemName)
            return itemMeta.getDisplayName();
        try {
            return (String) getItemNameMethod.invoke(itemMeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasItemName(ItemMeta itemMeta) {
        if (hasItemNameMethod == null || !useItemName)
            return itemMeta.hasDisplayName();
        try {
            return (boolean) hasItemNameMethod.invoke(itemMeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
