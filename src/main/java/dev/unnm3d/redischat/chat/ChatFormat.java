package dev.unnm3d.redischat.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChatFormat {
    private String permission;
    private String format;
    private String private_format;
    private String receive_private_format;
    private String inventory_format;
    private String item_format;
    private String enderchest_format;
    private String mention_format;
    private String link_format;
    private String staff_chat_format;
}
