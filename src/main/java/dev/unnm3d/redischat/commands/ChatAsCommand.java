package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class ChatAsCommand {
    private RedisChat plugin;

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("chatas")
                .withPermission("redischat.chatas")
                .withArguments(
                        new PlayerArgument("player"),
                        new StringArgument("channel"),
                        new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    final Player target = (Player) args.get(0);
                    final String channelString = (String) args.get(1);
                    final String message = (String) args.get(2);
                    if (message == null) return;

                    final Channel channel = plugin.getChannelManager().getChannel(channelString)
                            .orElse(plugin.getChannelManager().getPublicChannel(null));
                    RedisChat.getScheduler().runTaskAsynchronously(() ->
                            plugin.getChannelManager().playerChannelMessage(target, channel, message));

                });
    }
}
