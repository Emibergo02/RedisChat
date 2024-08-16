package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class FilterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AbstractFilter<? extends FiltersConfig.FilterSettings> filter;
    @Setter
    private FilterResult result;

    /**
     * Event that is called when a filter is applied to a message
     * @param filter The filter that is being applied
     * @param result The result of the filter
     */
    public FilterEvent(AbstractFilter<? extends FiltersConfig.FilterSettings> filter, FilterResult result) {
        super(!Bukkit.isPrimaryThread());
        this.filter = filter;
        this.result = result;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
