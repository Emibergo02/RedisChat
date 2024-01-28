package dev.unnm3d.redischat.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@AllArgsConstructor
public class BroadcastCommand {
    private final RedisChat plugin;


    public CommandAPICommand getBroadcastCommand() {
        return new CommandAPICommand("rbroadcast")
                .withAliases(plugin.config.getCommandAliases("rbroadcast"))
                .withPermission("redischat.broadcast")
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    final String message = (String) args.get(0);
                    if (message == null) return;
                    if (message.isEmpty()) return;
                    new UniversalRunnable() {
                        @Override
                        public void run() {
                            final Component component = plugin.getComponentProvider().parse(null,
                                    plugin.config.broadcast_format.replace("%message%", message),
                                    true, false, false);

                            plugin.getDataManager().sendChatMessage(new ChatMessageInfo(MiniMessage.miniMessage().serialize(component)));
                        }
                    }.runTaskAsynchronously(plugin);
                });
    }

    public CommandAPICommand getBroadcastRawCommand() {
        return new CommandAPICommand("rbroadcastraw")
                .withAliases(plugin.config.getCommandAliases("rbroadcastraw"))
                .withPermission("redischat.broadcastraw")
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    final String message = (String) args.get(0);
                    if (message == null) return;
                    if (message.isEmpty()) return;
                    new UniversalRunnable() {
                        @Override
                        public void run() {
                            final Component component = plugin.getComponentProvider().parse(null,
                                    message,
                                    true, false, false);

                            plugin.getDataManager().sendChatMessage(new ChatMessageInfo(MiniMessage.miniMessage().serialize(component)));
                        }
                    }.runTaskAsynchronously(plugin);
                });
    }


}
