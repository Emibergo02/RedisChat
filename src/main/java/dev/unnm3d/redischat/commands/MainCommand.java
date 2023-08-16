package dev.unnm3d.redischat.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import dev.unnm3d.redischat.Permission;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.utils.AdventureWebuiEditorAPI;
import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;

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
                .withPermission(Permission.REDIS_CHAT_ADMIN.getPermission())
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
                    plugin.loadYML();
                    plugin.getAnnounceManager().reload();
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
                            plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
                            return;
                        }
                        adventureWebuiEditorAPI.startSession(fieldString, "/redischat savemessage " + configField + " {token}", "RedisEconomy")
                                .thenAccept(token ->
                                        plugin.messages.sendMessage(sender, plugin.messages.editMessageClickHere
                                                .replace("%field%", configField)
                                                .replace("%url%", adventureWebuiEditorAPI.getEditorUrl(token))));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
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
                                        plugin.messages.editMessageSuccess.replace("%field%", configField));
                                plugin.saveMessages();
                            } else {
                                plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
                            }
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            plugin.messages.sendMessage(sender, plugin.messages.editMessageError);
                        }
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
                                .toList());
    }
}
