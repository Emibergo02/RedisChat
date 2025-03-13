package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatColorGUI;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class ChatColorCommand implements CommandExecutor, TabCompleter {
    private final RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (plugin.config.enableChatColorGUI) {
            Window.single()
                    .setTitle(plugin.guiSettings.chatColorGUITitle)
                    .setGui(new ChatColorGUI(plugin))
                    .open(player);
            return true;
        }

        if (args.length == 0) {
            plugin.messages.sendMessage(player, plugin.messages.missing_arguments);
            return true;
        }
        if(args[0].equalsIgnoreCase("reset")){
            plugin.getPlaceholderManager().removePlayerPlaceholder(player.getName(), "chat_color");
            plugin.messages.sendMessage(player, plugin.messages.color_set);
            return true;
        }
        if (!args[0].matches("#[0-9A-Fa-f]{6}")) {
            if (!getAvailableColors().contains(args[0])) {
                plugin.messages.sendMessage(player, plugin.messages.invalid_color);
                return true;
            } else if (!player.hasPermission(Permissions.CHAT_COLOR.getPermission() + "." + args[0].toLowerCase())) {
                plugin.messages.sendMessage(player, plugin.messages.noPermission);
                return true;
            }
        } else if (!player.hasPermission(Permissions.CHAT_COLOR.getPermission() + ".hex")) {
            plugin.messages.sendMessage(player, plugin.messages.noPermission);
            return true;
        }

        plugin.getPlaceholderManager().addPlayerPlaceholder(player.getName(), "chat_color", "<" + args[0] + ">");
        plugin.messages.sendMessage(player, plugin.messages.color_set);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.CHAT_COLOR.getPermission()) || plugin.config.enableChatColorGUI)
            return List.of();
        final List<String> colors = new ArrayList<>(getAvailableColors());
        colors.add("#RRGGBB");
        colors.add("reset");
        return colors.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
    }

    private Set<String> getAvailableColors() {
        return NamedTextColor.NAMES.keys();
    }
}
