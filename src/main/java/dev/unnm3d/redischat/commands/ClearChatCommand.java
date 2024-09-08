package dev.unnm3d.redischat.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class ClearChatCommand implements CommandExecutor {
    private final RedisChat plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        new UniversalRunnable() {
            @Override
            public void run() {
                plugin.getDataManager().sendChatMessage(
                        new ChatMessage(RedisChat.getInstance().config.clear_chat_message));
            }
        }.runTaskAsynchronously(plugin);
        return true;
    }
}
