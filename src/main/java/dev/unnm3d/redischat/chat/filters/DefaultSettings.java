package dev.unnm3d.redischat.chat.filters;

import dev.unnm3d.redischat.chat.filters.incoming.IgnorePlayerFilter;
import dev.unnm3d.redischat.chat.filters.outgoing.CapsFilter;
import dev.unnm3d.redischat.chat.filters.outgoing.DuplicateFilter;
import dev.unnm3d.redischat.chat.filters.outgoing.WordBlacklistFilter;
import dev.unnm3d.redischat.api.objects.AudienceType;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;

import java.util.Set;

@Getter
public enum DefaultSettings {

    IGNORE_PLAYER(new IgnorePlayerFilter.IgnorePlayerFilterProperties()),
    PERMISSION(new FiltersConfig.FilterSettings(true, 2, Set.of(AudienceType.CHANNEL), Set.of())),
    DISCORD(new FiltersConfig.FilterSettings(true, 1, Set.of(AudienceType.DISCORD), Set.of())),
    PRIVATE_OUT(new FiltersConfig.FilterSettings(true, 3, Set.of(AudienceType.PLAYER), Set.of())),
    CAPS(new CapsFilter.CapsFilterProperties()),
    DUPLICATE(new DuplicateFilter.DuplicateFilterProperties()),
    IGNORE(new FiltersConfig.FilterSettings(true, 4, Set.of(AudienceType.PLAYER), Set.of())),
    MUTED_CHANNEL(new FiltersConfig.FilterSettings(true, 5, Set.of(AudienceType.CHANNEL), Set.of())),
    TAGS(new FiltersConfig.FilterSettings(true, 9, Set.of(), Set.of())),
    WORDS(new WordBlacklistFilter.WordBlacklistFilterProperties()),
    SPAM(new FiltersConfig.FilterSettings(true, 1, Set.of(), Set.of()));

    private final FiltersConfig.FilterSettings filterSettings;

    DefaultSettings(FiltersConfig.FilterSettings filterSettings) {
        this.filterSettings = filterSettings;
    }
}
