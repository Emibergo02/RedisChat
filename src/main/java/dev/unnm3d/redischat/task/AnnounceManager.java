package dev.unnm3d.redischat.task;

import dev.unnm3d.redischat.RedisChat;

import java.util.HashMap;


public class AnnounceManager {
    private final HashMap<String, AnnounceTask> task;
    private final RedisChat plugin;

    public AnnounceManager(RedisChat plugin) {
        this.task = new HashMap<>();
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cancelAll();
        task.clear();
        plugin.config.announces.forEach(announce -> {
            AnnounceTask at = new AnnounceTask(plugin, announce.message(), announce.delay(), announce.interval());
            task.put(announce.announceName(), at);
            at.start();
        });
    }

    public void cancelAll() {
        task.values().forEach(AnnounceTask::cancel);
    }

    public AnnounceTask cancelAnnounce(String name) {
        AnnounceTask at = task.get(name);
        if (at != null) at.cancel();
        return at;
    }

    public AnnounceTask startAnnounce(String name) {
        AnnounceTask at = task.get(name);
        if (at != null) at.start();
        return at;
    }
}
