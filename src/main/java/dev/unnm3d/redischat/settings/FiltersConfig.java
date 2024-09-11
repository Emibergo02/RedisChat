package dev.unnm3d.redischat.settings;

import de.exlll.configlib.*;
import dev.unnm3d.redischat.chat.filters.DefaultSettings;
import dev.unnm3d.redischat.chat.filters.incoming.IgnorePlayerFilter;
import dev.unnm3d.redischat.chat.filters.outgoing.*;
import dev.unnm3d.redischat.api.objects.AudienceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;


@Configuration
public final class FiltersConfig {

    //INCOMING
    public IgnorePlayerFilter.IgnorePlayerFilterProperties ignorePlayer = (IgnorePlayerFilter.IgnorePlayerFilterProperties) DefaultSettings.IGNORE_PLAYER.getFilterSettings();
    public FilterSettings permission = DefaultSettings.PERMISSION.getFilterSettings();

    //OUTGOING
    public CapsFilter.CapsFilterProperties caps = (CapsFilter.CapsFilterProperties) DefaultSettings.CAPS.getFilterSettings();
    public FilterSettings spam = DefaultSettings.SPAM.getFilterSettings();
    public DuplicateFilter.DuplicateFilterProperties duplicate = (DuplicateFilter.DuplicateFilterProperties) DefaultSettings.DUPLICATE.getFilterSettings();
    public FilterSettings ignore = DefaultSettings.IGNORE.getFilterSettings();
    public FilterSettings mutedChannel = DefaultSettings.MUTED_CHANNEL.getFilterSettings();
    public FilterSettings tags = DefaultSettings.TAGS.getFilterSettings();
    public WordBlacklistFilter.WordBlacklistFilterProperties words = (WordBlacklistFilter.WordBlacklistFilterProperties) DefaultSettings.WORDS.getFilterSettings();


    @Configuration
    @AllArgsConstructor
    @Getter
    public static class FilterSettings {
        protected boolean enabled;
        protected int priority;
        protected Set<AudienceType> audienceWhitelist;
        protected Set<String> channelWhitelist;

        public FilterSettings() {
            this(false, 1, Set.of(), Set.of());
        }
    }
}