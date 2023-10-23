package dev.unnm3d.redischat.integrations;

import dev.unnm3d.redischat.api.TagResolverIntegration;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.jetbrains.annotations.NotNull;

public class OraxenTagResolver implements TagResolverIntegration {
    @Override
    public @NotNull String parseTags(String message) {
        return AdventureUtils.parseMiniMessage(message);
    }
}
