package dev.unnm3d.redischat.api.events;

import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.Filter;
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
