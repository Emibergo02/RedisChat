package dev.unnm3d.redischat.mail;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.events.*;
import lombok.Getter;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class MailGUIManager {

    @Getter
    private final RedisChat plugin;
    private final ConcurrentHashMap<UUID, Mail> editorMode;

    public MailGUIManager(RedisChat plugin) {
        this.plugin = plugin;
        this.editorMode = new ConcurrentHashMap<>();
    }

    /**
     * Start the editor mode for the given player
     *
     * @param sender The player to start the editor mode
     * @param target The recipient of the mail
     * @param title  The title of the mail
     * @param token  The token to open the web editor
     */
    public void startEditorMode(Player sender, String target, String title, String token) {
        final MailEditorEvent event = new MailEditorEvent(new Mail(this, sender.getName(), target, title),
                MailEditorEvent.MailEditorState.STARTED);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getPluginManager().callEvent(event);

            this.editorMode.put(sender.getUniqueId(), event.getMail());
            System.out.println("Starting editor mode for " + sender.getUniqueId() + " with mail " + event.getMail().serializeWithId());

            plugin.getComponentProvider().sendMessage(sender,
                    plugin.messages.mailEditorStart
                            .replace("%link%", plugin.getWebEditorAPI().getEditorUrl(token)));
        });

    }

    /**
     * Stop the editor mode for the given player
     * Fills the mail with the given content
     *
     * @param sender  The player to stop the editor mode
     * @param content The content of the mail
     */
    public void stopEditorMode(@NotNull Player sender, @NotNull String content) {

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            final Mail mail = editorMode.get(sender.getUniqueId());
            if (mail == null) {
                plugin.getComponentProvider().sendMessage(sender, plugin.messages.mailError);
                return;
            }
            mail.setContent(content);

            final MailEditorEvent event = new MailEditorEvent(mail, MailEditorEvent.MailEditorState.COMPLETED);
            plugin.getServer().getPluginManager().callEvent(event);
            editorMode.put(sender.getUniqueId(), event.getMail());
        });
    }

    /**
     * Preview the mail for the given player in the editor mode
     *
     * @param sender The player to preview the mail
     */
    public void previewMail(Player sender) {
        final Mail mail = editorMode.get(sender.getUniqueId());
        if (mail == null) {
            plugin.getComponentProvider().sendMessage(sender, plugin.messages.mailError);
            return;
        }
        mail.openPreview(sender, false);
    }

    /**
     * Confirm the mail for the given player in the editor mode
     * Send the mail to the recipient
     *
     * @param sender  The player to confirm the mail
     * @param confirm If the mail should be sent
     */
    public void confirmSendMail(Player sender, boolean confirm) {
        final Mail mail = editorMode.remove(sender.getUniqueId());
        System.out.println("Removing mail " + mail + " from editor mode");
        if (mail == null) {
            plugin.getComponentProvider().sendMessage(sender, plugin.messages.mailError);
            return;
        }
        if (!confirm) {
            plugin.getComponentProvider().sendMessage(sender,
                    plugin.messages.mailEditorFailed);
            return;
        }

        sendMail(mail).exceptionally(throwable -> {
            throwable.printStackTrace();
            return false;
        }).thenAccept(success -> {
            if (success) plugin.getComponentProvider().sendMessage(sender, plugin.messages.mailEditorSent);
            else plugin.getComponentProvider().sendMessage(sender, plugin.messages.mailEditorFailed);
        });

    }

    /**
     * Send mail to redis
     *
     * @param mail Mail to send
     * @return CompletionStage<Boolean> if mail was sent successfully
     */
    public CompletionStage<Boolean> sendMail(@NotNull Mail mail) {
        final MailSendEvent event = new MailSendEvent(mail);
        plugin.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) return CompletableFuture.completedFuture(false);

        if (mail.getCategory().equals(Mail.MailCategory.PUBLIC)) {
            return plugin.getDataManager().setPublicMail(event.getMail());
        }
        return plugin.getDataManager().setPlayerPrivateMail(event.getMail());
    }

    public void receiveMailUpdate(String message) {
        double mailID = Double.parseDouble(message.substring(0, message.indexOf("§§")));
        final Mail receivedMail = new Mail(this, mailID, message.substring(message.indexOf("§§") + 2));

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getPluginManager().callEvent(new MailReceivedEvent(receivedMail));
            if (receivedMail.category == Mail.MailCategory.PUBLIC) {
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    plugin.messages.sendMessage(onlinePlayer, plugin.messages.mailReceived
                            .replace("%sender%", receivedMail.getSender())
                            .replace("%title%", receivedMail.getTitle()));
                }
            } else if (receivedMail.category == Mail.MailCategory.PRIVATE) {
                final Player recipient = plugin.getServer().getPlayer(receivedMail.getReceiver());
                if (recipient != null) {
                    plugin.messages.sendMessage(recipient, plugin.messages.mailReceived
                            .replace("%sender%", receivedMail.getSender())
                            .replace("%title%", receivedMail.getTitle()));
                }
            }
        });
    }

    /**
     * Delete mail
     *
     * @param mail    Mail to delete
     * @param deleter The player who deletes the mail
     */
    public void deleteMail(@NotNull Mail mail, @NotNull Player deleter) {
        final MailDeleteEvent event = new MailDeleteEvent(mail, deleter);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
            plugin.getDataManager().deleteMail(mail);
            plugin.messages.sendMessage(deleter, plugin.messages.mailDeleted.replace("%title%", mail.getTitle()));
        });
    }

    /**
     * Change the read status of the mail
     *
     * @param mail Mail to change the read status
     */
    public void readMail(@NotNull Mail mail, @NotNull Player player, boolean read) {
        //Format mail id double 3 decimal places
        mail.setRead(read);
        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getServer().getPluginManager().callEvent(new MailReadStatusChangeEvent(mail, player)));
        plugin.getDataManager().setMailRead(player.getName(), mail);
        if (!read)
            plugin.messages.sendMessage(player, plugin.messages.mailUnRead.replace("%title%", mail.getTitle()));
    }


    /**
     * Get all public mails with the given player read status
     *
     * @param playerName The player name to get read statuses
     * @return CompletableFuture<List < Mail>> with all public mails
     */
    public CompletableFuture<List<Mail>> getPublicMails(@NotNull String playerName) {
        return plugin.getDataManager().getPublicMails(playerName);
    }

    /**
     * Get all private mails of the given player
     *
     * @param playerName The player name to get private mails
     * @return CompletableFuture<List < Mail>> with all player private mails
     */
    public CompletableFuture<List<Mail>> getPrivateMails(@NotNull String playerName) {
        return plugin.getDataManager().getPlayerPrivateMail(playerName).toCompletableFuture();
    }

    /**
     * Open the public mail gui for the given player
     *
     * @param player The player to open the gui to
     */
    public void openPublicMailGui(Player player) {
        getPublicMails(player.getName())
                .thenAccept(list -> {
                            Gui global = getMailGui(list);
                            RedisChat.getScheduler().runTask(() ->
                                    Window.single()
                                            .setTitle(plugin.guiSettings.publicMailTabTitle)
                                            .setGui(global)
                                            .open(player));
                        }
                );
    }

    /**
     * Open the private mail gui for the given player
     *
     * @param player The player to open the gui to
     */
    public void openPrivateMailGui(Player player) {
        getPrivateMails(player.getName())
                .thenAccept(list -> {
                            Gui global = getMailGui(list);
                            RedisChat.getScheduler().runTask(() ->
                                    Window.single()
                                            .setTitle(plugin.guiSettings.privateMailTabTitle)
                                            .setGui(global)
                                            .open(player));
                        }
                );
    }

    /**
     * Open the mail options gui for the given player
     *
     * @param player The player to open the gui to
     * @param mail   The mail to open the options to
     */
    public void openMailOptionsGui(@NotNull Player player, @NotNull Mail mail) {
        RedisChat.getScheduler().runTask(() ->
                Window.single()
                        .setTitle(plugin.guiSettings.mailOptionsTitle)
                        .setGui(getMailOptionsGui(mail))
                        .open(player));
    }

    private Gui getMailGui(List<Mail> list) {
        return PagedGui.items()
                .setStructure(
                        plugin.guiSettings.mailGUIStructure.toArray(new String[0])
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

    private Gui getMailOptionsGui(Mail mail) {
        return Gui.normal()
                .setStructure(
                        plugin.guiSettings.mailSettingsGUIStructure.toArray(new String[0])
                )
                .addIngredient('D', new ControlItem<>() {
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        event.setCancelled(true);
                        deleteMail(mail, player);
                        if (mail.getCategory().equals(Mail.MailCategory.PUBLIC)) {
                            openPublicMailGui(player);
                        } else {
                            openPrivateMailGui(player);
                        }
                    }

                    @Override
                    public ItemProvider getItemProvider(Gui gui) {
                        return new ItemBuilder(plugin.guiSettings.deleteButton);
                    }
                })
                .addIngredient('U', new ControlItem<>() {
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        event.setCancelled(true);
                        readMail(mail, player, false);

                        if (mail.getCategory().equals(Mail.MailCategory.PUBLIC)) {
                            openPublicMailGui(player);
                        } else {
                            openPrivateMailGui(player);
                        }
                    }

                    @Override
                    public ItemProvider getItemProvider(Gui gui) {
                        return new ItemBuilder(plugin.guiSettings.unreadButton);
                    }
                }).build();
    }


}
