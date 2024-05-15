package dev.unnm3d.redischat.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.lang.reflect.Field;
import java.util.List;

@Configuration
public final class GuiSettings implements ConfigValidator {

    public String publicMailTabTitle = "Public Mail";
    public String privateMailTabTitle = "Private Mail";

    public List<String> mailGUIStructure = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# < # P # p # > #");
    public ItemStack mailItem = new ItemStack(Material.BOOK);
    public ItemStack unreadMailItem = new ItemStack(Material.ENCHANTED_BOOK);
    public ItemStack backButton = getBackButton();
    public ItemStack forwardButton = getForwardButton();
    public ItemStack PublicButton = getPublicButton();
    public ItemStack privateButton = getPrivateButton();

    public List<String> mailSettingsGUIStructure = List.of(
            "x x x x x x x x x",
            "x x D x x x U x x",
            "x x x x x x x x x");
    public String mailOptionsTitle = "Mail Options";
    public ItemStack deleteButton = getDeleteButton();
    public ItemStack unreadButton = getUnreadButton();


    @Comment("The structure of the channel GUI. Use 'x' for the channel slots, '<' for the back button, '>' for the forward button, 'U' for the unmute all button and 'S' for the silence public button")
    public List<String> channelGUIStructure = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# < # # S # # > #");
    public ItemStack activeChannelButton = getActiveChannelButton();
    public ItemStack idleChannel = getIdleChannelButton();
    public ItemStack mutedChannel = getMutedChannelButton();
    public ItemStack silencePublicButton = getSilencePublicButton();
    public ItemStack unSilencePublicButton = getUnSilencePublicButton();

    private ItemStack getMutedChannelButton() {
        ItemStack item = new ItemStack(Material.CYAN_DYE);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setLore(List.of("§9The channel is currently muted",
                "§bRight click to unmute the channel"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getIdleChannelButton() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setLore(List.of("§9Right click to mute the channel",
                "§bLeft click to write on this channel"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getActiveChannelButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setLore(List.of("§2You're writing on this channel",
                "§bLeft click to \"unlisten\" the channel"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getSilencePublicButton() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setDisplayName("§cSilence Public");
        im.setLore(List.of("§7Click to silence",
                "§7public messages"));
        item.setItemMeta(im);
        return item;
    }

    private ItemStack getUnSilencePublicButton() {
        ItemStack item = new ItemStack(Material.GRAY_WOOL);
        ItemMeta im = item.getItemMeta();
        if (im == null) return item;
        im.setDisplayName("§cActivate public chat");
        item.setItemMeta(im);
        return item;
    }

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

    private ItemStack getDeleteButton() {
        return new ItemBuilder(Material.BARRIER)
                .setDisplayName("§cDelete mail")
                .setLegacyLore(List.of("§7Click to delete the mail"))
                .get();
    }

    private ItemStack getUnreadButton() {
        return new ItemBuilder(Material.BOOK)
                .addEnchantment(Enchantment.DURABILITY, 1, false)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .setDisplayName("§cUnread mail")
                .setLegacyLore(List.of("§7Click to set the mail as unread"))
                .get();
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

    @Override
    public void validateConfig() {

    }
}
