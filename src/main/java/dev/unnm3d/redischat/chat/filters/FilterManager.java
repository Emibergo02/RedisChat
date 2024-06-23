package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.events.FilterEvent;
import dev.unnm3d.redischat.chat.filters.incoming.*;
import dev.unnm3d.redischat.chat.filters.outgoing.*;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterManager {
    private final SortedMap<FiltersConfig.FilterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings>> sendFilters;
    private final SortedMap<FiltersConfig.FilterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings>> receiveFilters;
    private final ConcurrentHashMap<CommandSender, Queue<NewChatMessage>> lastMessages;


    public FilterManager(RedisChat redisChat) {
        sendFilters = new TreeMap<>(Comparator.comparingInt(FiltersConfig.FilterSettings::getPriority));
        receiveFilters = new TreeMap<>(Comparator.comparingInt(FiltersConfig.FilterSettings::getPriority));
        lastMessages = new ConcurrentHashMap<>();
    }

    public List<AbstractFilter<? extends FiltersConfig.FilterSettings>> knownFilters() {
        return List.of(
                new DiscordFilter(),
                new IgnorePlayerFilter(),
                new PermissionFilter(),
                new PrivateFilter(),
                new SpyFilter(),
                new CapsFilter(),
                new IgnoreFilter(),
                new MutedChannelFilter(),
                new SpamFilter(),
                new TagFilter(),
                new WordBlacklistFilter()
        );
    }

    public void addFilter(@NotNull FiltersConfig.FilterSettings filterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings> filter) {
        switch (filter.getDirection()) {
            case INCOMING:
                receiveFilters.put(filterSettings, filter);
                break;
            case OUTGOING:
                sendFilters.put(filterSettings, filter);
                break;
        }
    }


    /**
     * Filters a message
     *
     * @param chatEntity The player that is sending or receiving the message
     * @param message    The message to filter
     * @param filterType The type of filter to apply, incoming or outgoing
     * @return The result of the filter
     */
    public FilterResult filterMessage(CommandSender chatEntity, NewChatMessage message, FilterType filterType) {
        final SortedMap<FiltersConfig.FilterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings>> filters = filterType == FilterType.INCOMING ? receiveFilters : sendFilters;
        FilterResult result = null;
        for (Map.Entry<FiltersConfig.FilterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings>> filter : filters.entrySet()) {
            if (result != null && result.filtered()) {
                break;
            }

            if (!filter.getKey().getAudienceWhitelist().contains(message.getReceiver().getType())) {
                continue;
            }

            if (message.getReceiver().isChannel() && !filter.getKey().getChannelWhitelist().isEmpty() &&
                    !filter.getKey().getChannelWhitelist().contains(message.getReceiver().getName())) {
                continue;
            }

            result = filter.getValue().applyWithPrevious(chatEntity,
                    result != null ? result.message() : message,
                    lastMessages.getOrDefault(chatEntity, new LinkedList<>()).toArray(new NewChatMessage[0])
            );
            RedisChat.getInstance().getServer().getPluginManager().callEvent(new FilterEvent(filter.getValue(), result));
        }

        //Save message to last messages
        lastMessages.compute(chatEntity, (player, messages) -> {
            if (messages == null) {
                messages = new CircularFifoQueue<>(5);
            }
            messages.add(message);
            return messages;
        });

        return result;
    }


    public enum FilterType {
        INCOMING,
        OUTGOING
    }
}
