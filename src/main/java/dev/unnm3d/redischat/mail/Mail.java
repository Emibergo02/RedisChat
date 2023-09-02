package dev.unnm3d.redischat.mail;

import dev.unnm3d.redischat.RedisChat;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.inventory.Book;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.TimeZone;

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
        this.category = receiver.equals("-Public") ? MailCategory.PUBLIC : MailCategory.PRIVATE;
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


    public String serialize() {
        return category.toString() + "§§" + sender + "§§" + receiver + "§§" + title + "§§" + content;
    }

    @Override
    public ItemProvider getItemProvider() {
        if (content.isEmpty()) return new ItemBuilder(Material.PAPER);
        ZonedDateTime cal = ZonedDateTime.ofInstant(Instant.ofEpochMilli((long) id), TimeZone.getTimeZone(RedisChat.getInstance().config.mailTimestampZone).toZoneId());

        String timestampFormat = " <aqua>" + DateTimeFormatter.ofPattern(RedisChat.getInstance().config.mailTimestampFormat).format(cal);
        return new ItemBuilder(RedisChat.getInstance().guiSettings.mailItem)
                .setDisplayName(new AdventureComponentWrapper(RedisChat.getInstance().getComponentProvider().parse("<yellow>" + title + timestampFormat)))
                .addLoreLines(new AdventureComponentWrapper(RedisChat.getInstance().getComponentProvider().parse("<grey>" + content)));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        openPreview(player);
    }

    public void openPreview(@NotNull Player player) {
        player.closeInventory();
        //Craft written book with mail contents
        RedisChat.getInstance().getComponentProvider().openBook(player, Book.builder()
                .title(RedisChat.getInstance().getComponentProvider().parse(title))
                .addPage(RedisChat.getInstance().getComponentProvider().parse(content))
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
