package dev.unnm3d.redischat.chat;

import org.jetbrains.annotations.NotNull;

public record ChatFormat(
        @NotNull String permission,
        @NotNull String format,
        @NotNull String private_format,
        @NotNull String receive_private_format,
        @NotNull String inventory_format,
        @NotNull String item_format,
        @NotNull String enderchest_format,
        @NotNull String mention_format,
        @NotNull String link_format,
        @NotNull String staff_chat_format,
        @NotNull String join_format,
        @NotNull String quit_format) {
}
