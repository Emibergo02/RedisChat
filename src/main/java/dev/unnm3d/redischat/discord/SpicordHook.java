package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatActor;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spicord.SpicordLoader;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;

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
    public void sendDiscordMessage(Channel channel, ChatMessageInfo chatMessageInfo) {
        if (chatMessageInfo.getSender().isDiscord()) return;
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
        if (plugin.config.spicord.discordFormat() == null) {
            plugin.getLogger().warning("Discord format not found. Please regenerate the Spicord configuration section");
            return;
        }


        final Component discordComponent = chatMessageInfo.getSender().isServer() ?
                MiniMessage.miniMessage().deserialize(chatMessageInfo.getMessage()) :
                plugin.getComponentProvider()
                        .parsePlaceholders(null, //Parse placeholder for format
                                plugin.config.spicord.discordFormat()
                                        .replace("%channel%", channel.getName()) //Specific placeholders for Discord format
                                        .replace("%sender%", chatMessageInfo.getSender().getName()))
                        .replaceText(rBuilder -> //Replace %message% with the actual message component
                                rBuilder.matchLiteral("%message%")
                                        .replacement(MiniMessage.miniMessage().deserialize(chatMessageInfo.getMessage())));

        textChannel.sendMessage(new MessageCreateBuilder()
                .setAllowedMentions(List.of())// Disable mentions
                .setContent(PlainTextComponentSerializer.plainText().serialize(discordComponent))
                .setEmbeds(List.of())
                .build()).queue();
    }

    @Override
    public void onMessageReceived(DiscordBot bot, MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        for (Map.Entry<String, String> channelLink : plugin.config.spicord.spicordChannelLink().entrySet()) {
            if (channelLink.getValue().equals(event.getGuildChannel().getId())) {

                final Role highestRole;
                if (event.getMember() == null || event.getMember().getRoles().isEmpty()) {
                    highestRole = event.getGuild().getPublicRole();
                } else {
                    highestRole = event.getMember().getRoles().get(0);
                }

                plugin.getDataManager().sendChatMessage(new ChatMessageInfo(
                        new ChatActor(event.getAuthor().getName(), ChatActor.ActorType.DISCORD),
                        plugin.config.spicord.chatFormat()
                                .replace("%username%", event.getAuthor().getEffectiveName())
                                .replace("%role%", highestRole.getName()),
                        event.getMessage().getContentStripped(),
                        new ChatActor(channelLink.getKey(), ChatActor.ActorType.CHANNEL)));
            }
        }
    }

}
