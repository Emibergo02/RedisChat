package dev.unnm3d.redischat.chat;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Structure;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class ChatColorGUI {
    private final RedisChat plugin;

    public ChatColorGUI(RedisChat plugin) {
        this.plugin = plugin;
    }

    public Gui buildGui() {
        Structure structure = new Structure(plugin.guiSettings.chatColorGUIStructure.toArray(new String[0]));
        addColorIngredient(structure, '0', plugin.guiSettings.colorBlack, ChatColor.BLACK);
        addColorIngredient(structure, '1', plugin.guiSettings.colorDarkBlue, ChatColor.DARK_BLUE);
        addColorIngredient(structure, '2', plugin.guiSettings.colorDarkGreen, ChatColor.DARK_GREEN);
        addColorIngredient(structure, '3', plugin.guiSettings.colorDarkAqua, ChatColor.DARK_AQUA);
        addColorIngredient(structure, '4', plugin.guiSettings.colorDarkRed, ChatColor.DARK_RED);
        addColorIngredient(structure, '5', plugin.guiSettings.colorDarkPurple, ChatColor.DARK_PURPLE);
        addColorIngredient(structure, '6', plugin.guiSettings.colorGold, ChatColor.GOLD);
        addColorIngredient(structure, '7', plugin.guiSettings.colorGray, ChatColor.GRAY);
        addColorIngredient(structure, '8', plugin.guiSettings.colorDarkGray, ChatColor.DARK_GRAY);
        addColorIngredient(structure, '9', plugin.guiSettings.colorBlue, ChatColor.BLUE);
        addColorIngredient(structure, 'a', plugin.guiSettings.colorGreen, ChatColor.GREEN);
        addColorIngredient(structure, 'b', plugin.guiSettings.colorAqua, ChatColor.AQUA);
        addColorIngredient(structure, 'c', plugin.guiSettings.colorRed, ChatColor.RED);
        addColorIngredient(structure, 'd', plugin.guiSettings.colorLightPurple, ChatColor.LIGHT_PURPLE);
        addColorIngredient(structure, 'e', plugin.guiSettings.colorYellow, ChatColor.YELLOW);
        addColorIngredient(structure, 'f', plugin.guiSettings.colorWhite, ChatColor.WHITE);
        addColorIngredient(structure, 'r', plugin.guiSettings.colorReset, ChatColor.RESET);
        return Gui.of(structure);
    }

    private void addColorIngredient(Structure structure, char key, ItemStack item, ChatColor color) {
        structure.addIngredient(key, new AbstractItem() {
            @Override
            public ItemProvider getItemProvider(Player player) {
                return new ItemBuilder(item);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
                player.closeInventory();
                if (color == ChatColor.RESET) {
                    plugin.getPlaceholderManager().removePlayerPlaceholder(player.getName(), "chat_color");
                } else if (player.hasPermission(Permissions.CHAT_COLOR.getPermission() + "." + color.name().toLowerCase())) {
                    plugin.getPlaceholderManager().addPlayerPlaceholder(player.getName(), "chat_color", "<" + color.name().toLowerCase() + ">");
                } else {
                    plugin.messages.sendMessage(player, plugin.messages.noPermission);
                    return;
                }
                plugin.messages.sendMessage(player, plugin.messages.color_set);
            }
        });
    }
}
