package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spicord.SpicordLoader;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class SpicordHook extends SimpleAddon implements IDiscordHook {

    private final RedisChat plugin;
    private DiscordBot bot;

    public SpicordHook(RedisChat plugin) {
        super("RedisChat", "redischat", "Unnm3d", "1.0.0");
        this.plugin = plugin;

        SpicordLoader.addStartupListener((spicord) -> {
            if (spicord.getAddonManager().registerAddon(this)) {
                plugin.getLogger().info("Registered RedisChat Spicord addon");
            } else {
                plugin.getLogger().severe("Unable to register RedisChat Spicord addon");
            }
        });
    }

    @Override
    public void onLoad(DiscordBot bot) {
        this.bot = bot;
    }

    @Override
    public void onShutdown(DiscordBot bot) {
        this.bot = null;
    }

    @Override
    public void sendDiscordMessage(Channel channel, ChatMessageInfo message) {
        if (this.bot == null || this.bot.getJda() == null) {
            plugin.getLogger().warning("Unable to send message to Discord channel " + channel.getName() + ": bot not found");
            return;
        }
        final TextChannel textChannel = this.bot.getJda().getTextChannelById(
                plugin.config.spicord.spicordChannelLink().getOrDefault(channel.getName(), "0000000000000000000"));
        if (textChannel == null) {
            plugin.getLogger().warning("Unable to send message to Discord channel " + channel.getName() + ": channel not found");
            return;
        }
        textChannel.sendMessage(
                getMessage(
                        channel.getName(),
                        message.getSenderName(),
                        PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(message.getMessage()))
                )).queue();
    }

    @Override
    public void onMessageReceived(DiscordBot bot, MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        for (Map.Entry<String, String> channelLink : plugin.config.spicord.spicordChannelLink().entrySet()) {
            if (channelLink.getValue().equals(event.getGuildChannel().getId())) {
                plugin.getDataManager().sendChatMessage(ChatMessageInfo.craftChannelChatMessage(
                        event.getAuthor().getName(),
                        plugin.config.spicord.chatFormat().replace("%username%", event.getAuthor().getEffectiveName()),
                        event.getMessage().getContentStripped(),
                        channelLink.getKey()));
            }
        }
    }

    private MessageCreateData getMessage(String channelName, String senderName, String message) {
        return new MessageCreateBuilder()
                .setAllowedMentions(List.of())// Disable mentions
                .setEmbeds(List.of(new EmbedBuilder()
                        .setAuthor(senderName)
                        .setDescription(message)
                        .setColor(0x00fb9a)
                        .setFooter(channelName)
                        .setTimestamp(OffsetDateTime.now())
                        .build()))
                .build();
    }

}
