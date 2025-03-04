package dev.unnm3d.redischat.commands;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.ChannelAudience;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class TalkOnCommand implements CommandExecutor, TabCompleter {
    private RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            plugin.getComponentProvider().sendMessage(sender, plugin.messages.noConsole);
            return true;
        }

        if (args.length == 0) {
            plugin.getComponentProvider().sendMessage(sender, plugin.messages.missing_arguments);
            return true;
        }

        if (getAvailableChannelNames(sender).noneMatch(name -> name.equalsIgnoreCase(args[0])) && !args[0].equalsIgnoreCase("public")) {
            plugin.getComponentProvider().sendMessage(sender, plugin.messages.channelNotFound);
            return true;
        }

        plugin.getChannelManager().setActiveChannel(sender.getName(), args[0].equalsIgnoreCase("public") ? null : args[0]);
        plugin.messages.sendMessage(sender, plugin.messages.channelTalk
                .replace("%channel%", args[0])
        );

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getAvailableChannelNames(sender)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                        if ("public".startsWith(args[0].toLowerCase())) list.add("public");
                        return list;
                    }));
        }
        return List.of();
    }

    public Stream<String> getAvailableChannelNames(CommandSender sender) {
        return plugin.getChannelManager().getAllChannels().stream()
                .filter(channel -> {
                    if (channel.isShownByDefault()) {
                        return sender.isOp() || !sender.hasPermission(Permissions.CHANNEL_HIDE_PREFIX.getPermission() + channel.getName());
                    }
                    return sender.hasPermission(Permissions.CHANNEL_SHOW_PREFIX.getPermission() + channel.getName());

                })
                .map(ChannelAudience::getName);
    }
}
