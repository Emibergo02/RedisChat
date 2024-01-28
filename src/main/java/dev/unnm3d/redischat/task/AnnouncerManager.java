package dev.unnm3d.redischat.task;

import dev.unnm3d.redischat.RedisChat;

import java.util.HashMap;


public class AnnouncerManager {
    private final HashMap<String, AnnouncerTask> announcerTasks;
    private final RedisChat plugin;

    public AnnouncerManager(RedisChat plugin) {
        this.announcerTasks = new HashMap<>();
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cancelAll();
        announcerTasks.clear();
        plugin.config.announcer.forEach(announce -> {
            AnnouncerTask at = new AnnouncerTask(plugin,
                    announce.message(),
                    announce.channelName() == null || announce.channelName().isEmpty() ? "public" : announce.channelName(),
                    announce.delay(),
                    announce.interval());
            announcerTasks.put(announce.announcementName(), at);
            at.start();
        });
    }

    public void cancelAll() {
        announcerTasks.values().forEach(AnnouncerTask::cancel);
    }

    public AnnouncerTask cancelAnnounce(String name) {
        AnnouncerTask at = announcerTasks.remove(name);
        if (at == null) return null;
        at.cancel();

        at = new AnnouncerTask(plugin, at.getMessage(), at.getChannelName(), at.getDelay(), at.getInterval());
        announcerTasks.put(name, at);
        return at;
    }

    public AnnouncerTask startAnnounce(String name) {
        AnnouncerTask at = announcerTasks.get(name);
        if (at != null) at.start();
        return at;
    }
}
