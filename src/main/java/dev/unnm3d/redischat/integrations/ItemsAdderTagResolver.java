package dev.unnm3d.redischat.integrations;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;

public class ItemsAdderTagResolver implements TagResolverIntegration {
    @Override
    public String resolve(String message) {
        return FontImageWrapper.replaceFontImages(message);
    }
}
