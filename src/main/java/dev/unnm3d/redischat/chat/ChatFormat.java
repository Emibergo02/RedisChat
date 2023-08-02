package dev.unnm3d.redischat.chat;

public record ChatFormat(
        String permission,
        String format,
        String private_format,
        String receive_private_format,
        String inventory_format,
        String mention_format,
        String link_format,
        String staff_chat_format) {
}
