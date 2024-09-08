package dev.unnm3d.redischat.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.Channel;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Optional;

@AllArgsConstructor
public class BroadcastCommand {
    private final RedisChat plugin;


    public CommandAPICommand getBroadcastCommand() {
        return new CommandAPICommand("rbroadcast")
                .withAliases(plugin.config.getCommandAliases("rbroadcast"))
                .withPermission("redischat.broadcast")
                .withArguments(new StringArgument("channel")
                        .replaceSuggestions(ArgumentSuggestions.strings(getChannelsWithPublic())))
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    final Optional<Channel> channel = plugin.getChannelManager().getChannel((String) args.get(0), sender);
                    final String message = (String) args.get(1);
                    if (message == null) return;
                    if (message.isEmpty()) return;
                    if (channel.isEmpty()) {
                        plugin.messages.sendMessage(sender, plugin.messages.channelNotFound);
                        return;
                    }
                    new UniversalRunnable() {
                        @Override
                        public void run() {
                            final Component component = plugin.getComponentProvider().parse(null,
                                    plugin.config.broadcast_format.replace("{message}", message),
                                    true, false, false);
                            plugin.getChannelManager().broadcastMessage(channel.get(), MiniMessage.miniMessage().serialize(component));
                        }
                    }.runTaskAsynchronously(plugin);
                });
    }

    public CommandAPICommand getBroadcastRawCommand() {
        return new CommandAPICommand("rbroadcastraw")
                .withAliases(plugin.config.getCommandAliases("rbroadcastraw"))
                .withPermission("redischat.broadcastraw")
                .withArguments(new StringArgument("channel")
                        .replaceSuggestions(ArgumentSuggestions.strings(getChannelsWithPublic())))
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    final Optional<Channel> channel = plugin.getChannelManager().getChannel((String) args.get(0), sender);
                    final String message = (String) args.get(1);
                    if (message == null) return;
                    if (message.isEmpty()) return;
                    if (channel.isEmpty()) {
                        plugin.messages.sendMessage(sender, plugin.messages.channelNotFound);
                        return;
                    }
                    new UniversalRunnable() {
                        @Override
                        public void run() {
                            final Component component = plugin.getComponentProvider().parse(null,
                                    message,
                                    true, false, false);

                            plugin.getChannelManager().broadcastMessage(channel.get(), MiniMessage.miniMessage().serialize(component));
                        }
                    }.runTaskAsynchronously(plugin);
                });
    }

    private String[] getChannelsWithPublic() {
        final String[] array = new String[plugin.getChannelManager().getRegisteredChannels().size() + 1];
        array[0] = "public";
        int index = 1;

        for (String s : plugin.getChannelManager().getRegisteredChannels().keySet()) {
            array[index] = s;
            index++;
        }
        return array;
    }
}
