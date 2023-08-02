package dev.unnm3d.redischat.integrations;

import dev.unnm3d.redischat.api.TagResolverIntegration;
import io.th0rgal.oraxen.utils.AdventureUtils;

public class OraxenTagResolver implements TagResolverIntegration {
    @Override
    public String parseTags(String message) {
        return AdventureUtils.parseMiniMessage(message, AdventureUtils.OraxenTagResolver);
    }
}
