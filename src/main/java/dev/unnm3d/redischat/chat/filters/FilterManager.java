package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.events.FilterEvent;
import dev.unnm3d.redischat.chat.filters.incoming.*;
import dev.unnm3d.redischat.chat.filters.outgoing.*;
import dev.unnm3d.redischat.chat.objects.NewChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterManager {
    private final RedisChat plugin;
    private final SortedSet<AbstractFilter<? extends FiltersConfig.FilterSettings>> registeredFilters;
    private final ConcurrentHashMap<CommandSender, Queue<NewChatMessage>> lastMessages;


    public FilterManager(RedisChat plugin) {
        registeredFilters = new TreeSet<>((o1, o2) -> {
            if (o1.getFilterSettings().getPriority() > o2.getFilterSettings().getPriority()) return 1;
            if (o1.getFilterSettings().getPriority() < o2.getFilterSettings().getPriority()) return -1;
            return o1.hashCode() - o2.hashCode(); //Deny equal priority filters to be removed
        });
        lastMessages = new ConcurrentHashMap<>();
        this.plugin = plugin;
        initializeDefaultFilters();
    }

    /**
     * Initializes the filters
     * If no filters are present, it will add the default filters
     * If filters are present, it will add the filters from the config
     */
    public void initializeDefaultFilters() {
        //INCOMING
        addFilter(new DiscordFilter(plugin.filterSettings.discord));
        addFilter(new IgnorePlayerFilter(plugin.filterSettings.ignorePlayer));
        addFilter(new PermissionFilter(plugin.filterSettings.permission));
        addFilter(new PrivateFilter(plugin.filterSettings.privateFilter));
        addFilter(new SpyFilter(plugin, plugin.filterSettings.spy));

        //OUTGOING
        addFilter(new CapsFilter(plugin.filterSettings.caps));
        addFilter(new DuplicateFilter(plugin.filterSettings.duplicate));
        addFilter(new IgnoreFilter(plugin.filterSettings.ignore));
        addFilter(new MutedChannelFilter(plugin, plugin.filterSettings.mutedChannel));
        addFilter(new ParseContentFilter(plugin.filterSettings.content));
        addFilter(new TagFilter(plugin, plugin.filterSettings.tags));
        addFilter(new WordBlacklistFilter(plugin, plugin.filterSettings.words));
        for (AbstractFilter<? extends FiltersConfig.FilterSettings> registeredFilter : registeredFilters) {
            plugin.getLogger().info("Registered filter: " + registeredFilter.getName());
        }
    }

    public void addFilter(AbstractFilter<? extends FiltersConfig.FilterSettings> filter) {
        registeredFilters.removeIf(value -> value.getName().equals(filter.getName()));
        registeredFilters.add(filter);
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
    public FilterResult filterMessage(CommandSender chatEntity, NewChatMessage message, AbstractFilter.Direction filterType) {
        FilterResult result = null;

        for (AbstractFilter<? extends FiltersConfig.FilterSettings> filter : registeredFilters) {
            //Skip filter if it is not the correct type
            if (!(filter.getDirection() == filterType || filter.getDirection() == AbstractFilter.Direction.BOTH))
                continue;

            if (!filter.getFilterSettings().isEnabled()) continue;

            if (!chatEntity.isOp() && chatEntity.hasPermission(Permissions.BYPASS_FILTER_PREFIX.getPermission() + filter.getName()))
                continue;

            //If the filter is cancelled, stop filtering
            if (result != null && result.filtered()) {
                break;
            }


            if (!filter.getFilterSettings().getAudienceWhitelist().isEmpty() && !filter.getFilterSettings().getAudienceWhitelist().contains(message.getReceiver().getType())) {
                continue;
            }

            if (message.getReceiver().isChannel() && !filter.getFilterSettings().getChannelWhitelist().isEmpty() &&
                    !filter.getFilterSettings().getChannelWhitelist().contains(message.getReceiver().getName())) {
                continue;
            }


            result = filter.applyWithPrevious(chatEntity,
                    result != null ? result.message() : message,
                    lastMessages.getOrDefault(chatEntity, new LinkedList<>()).toArray(new NewChatMessage[0])
            );
            RedisChat.getInstance().getServer().getPluginManager().callEvent(new FilterEvent(filter, result));
            if (plugin.config.debug) {
                plugin.getLogger().info("Filtered message: " + result.message().serialize() + " with filter: " + filter.getName());
            }
        }

        //Save message to last messages
        lastMessages.compute(chatEntity, (player, messages) -> {
            if (messages == null) {
                messages = new CircularFifoQueue<>(5);
            }
            messages.add(message);
            return messages;
        });

        if (result == null) {
            return new FilterResult(message, true, Optional.of(plugin.getComponentProvider().parse(chatEntity,
                    plugin.messages.not_filtered,
                    true,
                    false,
                    false)));
        }


        return result;
    }

}
