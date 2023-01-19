package dev.unnm3d.redischat.task;

import dev.unnm3d.redischat.RedisChat;
import org.bukkit.scheduler.BukkitRunnable;


public class AnnounceTask extends BukkitRunnable {

    private final RedisChat plugin;
    private final String message;

    public AnnounceTask(RedisChat plugin, String message) {
        this.plugin = plugin;
        this.message = message;
    }

    @Override
    public void run() {

    }
}
