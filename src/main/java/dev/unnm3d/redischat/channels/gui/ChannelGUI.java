package dev.unnm3d.redischat.channels.gui;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.events.ChannelGuiPopulateEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.List;

@AllArgsConstructor
public class ChannelGUI {
    private final RedisChat plugin;


    public Gui getChannelsGUI(@NotNull Player player, @Nullable String activeChannelName) {

        final List<PlayerChannel> items = plugin.getChannelManager().getAllChannels().stream()
                .filter(channel -> channel.isShownByDefault() || player.hasPermission(Permissions.CHANNEL_SHOW_PREFIX.getPermission() + channel.getName()))
                .map(channel -> new PlayerChannel(channel, player, channel.getName().equals(activeChannelName)))
                .filter(playerChannel -> !playerChannel.isHidden())
                .toList();

        final ChannelGuiPopulateEvent populateEvent = new ChannelGuiPopulateEvent(player, items);
        plugin.getServer().getPluginManager().callEvent(populateEvent);

        return PagedGui.items()
                .setStructure(
                        plugin.guiSettings.channelGUIStructure.toArray(new String[0]))
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
                .setContent(populateEvent.getChannelItems().stream().map(Item.class::cast).toList())
                .build();
    }


}
