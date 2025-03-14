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
    @Comment({"Filters incoming private messages: Checks if the player is ignoring the sender",
            "If the player is ignoring the sender, it may send a warning message"})
    public IgnorePlayerFilter.IgnorePlayerFilterProperties ignorePlayer = (IgnorePlayerFilter.IgnorePlayerFilterProperties) DefaultSettings.IGNORE_PLAYER.getFilterSettings();
    @Comment({"Filters incoming channel messages: Checks if the player has all the permissions",
            "to see the message (if the channel is permission-enabled)"})
    public FilterSettings permission = DefaultSettings.PERMISSION.getFilterSettings();

    //OUTGOING
    public CapsFilter.CapsFilterProperties caps = (CapsFilter.CapsFilterProperties) DefaultSettings.CAPS.getFilterSettings();
    public FilterSettings spam = DefaultSettings.SPAM.getFilterSettings();
    public DuplicateFilter.DuplicateFilterProperties duplicate = (DuplicateFilter.DuplicateFilterProperties) DefaultSettings.DUPLICATE.getFilterSettings();
    @Comment({"Filters outgoing private messages: If a player is ignoring the receiver",
            "or the receiver is ignoring the sender, the message will not be sent"})
    public IgnoreFilter.IgnoreFilterProperties ignore = (IgnoreFilter.IgnoreFilterProperties) DefaultSettings.IGNORE.getFilterSettings();
    @Comment({"Filters outgoing channel messages: If a player is muted on that channel",
            "or does not have permission to write, the message will not be sent"})
    public FilterSettings mutedChannel = DefaultSettings.MUTED_CHANNEL.getFilterSettings();
    @Comment({"Filters outgoing channel messages: Replace dangerous tags with safer ones",
            "and checks if the player has permission to use them"})
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