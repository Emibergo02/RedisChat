package dev.unnm3d.redischat.configs;

import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.List;

@Configuration
public final class GuiSettings {

    public String publicMailTabTitle = "Public Mail";
    public String privateMailTabTitle = "Private Mail";

    public List<String> structure = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# < # P # p # > #");
    public ItemStack mailItem = new ItemStack(Material.PAPER);
    public ItemStack backButton = getBackButton();
    public ItemStack forwardButton = getForwardButton();
    public ItemStack PublicButton = getPublicButton();
    public ItemStack privateButton = getPrivateButton();


    private ItemStack getBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setDisplayName("§cBack");
        im.setLore(List.of("§7Go back to the previous page"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getForwardButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setDisplayName("§aForward");
        im.setLore(List.of("§7Go forward to the next page"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getPublicButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setDisplayName("§aPublic");
        im.setLore(List.of("§7Click to see",
                "§7public announces and messages"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getPrivateButton() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setDisplayName("§cPrivate");
        im.setLore(List.of("§7Click to see",
                "§7the received and sent mail tab"));
        item.setItemMeta(im);
        return item;
    }


    public void setIngredient(String key, ItemStack item) {
        //Do the previous code with reflection
        for (Field declaredField : getClass().getDeclaredFields()) {
            if (declaredField.getName().equals(key)) {
                try {
                    declaredField.set(this, item);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

    }

}
