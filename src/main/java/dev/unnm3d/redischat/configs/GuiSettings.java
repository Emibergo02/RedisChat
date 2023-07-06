package dev.unnm3d.redischat.configs;

import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

@Configuration
public final class GuiSettings {

    public List<String> structure = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# < # P # p # > #");
    public ItemStack mailItem = new ItemStack(Material.PAPER);
    public ItemStack backButton = new ItemStack(Material.ARROW);
    public ItemStack forwardButton = new ItemStack(Material.ARROW);
    public ItemStack PublicButton = new ItemStack(Material.GREEN_WOOL);
    public ItemStack privateButton = new ItemStack(Material.RED_WOOL);

    public String publicMailTabTitle = "Public Mail";
    public String privateMailTabTitle = "Private Mail";


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
