package dev.unnm3d.redischat.task;

import dev.unnm3d.redischat.RedisChat;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class AnnounceManager {
    private final RedisChat plugin;
    private final List<AnnounceTask> task = new ArrayList<>();
}
