package dev.unnm3d.redischat.utils;

import lombok.AllArgsConstructor;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@AllArgsConstructor
public class ItemNameProvider {
    private Method getItemNameField;
    private Method setItemNameField;
    private Method hasItemNameField;

    public ItemNameProvider() {
        try {
            this.getItemNameField = ItemMeta.class.getDeclaredMethod("getItemName");
            this.setItemNameField = ItemMeta.class.getDeclaredMethod("setItemName", String.class);
            this.hasItemNameField = ItemMeta.class.getDeclaredMethod("hasItemName");
        } catch (NoSuchMethodException ignored) {
        }
    }


    public String getItemName(ItemMeta itemMeta) {
        if (getItemNameField == null)
            return itemMeta.getDisplayName();
        try {
            return (String) getItemNameField.invoke(itemMeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public ItemMeta setItemName(ItemMeta itemMeta, String name) {
        if (setItemNameField == null) {
            itemMeta.setDisplayName(name);
            return itemMeta;
        }
        try {
            setItemNameField.invoke(itemMeta, name);
            return itemMeta;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasItemName(ItemMeta itemMeta) {
        if (hasItemNameField == null)
            return itemMeta.hasDisplayName();
        try {
            return (boolean) hasItemNameField.invoke(itemMeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
