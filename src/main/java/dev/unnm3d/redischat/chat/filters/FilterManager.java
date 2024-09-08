package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.events.FilterEvent;
import dev.unnm3d.redischat.chat.filters.incoming.IgnorePlayerFilter;
import dev.unnm3d.redischat.chat.filters.incoming.PermissionFilter;
import dev.unnm3d.redischat.chat.filters.outgoing.*;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChannelAudience;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class FilterManager {
    private final RedisChat plugin;
    private final SortedSet<AbstractFilter<? extends FiltersConfig.FilterSettings>> registeredFilters;
    private final ConcurrentHashMap<String, Queue<ChatMessage>> lastMessagesCache;

    public FilterManager(RedisChat plugin) {
        this.plugin = plugin;
        registeredFilters = new TreeSet<>((o1, o2) -> {
            if (o1.getFilterSettings().getPriority() > o2.getFilterSettings().getPriority()) return 1;
            if (o1.getFilterSettings().getPriority() < o2.getFilterSettings().getPriority()) return -1;
            return o1.hashCode() - o2.hashCode(); //Deny equal priority filters to be removed
        });
        lastMessagesCache = new ConcurrentHashMap<>();

        initializeDefaultFilters();
    }

    /**
     * Initializes the filters
     * If no filters are present, it will add the default filters
     * If filters are present, it will add the filters from the config
     */
    public void initializeDefaultFilters() {
        //INCOMING
        addFilter(new IgnorePlayerFilter(plugin.filterSettings.ignorePlayer));
        addFilter(new PermissionFilter(plugin, plugin.filterSettings.permission));

        //OUTGOING
        addFilter(new CapsFilter(plugin));
        addFilter(new SpamFilter(plugin, plugin.filterSettings.spam));
        addFilter(new DuplicateFilter(plugin.filterSettings.duplicate));
        addFilter(new IgnoreFilter(plugin, plugin.filterSettings.ignore));
        addFilter(new MutedChannelFilter(plugin, plugin.filterSettings.mutedChannel));
        addFilter(new TagFilter(plugin, plugin.filterSettings.tags));
        addFilter(new WordBlacklistFilter(plugin, plugin.filterSettings.words));
        for (AbstractFilter<? extends FiltersConfig.FilterSettings> registeredFilter : registeredFilters) {
            plugin.getLogger().info("Registered filter: " + registeredFilter.getName());
        }
    }

    public void addFilter(AbstractFilter<? extends FiltersConfig.FilterSettings> filter) {
        removeFilter(filter);
        registeredFilters.add(filter);
    }

    public void removeFilter(AbstractFilter<? extends FiltersConfig.FilterSettings> filter) {
        registeredFilters.remove(filter);
        registeredFilters.removeIf(value -> value.getName().equals(filter.getName()));
    }

    public Optional<AbstractFilter<? extends FiltersConfig.FilterSettings>> getFilterByName(String name) {
        return registeredFilters.stream()
                .filter(filter -> filter.getName().equals(name))
                .findFirst();
    }


    /**
     * Filters a message
     *
     * @param chatEntity The player that is sending or receiving the message
     * @param message    The message to filter
     * @param filterType The type of filter to apply, incoming or outgoing
     * @return The result of the filter
     */
    public FilterResult filterMessage(@NotNull CommandSender chatEntity, @NotNull ChatMessage message, AbstractFilter.Direction filterType) {
        FilterResult result = new FilterResult(message, false);
        final Queue<ChatMessage> lastMessages = lastMessagesCache.getOrDefault(
                genKeyIndex(filterType, message.getReceiver(), chatEntity.getName()),
                new CircularFifoQueue<>(plugin.config.last_message_count));

        if (plugin.config.debug) {
            plugin.getLogger().info("Starting filtering filterType: " + filterType + " for player: " + chatEntity.getName());
        }
        for (AbstractFilter<? extends FiltersConfig.FilterSettings> filter : registeredFilters) {
            //Skip filter if it is not the correct type
            if (!(filter.getDirection() == filterType || filter.getDirection() == AbstractFilter.Direction.BOTH)) {
                if (plugin.config.debug) {
                    plugin.getLogger().info("Skipping filter: " + filter.getName() + " because it is not the correct type");
                }
                continue;
            }

            if (!filter.getFilterSettings().isEnabled()) continue;

            if (chatEntity.hasPermission(Permissions.BYPASS_FILTER_PREFIX.getPermission() + filter.getName())) {
                if (plugin.config.debug) {
                    plugin.getLogger().info("Skipping filter: " + filter.getName() + " because the player has the bypass permission");
                }
                continue;
            }

            //If the filter is cancelled, stop filtering
            if (result != null && result.filtered()) {
                break;
            }


            if (!filter.getFilterSettings().getAudienceWhitelist().isEmpty() && !filter.getFilterSettings().getAudienceWhitelist().contains(message.getReceiver().getType())) {
                if (plugin.config.debug) {
                    plugin.getLogger().info("Skip: wrong audience type: " + message.getReceiver().getType());
                }
                continue;
            }

            if (message.getReceiver().isChannel() && !filter.getFilterSettings().getChannelWhitelist().isEmpty() &&
                    !filter.getFilterSettings().getChannelWhitelist().contains(message.getReceiver().getName())) {
                if (plugin.config.debug) {
                    plugin.getLogger().info("Skip: wrong channel: " + message.getReceiver().getName());
                }
                continue;
            }

            //Check if the channel is filtered. if not skip caps and wordblacklist
            if ((filter instanceof CapsFilter || filter instanceof WordBlacklistFilter) && message.getReceiver().isChannel()) {
                Optional<Boolean> isFiltered = plugin.getChannelManager().getChannel(message.getReceiver().getName(), null)
                        .map(Channel::isFiltered);

                if (isFiltered.isPresent() && !isFiltered.get()) continue;
            }

            result = filter.applyWithPrevious(chatEntity,
                    result != null ? result.message() : message,
                    lastMessages.toArray(new ChatMessage[0])
            );

            if (plugin.config.debug && result.filtered()) {
                plugin.getLogger().info("Filter" + filter.getName() + " filtered the message");
            }


            plugin.getServer().getPluginManager().callEvent(new FilterEvent(filter, result));
            if (plugin.config.debug) {
                plugin.getLogger().info("Filtering complete message: " + result.message() + " with filter: " + filter.getName());
            }
        }

        //Save message to last messages
        if (result != null && !result.filtered()) {
            lastMessages.add(message);
            lastMessagesCache.put(genKeyIndex(filterType, message.getReceiver(), chatEntity.getName()), lastMessages);
        }


        return result;
    }

    private String genKeyIndex(AbstractFilter.Direction direction, ChannelAudience audience, String playerName) {
        return direction.toString() + playerName + audience.getType().toString() + audience.getName();
    }

}
