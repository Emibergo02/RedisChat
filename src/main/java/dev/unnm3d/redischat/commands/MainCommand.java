package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class MainCommand implements CommandExecutor, TabCompleter {
    private final RedisChat plugin;
    private final AdventureWebuiEditorAPI adventureWebuiEditorAPI;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Permission.REDIS_CHAT_ADMIN.getPermission())) return true;
        if (args.length < 1) return true;

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.loadYML();
            plugin.getAnnounceManager().reload();
            plugin.getComponentProvider().sendMessage(sender, "<green>Config reloaded");
            return true;
        }

        if (args.length < 2) {
            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
            return true;
        }

        String configField = args[1];

        if (args[0].equalsIgnoreCase("savemessage")) {
            if (args.length < 3) return true;
            adventureWebuiEditorAPI.retrieveSession(args[2]).thenAccept(message -> {
                try {
                    if (plugin.messages.setStringField(configField, message)) {
                        plugin.messages.sendMessage(sender, plugin.messages.editMessageSuccess.replace("%field%", configField));
                        plugin.saveMessages();
                    } else {
                        plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
                    }
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
                }
            });
        } else if (args[0].equalsIgnoreCase("editmessage")) {
            try {
                String fieldString = plugin.messages.getStringFromField(configField);
                if (fieldString == null) {
                    plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
                    return true;
                }
                adventureWebuiEditorAPI.startSession(fieldString, "/redischat savemessage " + configField + " {token}", "RedisEconomy")
                        .thenAccept(token ->
                                plugin.messages.sendMessage(sender, plugin.messages.editMessageClickHere
                                        .replace("%field%", configField)
                                        .replace("%url%", adventureWebuiEditorAPI.getEditorUrl(token))));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1 && sender.hasPermission(Permission.REDIS_CHAT_ADMIN.getPermission())) list.add("reload");
        if (sender.hasPermission(Permission.REDIS_CHAT_ADMIN_EDIT.getPermission())) {
            if (args.length == 1) list.add("editmessage");
            else if (args.length == 2) {
                return Arrays.stream(plugin.messages.getClass().getFields()).filter(field -> field.getType().equals(String.class)).map(Field::getName).toList();
            }
        }
        return list;
    }
}
