package dev.unnm3d.redischat.channels.gui;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.events.ChannelGuiPopulateEvent;
import dev.unnm3d.redischat.api.objects.KnownChatEntities;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ChannelGUI {
    private final RedisChat plugin;


    public Gui getChannelsGUI(@NotNull Player player, @Nullable String activeChannelName) {

        final List<PlayerChannel> items = plugin.getChannelManager().getAllChannels().stream()
                .filter(channel -> channel.isShownByDefault() || player.hasPermission(Permissions.CHANNEL_SHOW_PREFIX.getPermission() + channel.getName()))
                .map(channel -> new PlayerChannel(channel, player, channel.getName().equals(activeChannelName)))
                .filter(playerChannel -> !playerChannel.isHidden())
                .collect(Collectors.toList());

        final ChannelGuiPopulateEvent populateEvent = new ChannelGuiPopulateEvent(player, items);
        plugin.getServer().getPluginManager().callEvent(populateEvent);

        return PagedGui.itemsBuilder()
                .setStructure(
                        plugin.guiSettings.channelGUIStructure.toArray(new String[0]))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new AbstractPagedGuiBoundItem() {
                    @Override
                    public ItemProvider getItemProvider(Player player) {
                        return new ItemBuilder(plugin.guiSettings.backButton);
                    }

                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
                        PagedGui<?> gui = getGui();
                        if (gui.getPage() > 0) gui.setPage(gui.getPage() - 1);
                    }
                })
                .addIngredient('>', new AbstractPagedGuiBoundItem() {
                    @Override
                    public ItemProvider getItemProvider(Player player) {
                        return new ItemBuilder(plugin.guiSettings.forwardButton);
                    }

                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
                        PagedGui<?> gui = getGui();
                        if (gui.getPage() < gui.getPageCount() - 1) gui.setPage(gui.getPage() + 1);
                    }
                })
                .addIngredient('G', new GlobalChannel(
                        plugin.getChannelManager().getPublicChannel(null),
                        player,
                        KnownChatEntities.GENERAL_CHANNEL.toString().equals(activeChannelName)))
                .setContent(populateEvent.getChannelItems().stream().map(Item.class::cast).toList())
                .build();
    }


}
