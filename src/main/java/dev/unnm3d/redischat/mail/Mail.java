package dev.unnm3d.redischat.mail;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ComponentProvider;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.inventory.Book;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.Arrays;

@Getter
public class Mail extends AbstractItem {

    final double id;
    @Setter
    MailCategory category;
    final String sender;
    final String receiver;
    final String title;
    @Setter
    String content;

    public Mail(String sender, String receiver, String title) {
        this.id = System.currentTimeMillis() + ((int) (Math.random() * 100.0) / 100.0);
        this.category = receiver.equals("*public") ? MailCategory.PUBLIC : MailCategory.PRIVATE;
        this.sender = sender;
        this.receiver = receiver;
        this.title = title;
    }

    public Mail(double id, String serialized) {
        String[] splitted = serialized.split("§§");
        this.id = id;
        this.category = MailCategory.fromString(splitted[0]);
        this.sender = splitted[1];
        this.receiver = splitted[2];
        this.title = splitted[3];
        this.content = splitted[4];
    }


    @Override
    public String toString() {
        return category.toString() + "§§" + sender + "§§" + receiver + "§§" + title + "§§" + content;
    }

    @Override
    public ItemProvider getItemProvider() {
        if (content.isEmpty()) return new ItemBuilder(Material.PAPER);
        return new ItemBuilder(RedisChat.getInstance().guiSettings.mailItem)
                .setDisplayName(ComponentProvider.getInstance().toBaseComponent(
                        ComponentProvider.getInstance().parse("<reset>"+title)))
                .addLoreLines(ComponentProvider.getInstance().toBaseComponent(
                        ComponentProvider.getInstance().parse("<reset>"+content)));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.closeInventory();
        //Craft written book with mail contents
        ComponentProvider.getInstance().openBook(player, Book.builder()
                .title(ComponentProvider.getInstance().parse(title))
                .addPage(ComponentProvider.getInstance().parse(content))
                .build());
    }


    public enum MailCategory {
        PRIVATE("p"),
        PUBLIC("P"),
        SENT("s"),
        ;
        private final String keyName;

        /**
         * @param keyName the name of the key
         */
        MailCategory(final String keyName) {
            this.keyName = keyName;
        }

        public static MailCategory fromString(String keyName) {
            return Arrays.stream(MailCategory.values())
                    .filter(category -> category.keyName.equals(keyName))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String toString() {
            return keyName;
        }
    }
}
