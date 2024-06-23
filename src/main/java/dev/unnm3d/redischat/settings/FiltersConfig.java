package dev.unnm3d.redischat.settings;

import de.exlll.configlib.*;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterSettingsSerializer;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Configuration
public final class FiltersConfig implements ConfigValidator {

    public Map<String, FilterSettings> filters = Map.of();


    @Override
    public boolean validateConfig() {
        if (filters.isEmpty()) {
            RedisChat.getInstance().getFilterManager().knownFilters()
                    .forEach(filter -> filters.put(filter.getName(), filter.getFilterSettings()));
            return true;
        }
        return false;
    }


    @AllArgsConstructor
    @Getter
    @Polymorphic
    public static class FilterSettings {
        @Comment("If the filter is enabled at all")
        boolean enabled;
        @Comment("The default priority of the filter")
        int priority;
        @Comment("The audience type of the filter")
        Set<AudienceType> audienceWhitelist;
        @Comment("The filter channels")
        Set<String> channelWhitelist;
    }
}