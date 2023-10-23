package dev.unnm3d.redischat.discord;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.spicord.Spicord;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;

import java.util.Map;

public class SpicordHook extends SimpleAddon implements IDiscordHook {

    private final RedisChat plugin;
    private final DiscordBot bot;

    public SpicordHook(RedisChat plugin) {
        super("RedisChat", "redischat", "Unnm3d","1.0.0");
        this.plugin = plugin;

        // Register addon
        if (Spicord.getInstance().getAddonManager().registerAddon(this)) {
            plugin.getLogger().info("Registered RedisChat Spicord addon");
        } else {
            plugin.getLogger().severe("Unable to register RedisChat Spicord addon");
        }

        this.bot = getSpicord().getBotByName(plugin.config.spicord.botName());
    }

    @Override
    public void sendDiscordMessage(Channel channel, ChatMessageInfo message) {
        final TextChannel textChannel = this.bot.getJda().getTextChannelById(
                plugin.config.spicord.spicordChannelLink().getOrDefault(channel.getName(), "0000000000000000000"));
        if (textChannel == null) {
            plugin.getLogger().warning("Unable to send message to Discord channel " + channel.getName() + ": channel not found");
            return;
        }
        textChannel.sendMessageEmbeds(
                buildFromJson(
                        getDiscordMessageJson(plugin, channel, message))).queue();
    }

    @Override
    public void onMessageReceived(DiscordBot bot, MessageReceivedEvent event) {
        for (Map.Entry<String, String> channelLink : plugin.config.spicord.spicordChannelLink().entrySet()) {
            System.out.println(channelLink.getKey() + " " + channelLink.getValue());
            if (channelLink.getValue().equals(event.getGuildChannel().getId())) {
                plugin.getDataManager().sendChatMessage(new ChatMessageInfo(
                        event.getAuthor().getName(),
                        "%message%",
                        event.getMessage().getContentStripped()
                ));
            }
        }
    }

    private MessageEmbed buildFromJson(String jsonString) {
        MessageEmbed embed = EmbedBuilder.fromData(DataObject.fromJson(jsonString)).build();
        System.out.println(jsonString);
        return embed;
    }
}
