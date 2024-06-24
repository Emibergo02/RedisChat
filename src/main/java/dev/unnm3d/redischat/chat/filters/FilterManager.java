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
    private final RedisChat plugin;
    private final SortedMap<FiltersConfig.FilterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings>> registeredFilters;
    private final ConcurrentHashMap<CommandSender, Queue<NewChatMessage>> lastMessages;


    public FilterManager(RedisChat plugin) {
        registeredFilters = new TreeMap<>(Comparator.comparingInt(FiltersConfig.FilterSettings::getPriority));
        lastMessages = new ConcurrentHashMap<>();
        this.plugin = plugin;

        initializeFilters();
    }

    /**
     * Initializes the filters
     * If no filters are present, it will add the default filters
     * If filters are present, it will add the filters from the config
     */
    public void initializeFilters() {
        if (this.plugin.filterSettings.filters.isEmpty()) {
            for (AbstractFilter<? extends FiltersConfig.FilterSettings> knownFilter : knownFilters()) {
                addFilter(knownFilter, knownFilter.getFilterSettings());
                this.plugin.filterSettings.filters.add(knownFilter.getFilterSettings());
            }
            this.plugin.saveFilters();
        }
        for (FiltersConfig.FilterSettings filter : this.plugin.filterSettings.filters) {
            AbstractFilter<? extends FiltersConfig.FilterSettings> knownFilter = knownFilters().stream()
                    .filter(f -> f.getName().equals(filter.getFilterName()))
                    .findFirst()
                    .orElse(null);
            if (knownFilter == null) {
                continue;
            }
            addFilter(knownFilter, filter);
        }
    }

    public void addFilter(AbstractFilter<? extends FiltersConfig.FilterSettings> filter, @NotNull FiltersConfig.FilterSettings filterSettings) {
        registeredFilters.entrySet().stream().filter(value -> value.getValue().getName().equals(filter.getName()))
                .findFirst()
                .ifPresent(value -> registeredFilters.remove(value.getKey()));
        registeredFilters.put(filterSettings, filter);
    }


    /**
     * Filters a message
     *
     * @param chatEntity The player that is sending or receiving the message
     * @param message    The message to filter
     * @param filterType The type of filter to apply, incoming or outgoing
     * @return The result of the filter
     */
    public FilterResult filterMessage(CommandSender chatEntity, NewChatMessage message, AbstractFilter.Direction filterType) {
        FilterResult result = null;

        for (Map.Entry<FiltersConfig.FilterSettings, AbstractFilter<? extends FiltersConfig.FilterSettings>> filter : registeredFilters.entrySet()) {
            //Skip filters for the wrong direction
            if (filter.getValue().getDirection() != filterType) continue;

            if (!filter.getKey().isEnabled()) continue;

            //If the filter is cancelled, stop filtering
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


    public static Set<AbstractFilter<? extends FiltersConfig.FilterSettings>> knownFilters() {
        return Set.of(
                new DiscordFilter(),
                new IgnorePlayerFilter(),
                new PermissionFilter(),
                new PrivateFilter(),
                new SpyFilter(),
                new CapsFilter(),
                new IgnoreFilter(),
                new MutedChannelFilter(),
                new ParseContentFilter(),
                new SpamFilter(),
                new TagFilter(),
                new WordBlacklistFilter()
        );
    }
}
