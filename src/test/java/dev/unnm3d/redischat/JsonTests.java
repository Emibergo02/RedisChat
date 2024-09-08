package dev.unnm3d.redischat;


import com.google.gson.Gson;
import dev.unnm3d.redischat.api.objects.KnownChatEntities;
import dev.unnm3d.redischat.api.objects.AudienceType;
import dev.unnm3d.redischat.api.objects.ChannelAudience;
import dev.unnm3d.redischat.api.objects.Channel;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JsonTests {
    private static final Gson gson = new Gson();

    @Test
    @DisplayName("Channel serialization")
    public void testChannelSerialization() {
        Channel nc = Channel.builder("name")
                .format("fabrizio {message}")
                .rateLimit(10)
                .rateLimitPeriod(1000)
                .discordWebhook("webhook")
                .proximityDistance(-1)
                .filtered(false)
                .permission("perm1")
                .notificationSound("sound")
                .build();
        Channel nc2 = gson.fromJson(gson.toJson(nc), Channel.class);
        assertEquals(nc, nc2);
        System.out.println(nc);
        System.out.println(nc2);
    }

    @Test
    @DisplayName("Channel serialization")
    public void testChannelAudienceSerialization() {
        ChannelAudience nc = ChannelAudience.builder("name")
                .proximityDistance(-1)
                .permission("perm1")
                .type(AudienceType.PLAYER)
                .permission("perm2")
                .build();
        ChannelAudience nc2 = gson.fromJson(gson.toJson(nc), ChannelAudience.class);
        assertEquals(nc, nc2);
        System.out.println(nc);
        System.out.println(nc2);
    }

    @Test
    @DisplayName("ChatMessage serialization")
    public void testChatMessageSerialization() {
        ChannelAudience receiver = ChannelAudience.builder("name")
                .proximityDistance(-1)
                .permission("perm1")
                .type(AudienceType.PLAYER)
                .permission("perm2")
                .build();
        ChatMessage ncm = new ChatMessage(
                new ChannelAudience(AudienceType.PLAYER, KnownChatEntities.SERVER_SENDER.toString()),
                "{message}",
                "Hello World",
                receiver
        );
        ChatMessage ncm2 = gson.fromJson(gson.toJson(ncm), ChatMessage.class);
        assertEquals(ncm, ncm2);

        //Change a superclass field
        ncm2.getReceiver().setProximityDistance(10);
        assertNotEquals(ncm, ncm2);
    }


}
