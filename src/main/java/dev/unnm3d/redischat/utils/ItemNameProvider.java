package dev.unnm3d.redischat.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class ItemNameProvider {
    private Method getItemNameField;
    private Method hasItemNameField;
    private final boolean useItemName;

    public ItemNameProvider(boolean useItemName) {
        try {
            this.getItemNameField = ItemMeta.class.getDeclaredMethod("getItemName");
            this.hasItemNameField = ItemMeta.class.getDeclaredMethod("hasItemName");
        } catch (NoSuchMethodException ignored) {
            this.useItemName = false;
            Bukkit.getLogger().warning("Failed to find ItemMeta#getItemName() method. Falling back to display name.");
            return;
        }
        this.useItemName = useItemName;
    }

    public String getItemName(ItemMeta itemMeta) {
        if (getItemNameField == null || !useItemName)
            return itemMeta.getDisplayName();
        try {
            return (String) getItemNameField.invoke(itemMeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasItemName(ItemMeta itemMeta) {
        if (hasItemNameField == null || !useItemName)
            return itemMeta.hasDisplayName();
        try {
            return (boolean) hasItemNameField.invoke(itemMeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
