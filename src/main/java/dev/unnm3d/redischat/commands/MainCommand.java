package dev.unnm3d.redischat.commands;

import de.exlll.configlib.ConfigurationException;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.settings.Config;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
public class MainCommand {
    private final RedisChat plugin;
    private final AdventureWebuiEditorAPI adventureWebuiEditorAPI;


    public CommandAPICommand getCommand() {
        return new CommandAPICommand("redischat")
                .withPermission(Permissions.ADMIN.getPermission())
                .withSubcommand(getReloadSubcommand())
                .withSubcommand(getEditMessageSubcommand())
                .withSubcommand(getSaveMessageSubcommand())
                .executes((sender, args) -> {
                    plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                }, ExecutorType.PLAYER, ExecutorType.CONSOLE);

    }

    public CommandAPICommand getReloadSubcommand() {
        return new CommandAPICommand("reload")
                .executes((commandExecutor) -> {
                    if (plugin.config.enableQuitJoinMessages) {
                        if (plugin.config.getDataType() == Config.DataType.REDIS) {
                            if (plugin.getJoinQuitManager() == null)
                                plugin.getServer().getPluginManager().registerEvents(plugin.getJoinQuitManager(), plugin);
                        } else {
                            plugin.getLogger().warning("Join/Quit messages are not supported with H2 or MySQL");
                        }
                    } else if (plugin.getJoinQuitManager() != null) {
                        HandlerList.unregisterAll(plugin.getJoinQuitManager());
                    }
                    try {
                        plugin.loadYML();
                    } catch (ConfigurationException e) {
                        plugin.getLogger().severe("config.yml or messages.yml or guis.yml is invalid! Please regenerate them (starting from config.yml: " + e.getMessage());
                    }
                    plugin.getAnnouncerManager().reload();
                    plugin.getChannelManager().updateChannels();
                    plugin.getChannelManager().getMuteManager().reload();
                    plugin.getComponentProvider().sendMessage(commandExecutor.sender(), "<green>Config reloaded");
                }, ExecutorType.CONSOLE, ExecutorType.PLAYER);
    }

    public CommandAPICommand getEditMessageSubcommand() {
        return new CommandAPICommand("editmessage")
                .withArguments(new StringArgument("configField")
                        .replaceSuggestions(ArgumentSuggestions.stringCollectionAsync(getConfigFields()))
                )
                .executes((sender, args) -> {
                    try {
                        String configField = (String) args.get("configField");
                        String fieldString = plugin.messages.getStringFromField(configField);
                        if (configField == null) {
                            plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                            return;
                        }
                        if (fieldString == null) {
                            plugin.messages.sendMessage(sender, plugin.messages.edit_message_error);
                            return;
                        }
                        adventureWebuiEditorAPI.startSession(fieldString, "/redischat savemessage " + configField + " {token}", "RedisEconomy")
                                .thenAccept(token ->
                                        plugin.messages.sendMessage(sender, plugin.messages.edit_message_click_here
                                                .replace("%field%", configField)
                                                .replace("%url%", adventureWebuiEditorAPI.getEditorUrl(token))))
                                .exceptionally(throwable -> {
                                    plugin.messages.sendMessage(sender, plugin.messages.edit_message_error);
                                    throwable.printStackTrace();
                                    return null;
                                });
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        plugin.messages.sendMessage(sender, plugin.messages.edit_message_error);
                    }
                }, ExecutorType.CONSOLE, ExecutorType.PLAYER);
    }

    public CommandAPICommand getSaveMessageSubcommand() {
        return new CommandAPICommand("savemessage")
                .withArguments(new StringArgument("configField")
                        .replaceSuggestions(ArgumentSuggestions.stringCollectionAsync(getConfigFields()))
                )
                .withArguments(new StringArgument("token"))
                .executes((sender, args) -> {
                    String configField = (String) args.get("configField");
                    String token = (String) args.get("token");
                    if (configField == null || token == null) {
                        plugin.messages.sendMessage(sender, plugin.messages.missing_arguments);
                        return;
                    }
                    adventureWebuiEditorAPI.retrieveSession(token).thenAccept(message -> {
                        try {
                            if (plugin.messages.setStringField(configField, message)) {
                                plugin.messages.sendMessage(sender,
                                        plugin.messages.edit_message_field.replace("%field%", configField));
                                plugin.saveMessages();
                            } else {
                                plugin.messages.sendMessage(sender, plugin.messages.edit_message_error);
                            }
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            plugin.messages.sendMessage(sender, plugin.messages.edit_message_error);
                        }
                    }).exceptionally(throwable -> {
                        plugin.messages.sendMessage(sender, plugin.messages.edit_message_error);
                        throwable.printStackTrace();
                        return null;
                    });
                });
    }

    private Function<SuggestionInfo<CommandSender>, CompletableFuture<Collection<String>>> getConfigFields() {
        return commandSenderSuggestionInfo ->
                CompletableFuture.supplyAsync(() ->
                                Arrays.stream(plugin.messages.getClass().getFields())
                                        .filter(field -> field.getType().equals(String.class))
                                        .map(Field::getName)
                                        .filter(fieldName -> fieldName.startsWith(commandSenderSuggestionInfo.currentArg()))
                                        .toList(),
                        plugin.getExecutorService());
    }
}
