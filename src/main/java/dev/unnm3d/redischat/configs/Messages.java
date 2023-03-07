package dev.unnm3d.redischat.configs;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.RedisChat;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@Configuration
public final class Messages {
    private static final BukkitAudiences audiences = BukkitAudiences.create(RedisChat.getInstance());
    @Comment("Here you can configure the messages of the plugin")
    public String player_not_online = "<red>The player %player% is not online</red>";
    public String cannot_message_yourself = "<red>You cannot message yourself</red>";
    public String missing_arguments = "<red>Missing arguments</red>";
    public String action_completed_successfully = "<green>Action completed successfully</green>";
    public String announce_not_found = "<red>The announce %name% does not exist</red>";
    public String no_reply_found = "<red>You do not have any message to reply</red>";
    public String reply_not_online = "<red>%player% is not online</red>";
    public String rate_limited = "<red>You've been rate limited</red>";
    @Comment("%list% is the list of players (separated by commas)")
    public String ignoring_list = "<aqua>Player ignored</aqua><br><green>%list%</green>";
    public String ignoring_player = "<green>Ignoring %player%</green>";
    public String not_ignoring_player = "<green>Ignore removed for %player%</green>";
    public String spychat_format = "<red>%sender% said to %receiver% : %message%</red>";
    public String spychat_enabled = "<green>Spychat enabled for %player%</green>";
    public String spychat_disabled = "<red>Spychat disabled for %player%</red>";
    public String editMessageError = "<red>This config entry is not a String or doesn't exist!";
    @Comment("%url% is the url to the WebUI, %field% is the config field to edit")
    public String editMessageClickHere = "<click:open_url:%url%>Click here to edit the message %field%!</click>";
    @Comment("%field% is the config field edited")
    public String editMessageSuccess = "<green>Saved successfully %field%!";

    public void sendMessage(CommandSender p, String message) {
        audiences.sender(p).sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void sendMessage(CommandSender p, Component component) {
        audiences.sender(p).sendMessage(component);
    }

    public @Nullable Field getStringField(String name) throws NoSuchFieldException {
        Field field = getClass().getField(name);
        return field.getType().equals(String.class) ? field : null;
    }

    public @Nullable String getStringFromField(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = getStringField(fieldName);
        return field != null ? (String) field.get(this) : null;
    }

    public boolean setStringField(String fieldName, String text) throws NoSuchFieldException, IllegalAccessException {
        Field field = getStringField(fieldName);
        if (field == null) return false;
        field.set(this, text);
        return true;
    }
}
