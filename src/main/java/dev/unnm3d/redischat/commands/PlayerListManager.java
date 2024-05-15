package dev.unnm3d.redischat.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.VanishIntegration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListManager {
    private final MyScheduledTask task;
    private final ConcurrentHashMap<String, Long> playerList;
    private final List<VanishIntegration> vanishIntegrations;


    public PlayerListManager(RedisChat plugin) {
        this.playerList = new ConcurrentHashMap<>();
        this.vanishIntegrations = new ArrayList<>();
        this.task = new UniversalRunnable() {
            @Override
            public void run() {
                playerList.entrySet().removeIf(stringLongEntry -> System.currentTimeMillis() - stringLongEntry.getValue() > 1000 * 4);

                final List<String> tempList = plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> !isVanished(player))
                        .map(HumanEntity::getName)
                        .filter(s -> !s.isEmpty())
                        .toList();
                if (!tempList.isEmpty())
                    plugin.getDataManager().publishPlayerList(tempList);
                tempList.forEach(s -> playerList.put(s, System.currentTimeMillis()));

                plugin.getServer().getOnlinePlayers().forEach(player ->
                        player.setCustomChatCompletions(getPlayerList(player)));
            }
        }.runTaskTimerAsynchronously(plugin, 0, 60);//3 seconds
    }

    public void updatePlayerList(List<String> inPlayerList) {
        long currentTimeMillis = System.currentTimeMillis();
        inPlayerList.forEach(s -> {
            if (s != null && !s.isEmpty())
                playerList.put(s, currentTimeMillis);
        });
    }

    public void addVanishIntegration(VanishIntegration vanishIntegration) {
        vanishIntegrations.add(vanishIntegration);
    }

    public void removeVanishIntegration(VanishIntegration vanishIntegration) {
        vanishIntegrations.remove(vanishIntegration);
    }

    public Set<String> getPlayerList(@Nullable CommandSender sender) {
        final Set<String> keySet = new HashSet<>(playerList.keySet());

        if (sender != null) {
            vanishIntegrations.forEach(vanishIntegration ->
                    keySet.removeIf(pName -> !vanishIntegration.canSee(sender, pName)));
        }
        return keySet;
    }

    public boolean isVanished(Player player) {
        return player.getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean) ||
                vanishIntegrations.stream().anyMatch(vanishIntegration -> vanishIntegration.isVanished(player));
    }

    public void stop() {
        task.cancel();
    }

}
