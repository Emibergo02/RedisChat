package dev.unnm3d.redischat.settings;

import de.exlll.configlib.*;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;


@Configuration
public final class FiltersConfig {

    public Set<FilterSettings> filters = new HashSet<>();


    @Configuration
    @AllArgsConstructor
    @Getter
    @Polymorphic(property = "propertyClass")
    public static class FilterSettings {
        @Comment("The name of the filter")
        protected String filterName;
        @Comment("If the filter is enabled at all")
        protected boolean enabled;
        @Comment("The default priority of the filter")
        protected int priority;
        @Comment("The audience type of the filter, if empty it will be applied to all")
        protected Set<AudienceType> audienceWhitelist;
        @Comment("The filter channels,if empty it will be applied to public channel only")
        protected Set<String> channelWhitelist;

        public FilterSettings() {
            this("empty",false, 1, Set.of(), Set.of());
        }
    }
}