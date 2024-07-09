package dev.unnm3d.redischat;


import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.chat.objects.AudienceType;
import dev.unnm3d.redischat.chat.objects.ChannelAudience;
import dev.unnm3d.redischat.chat.objects.Channel;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JsonTests {

    @Test
    @DisplayName("Channel serialization")
    public void testChannelSerialization() {
        Channel nc = Channel.channelBuilder("name")
                .format("fabrizio %message%")
                .rateLimit(10)
                .rateLimitPeriod(1000)
                .discordWebhook("webhook")
                .proximityDistance(-1)
                .filtered(false)
                .permission("perm1")
                .notificationSound("sound")
                .build();
        Channel nc2 = Channel.deserialize(nc.serialize());
        assertEquals(nc, nc2);
        System.out.println(nc);
        System.out.println(nc2);
    }

    @Test
    @DisplayName("Channel serialization")
    public void testChannelAudienceSerialization() {
        ChannelAudience nc = ChannelAudience.audienceBuilder("name")
                .proximityDistance(-1)
                .permission("perm1")
                .type(AudienceType.PLAYER)
                .permission("perm2")
                .build();
        ChannelAudience nc2 = ChannelAudience.deserialize(nc.serialize());
        assertEquals(nc, nc2);
        System.out.println(nc);
        System.out.println(nc2);
    }

    @Test
    @DisplayName("ChatMessage serialization")
    public void testChatMessageSerialization() {
        ChannelAudience receiver = ChannelAudience.audienceBuilder("name")
                .proximityDistance(-1)
                .permission("perm1")
                .type(AudienceType.PLAYER)
                .permission("perm2")
                .build();
        ChatMessage ncm = new ChatMessage(
                new ChannelAudience(KnownChatEntities.SERVER_SENDER.toString(), AudienceType.PLAYER),
                "%message%",
                "Hello World",
                receiver
        );
        ChatMessage ncm2 = ChatMessage.deserialize(ncm.serialize());
        assertEquals(ncm, ncm2);

        //Change a superclass field
        ncm2.getReceiver().setProximityDistance(10);
        assertNotEquals(ncm, ncm2);
    }


}
