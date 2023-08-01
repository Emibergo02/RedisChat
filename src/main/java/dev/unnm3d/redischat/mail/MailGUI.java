package dev.unnm3d.redischat.mail;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

@AllArgsConstructor
public class MailGUI {

    private final RedisChat plugin;

    public void openPublicMailGui(Player player) {
        this.plugin.getDataManager().getPublicMails()
                .thenAccept(list -> {
                            Gui global = getMailGui(list);
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    Window.single()
                                            .setTitle(plugin.guiSettings.publicMailTabTitle)
                                            .setGui(global)
                                            .open(player));
                        }
                );
    }

    public void openPrivateMailGui(Player player) {
        this.plugin.getDataManager().getPlayerPrivateMail(player.getName())
                .thenAccept(list -> {
                            Gui global = getMailGui(list);
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    Window.single()
                                            .setTitle(plugin.guiSettings.privateMailTabTitle)
                                            .setGui(global)
                                            .open(player));
                        }
                );
    }

    private Gui getMailGui(List<Mail> list) {
        return PagedGui.items()
                .setStructure(
                        plugin.guiSettings.structure.toArray(new String[0])
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // where paged items should be put
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new ItemBuilder(plugin.guiSettings.backButton);
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new ItemBuilder(plugin.guiSettings.forwardButton);
                    }
                })
                .addIngredient('P', new ControlItem<>() {
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        event.setCancelled(true);
                        player.closeInventory();
                        openPublicMailGui(player);
                    }

                    @Override
                    public ItemProvider getItemProvider(Gui gui) {
                        return new ItemBuilder(plugin.guiSettings.PublicButton);
                    }
                })
                .addIngredient('p', new ControlItem<>() {
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        event.setCancelled(true);
                        player.closeInventory();
                        openPrivateMailGui(player);
                    }

                    @Override
                    public ItemProvider getItemProvider(Gui gui) {
                        return new ItemBuilder(plugin.guiSettings.privateButton);
                    }
                })
                .setContent(list.stream().map(mail -> (Item) mail).toList())
                .build();
    }


}
