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
    public String player_not_online = "<yellow>RedisChat</yellow> <gray>»</gray> <red>The player %player% is not online</red>";
    public String cannot_message_yourself = "<yellow>RedisChat</yellow> <gray>»</gray> <red>You cannot message yourself</red>";
    public String missing_arguments = "<yellow>RedisChat</yellow> <gray>»</gray> <red>Missing arguments</red>";
    public String action_completed_successfully = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Action completed successfully</green>";
    public String announce_not_found = "<yellow>RedisChat</yellow> <gray>»</gray> <red>The announce %name% does not exist</red>";
    public String no_reply_found = "<yellow>RedisChat</yellow> <gray>»</gray> <red>You do not have any message to reply</red>";
    public String reply_not_online = "<yellow>RedisChat</yellow> <gray>»</gray> <red>%player% is not online</red>";
    public String rate_limited = "<yellow>RedisChat</yellow> <gray>»</gray> <red>You've been rate limited</red>";
    public String caps = "<yellow>RedisChat</yellow> <gray>»</gray> <red>Don't use caps</red>";
    @Comment("%list% is the list of players (separated by commas)")
    public String ignoring_list = "<yellow>RedisChat</yellow> <gray>»</gray> <aqua>Player ignored</aqua><br><green>%list%</green>";
    public String ignoring_player = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Ignoring %player%</green>";
    public String not_ignoring_player = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Ignore removed for %player%</green>";
    public String spychat_format = "<yellow>RedisChat</yellow> <gray>»</gray> <red>%sender% said to %receiver% : %message%</red>";
    public String spychat_enabled = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Spychat enabled for %player%</green>";
    public String spychat_disabled = "<yellow>RedisChat</yellow> <gray>»</gray> <red>Spychat disabled for %player%</red>";
    public String editMessageError = "<yellow>RedisChat</yellow> <gray>»</gray> <red>This config entry is not a String or doesn't exist!";
    @Comment("%url% is the url of the WebUI, %field% is the config field to edit")
    public String editMessageClickHere = "<yellow>RedisChat</yellow> <gray>»</gray> <click:open_url:%url%>Click here to edit the message %field%!</click>";
    @Comment("%field% is the field edited")
    public String editMessageSuccess = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Saved successfully %field%!";
    public String mailEditorStart = "<yellow>RedisChat</yellow> <gray>»</gray> <click:open_url:%link%><blue>Click here to start the mail editor!</blue></click>";
    public String mailEditorConfirm = "<yellow>RedisChat</yellow> <gray>»</gray> Valid mail. What would you do?<br><click:run_command:/rmail webui %token% confirm>[<green>Confirm, send!</green>]</click>  <click:run_command:/rmail webui %token% preview>[<aqua>Preview</aqua>]</click>  <click:run_command:/rmail webui %token% abort>[<red>Dismiss</red>]</click>";
    public String mailError = "<yellow>RedisChat</yellow> <gray>»</gray> <red>You do not have any pending mail!</red>";
    public String mailEditorSent = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Mail sent!</green>";
    public String mailEditorAbort = "<yellow>RedisChat</yellow> <gray>»</gray> <red>Mail aborted!</red>";
    public String noConsole = "<yellow>RedisChat</yellow> <gray>»</gray> <red>You cannot execute this command from console</red>";
    public String itemSet = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Item set!</green>";
    public String noPermission = "<yellow>RedisChat</yellow> <gray>»</gray> <red>You do not have permission to execute this command</red>";
    public String channelCreated = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Channel created!</green>";
    public String channelRemoved = "<yellow>RedisChat</yellow> <gray>»</gray> <red>Channel removed!</red>";
    public String channelEnabled = "<yellow>RedisChat</yellow> <gray>»</gray> <green>Channel %channel% enabled for %player%!</green>";
    public String channelDisabled = "<yellow>RedisChat</yellow> <gray>»</gray> <red>Channel %channel% disabled for %player%!</red>";
    public String publicChannelMuted = "<yellow>RedisChat</yellow> <gray>»</gray> <aqua>You silenced the public channel!</aqua>";
    public String publicChannelUnmuted = "<yellow>RedisChat</yellow> <gray>»</gray> <aqua>You unmuted the public channel!</aqua>";

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
