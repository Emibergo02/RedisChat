package dev.unnm3d.redischat.datamanagers.sqlmanagers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.chat.objects.Channel;
import dev.unnm3d.redischat.chat.objects.ChatMessage;
import dev.unnm3d.redischat.datamanagers.DataKey;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class SQLDataManager extends PluginMessageManager implements DataManager {

    private final ConcurrentHashMap<String, Map.Entry<Integer, Long>> rateLimit;

    public SQLDataManager(RedisChat plugin) {
        super(plugin);
        this.rateLimit = new ConcurrentHashMap<>();
    }

    protected String[] getSQLSchema() {
        return new String[]{"""
            create table if not exists mails
            (
                id              double      not null primary key,
                recipient       varchar(16) default null,
                serializedMail  text        not null
            );
            """, """
            create table if not exists read_mails
            (
                player_name     varchar(16) not null,
                mail_id         double      not null,
                primary key (player_name, mail_id)
            );
            """, """
            create table if not exists player_data
            (
                player_name     varchar(16)     not null primary key,
                ignore_list     TEXT            default NULL,
                reply_player    varchar(16)     default NULL,
                active_channel  varchar(16)     default 'public',
                chat_color      varchar(12)     default NULL,
                is_spying       BOOLEAN         default FALSE,
                inv_serialized  MEDIUMTEXT      default NULL,
                item_serialized TEXT            default NULL,
                ec_serialized   MEDIUMTEXT      default NULL
            );
            """, """
            create table if not exists channels
            (
                name                varchar(16)     not null primary key,
                display_name        varchar(16)     not null,
                format              TEXT            ,
                rate_limit          int             default 5,
                rate_limit_period   int             default 3,
                proximity_distance  int             default -1,
                discord_webhook      varchar(128)    default '',
                filtered            BOOLEAN         default 1,
                shown_by_default    BOOLEAN         default 1,
                needs_permission    BOOLEAN         default 1,
                notification_sound   varchar(32)     default NULL
            );
            """, """
            create table if not exists muted_entities
            (
                entity_key      varchar(24)         not null,
                entities_value  TEXT       not null,
                primary key (entity_key)
            );
            """, """
            create table if not exists player_placeholders
            (
                player_name    varchar(16) not null,
                placeholders   TEXT        not null,
                primary key (player_name)
            );
            """, """
            create table if not exists whitelist_enabled_players
            (
                player_name    varchar(16) not null,
                primary key (player_name)
            );
            """
        };
    }


    protected abstract Connection getConnection() throws SQLException;

    protected abstract void initialize() throws IllegalStateException;

    @Override
    public CompletionStage<Optional<String>> getReplyName(@NotNull String requesterName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT `reply_player`
                        FROM player_data
                        WHERE `player_name`=?""")) {
                    statement.setString(1, requesterName);

                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        return Optional.ofNullable(resultSet.getString("reply_player"));
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch a reply name from the database", e);
            }
            return Optional.empty();
        }, plugin.getExecutorService());
    }

    @Override
    public void setReplyName(@NotNull String nameReceiver, @NotNull String requesterName) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_data
                        (`player_name`, `reply_player`)
                    VALUES
                        (?,?)
                    ON DUPLICATE KEY UPDATE `reply_player` = ?;""")) {

                statement.setString(1, nameReceiver);
                statement.setString(2, requesterName);
                statement.setString(3, requesterName);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert reply name into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert reply name into database", e);
        }
    }

    @Override
    public boolean isRateLimited(@NotNull String playerName, @NotNull Channel channel) {
        final Map.Entry<Integer, Long> info = this.rateLimit.get(playerName);
        if (info != null) {
            long elapsedTime = System.currentTimeMillis() - info.getValue();
            if (elapsedTime > channel.getRateLimitPeriod() * 1000L) {
                this.rateLimit.remove(playerName);
                return false;
            }
            return info.getKey() >= channel.getRateLimit();
        }
        return false;
    }

    @Override
    public CompletionStage<Boolean> isSpying(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT `is_spying`
                        FROM player_data
                        WHERE `player_name`=?""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        return resultSet.getBoolean("is_spying");
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch if player is spying from the database", e);
            }
            return false;
        }, plugin.getExecutorService());
    }

    @Override
    public void setSpying(@NotNull String playerName, boolean spy) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_data
                        (`player_name`, `is_spying`)
                    VALUES
                        (?,?)
                    ON DUPLICATE KEY UPDATE `is_spying` = ?;""")) {

                statement.setString(1, playerName);
                statement.setBoolean(2, spy);
                statement.setBoolean(3, spy);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert spy toggling into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert spy toggling into database", e);
        }
    }

    @Override
    public CompletionStage<Map<String, Set<String>>> getAllMutedEntities() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select * from muted_entities;""")) {

                    final ResultSet resultSet = statement.executeQuery();
                    final Map<String, Set<String>> mutedEntities = new HashMap<>();
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("entity_key");
                        String mutedPlayer = resultSet.getString("entities_value");
                        mutedEntities.put(playerName,
                                Arrays.stream(mutedPlayer.split(","))
                                        .collect(Collectors.toCollection(HashSet::new)));
                    }
                    return mutedEntities;
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch muted entities from the database", e);
            }
            return Map.of();
        }, plugin.getExecutorService());
    }

    @Override
    public void setMutedEntities(@NotNull String entityKey, @NotNull Set<String> entitiesValue) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(entitiesValue.isEmpty() ? """
                    DELETE FROM muted_entities
                    WHERE entity_key = ?;""" : """
                    INSERT INTO muted_entities
                        (`entity_key`, `entities_value`)
                    VALUES
                        (?,?)
                    ON DUPLICATE KEY UPDATE `entities_value` = ?;""")) {

                statement.setString(1, entityKey);
                if (!entitiesValue.isEmpty()) {
                    statement.setString(2, String.join(",", entitiesValue));
                    statement.setString(3, String.join(",", entitiesValue));
                }
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert muted entities into database: " + statement);
                }
                sendMutedEntityUpdate(entityKey, entitiesValue);
            }
        } catch (SQLException e) {
            errWarn("Failed to insert muted entities into database", e);
        }
    }

    @Override
    public CompletionStage<Map<String, String>> getPlayerPlaceholders(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT `placeholders`
                        FROM player_placeholders
                        WHERE `player_name`=?""")) {
                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        return deserializePlayerPlaceholders(resultSet.getString("placeholders"));
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch player placeholders from the database", e);
            }
            return Map.of();
        }, plugin.getExecutorService());
    }

    @Override
    public void setPlayerPlaceholders(@NotNull String playerName, @NotNull Map<String, String> placeholders) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO player_placeholders
                            (`player_name`, `placeholders`)
                        VALUES
                            (?,?)
                        ON DUPLICATE KEY UPDATE `placeholders` = ?;""")) {

                    statement.setString(1, playerName);
                    statement.setString(2, serializePlayerPlaceholders(placeholders));
                    statement.setString(3, serializePlayerPlaceholders(placeholders));
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Fat player placeholders into database");
                    }
                    sendPlayerPlaceholdersUpdate(playerName + "§;" + serializePlayerPlaceholders(placeholders));
                }
            } catch (SQLException e) {
                errWarn("Failed to insert player placeholders into database", e);
            }
        }, plugin.getExecutorService());
    }

    @Override
    public void addInventory(@NotNull String name, ItemStack[] inv) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_data
                        (`player_name`, `inv_serialized`)
                    VALUES
                        (?,?)
                    ON DUPLICATE KEY UPDATE `inv_serialized` = VALUES(`inv_serialized`);
                    """)) {

                statement.setString(1, name);
                statement.setString(2, serialize(inv));
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert serialized inventory into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert serialized inventory into database", e);
        }
        if (plugin.config.debugItemShare) {
            plugin.getLogger().info("05 Added inventory for " + name);
        }
    }


    @Override
    public void addItem(@NotNull String name, ItemStack item) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_data
                        (`player_name`, `item_serialized`)
                    VALUES
                        (?,?)
                    ON DUPLICATE KEY UPDATE `item_serialized` = VALUES(`item_serialized`);""")) {

                statement.setString(1, name);
                statement.setString(2, serialize(item));
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert serialized item into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert serialized item into database", e);
        }
        if (plugin.config.debugItemShare) {
            plugin.getLogger().info("08 Added item for " + name);
        }
    }

    @Override
    public void addEnderchest(@NotNull String name, ItemStack[] inv) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_data
                        (`player_name`, `ec_serialized`)
                    VALUES
                        (?,?)
                    ON DUPLICATE KEY UPDATE `ec_serialized` = VALUES(`ec_serialized`);""")) {

                statement.setString(1, name);
                statement.setString(2, serialize(inv));
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert serialized enderchest into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert serialized enderchest into database", e);
        }
        if (plugin.config.debugItemShare) {
            plugin.getLogger().info("10 Added enderchest for " + name);
        }
    }

    @Override
    public void clearInvShareCache() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE player_data SET inv_serialized = NULL, item_serialized = NULL, ec_serialized = NULL;
                    """)) {

                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to clear inv share cache: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to clear inv share cache", e);
        }
    }

    @Override
    public CompletionStage<@Nullable ItemStack> getPlayerItem(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select item_serialized from player_data
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        String serializedItem = resultSet.getString("item_serialized");
                        return serializedItem == null || serializedItem.isEmpty() ? new ItemStack(Material.AIR) : deserialize(serializedItem)[0];
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch serialized item from the database", e);
            }
            return new ItemStack(Material.AIR);
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<ItemStack[]> getPlayerInventory(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select inv_serialized from player_data
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        String serializedInv = resultSet.getString("inv_serialized");
                        return serializedInv == null || serializedInv.isEmpty() ? new ItemStack[0] : deserialize(serializedInv);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch serialized inventory from the database", e);
            }
            return new ItemStack[0];
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<ItemStack[]> getPlayerEnderchest(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select ec_serialized from player_data
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        String serializedEc = resultSet.getString("ec_serialized");
                        return serializedEc == null || serializedEc.isEmpty() ? new ItemStack[0] : deserialize(serializedEc);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch serialized enderchest from the database", e);
            }
            return new ItemStack[0];
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select id, serializedMail, player_name from mails
                        left join read_mails on mails.id = read_mails.mail_id and read_mails.player_name = ?
                        where recipient = ?;
                        """)) {

                    statement.setString(1, playerName);
                    statement.setString(2, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    final List<Mail> mails = new ArrayList<>();
                    while (resultSet.next()) {
                        Mail mail = new Mail(plugin.getMailGUIManager(),
                                resultSet.getDouble("id"),
                                resultSet.getString("serializedMail"));
                        mail.setRead(resultSet.getString("player_name") != null);
                        mails.add(mail);
                    }
                    return mails;
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch serialized private mails from the database", e);
            }
            return List.of();
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<Boolean> setPlayerPrivateMail(@NotNull Mail mail) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO mails
                            (`id`,`recipient`,`serializedMail`)
                        VALUES
                            (?,?,?),
                            (?,?,?);""")) {

                    statement.setDouble(1, mail.getId());
                    statement.setString(2, mail.getReceiver());
                    statement.setString(3, mail.serialize());
                    sendMailUpdate(mail);

                    mail.setCategory(Mail.MailCategory.SENT);
                    statement.setDouble(4, mail.getId() + 0.001);
                    statement.setString(5, mail.getSender());
                    statement.setString(6, mail.serialize());

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to insert serialized private mail into database: " + statement);
                    }
                    return true;
                }
            } catch (SQLException e) {
                errWarn("Failed to insert serialized private mail into database", e);
            }
            return false;
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<Boolean> setPublicMail(@NotNull Mail mail) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO mails
                            (`id`,`recipient`,`serializedMail`)
                        VALUES
                            (?,?,?);""")) {

                    statement.setDouble(1, mail.getId());
                    statement.setString(2, mail.getReceiver());
                    statement.setString(3, mail.serialize());

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to insert serialized public mail into database: " + statement);
                    }
                    sendMailUpdate(mail);
                    return true;
                }
            } catch (SQLException e) {
                errWarn("Failed to insert serialized public mail into database", e);
            }
            return false;
        }, plugin.getExecutorService());
    }

    @Override
    public CompletableFuture<List<Mail>> getPublicMails(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select id, serializedMail, player_name
                        from mails
                        left join read_mails on mails.id = read_mails.mail_id and read_mails.player_name = ?
                        where recipient = '-Public';""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    final List<Mail> mails = new ArrayList<>();
                    while (resultSet.next()) {
                        final Mail mail = new Mail(plugin.getMailGUIManager(),
                                resultSet.getDouble("id"),
                                resultSet.getString("serializedMail"));

                        mail.setRead(resultSet.getString("player_name") != null);
                        mails.add(mail);
                    }
                    return mails;
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch serialized public mails from the database", e);
            }
            return List.of();
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<Boolean> setMailRead(@NotNull String playerName, @NotNull Mail mail) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(mail.isRead() ? """
                        INSERT IGNORE INTO read_mails
                            (`player_name`, `mail_id`)
                        VALUES
                            (?,?);
                        """ : """
                        DELETE FROM read_mails
                        WHERE player_name = ? AND mail_id = ?;
                        """)) {

                    statement.setString(1, playerName);
                    statement.setDouble(2, mail.getId());
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to insert read mail into database: " + statement);
                    }
                    return true;
                }
            } catch (SQLException e) {
                errWarn("Failed to insert read mail into database", e);
            }
            return false;
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<Boolean> deleteMail(@NotNull Mail mail) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement deleteMailStatement = connection.prepareStatement(
                        "DELETE FROM mails WHERE id =?;");
                     PreparedStatement deleteReadMailStatement = connection.prepareStatement(
                             "DELETE FROM read_mails WHERE mail_id =?;")) {

                    deleteMailStatement.setDouble(1, mail.getId());
                    if (deleteMailStatement.executeUpdate() == 0) {
                        throw new SQLException("Failed to delete mail from database: " + deleteMailStatement);
                    }

                    deleteReadMailStatement.setDouble(1, mail.getId());
                    deleteReadMailStatement.executeUpdate();

                    return true;
                }
            } catch (SQLException e) {
                errWarn("Failed to delete mail from database", e);
            }
            return false;
        }, plugin.getExecutorService());
    }

    @Override
    public void registerChannel(@NotNull Channel channel) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO channels
                            (`name`,`display_name`,`format`,`rate_limit`,`rate_limit_period`,`proximity_distance`,`discord_webhook`,`filtered`,`shown_by_default`,`needs_permission`,`notification_sound`)
                        VALUES
                            (?,?,?,?,?,?,?,?,?,?,?)
                        ON DUPLICATE KEY UPDATE
                            `display_name` = VALUES(`display_name`),
                            `format` = VALUES(`format`),
                            `rate_limit` = VALUES(`rate_limit`),
                            `rate_limit_period` = VALUES(`rate_limit_period`),
                            `proximity_distance` = VALUES(`proximity_distance`),
                            `discord_webhook` = VALUES(`discord_webhook`),
                            `filtered` = VALUES(`filtered`),
                            `shown_by_default` = VALUES(`shown_by_default`),
                            `needs_permission` = VALUES(`needs_permission`),
                            `notification_sound` = VALUES(`notification_sound`);
                        """)) {

                    statement.setString(1, channel.getName());
                    statement.setString(2, channel.getDisplayName());
                    statement.setString(3, channel.getFormat());
                    statement.setInt(4, channel.getRateLimit());
                    statement.setInt(5, channel.getRateLimitPeriod());

                    statement.setInt(6, channel.getProximityDistance());
                    statement.setString(7, channel.getDiscordWebhook());
                    statement.setBoolean(8, channel.isFiltered());
                    statement.setBoolean(9, channel.isShownByDefault());
                    statement.setBoolean(10, channel.isPermissionEnabled());
                    statement.setString(11, channel.getNotificationSound());
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to register channel to database: " + statement);
                    }

                    sendChannelUpdate(channel.getName(), channel);

                }
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    Bukkit.getLogger().warning("Channel " + channel.getName() + "already exists in database");
                }
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
            }
        }, plugin.getExecutorService());
    }

    @Override
    public void unregisterChannel(@NotNull String channelName) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM channels
                        where name = ?;""")) {

                    statement.setString(1, channelName);
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to unregister channel to database: " + statement);
                    }

                    sendChannelUpdate(channelName, null);

                    return true;
                }
            } catch (SQLException e) {
                errWarn("Failed to unregister channel to database", e);
            }
            return false;
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<String> getActivePlayerChannel(@NotNull String playerName, Map<String, Channel> registeredChannels) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select active_channel from player_data
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        return resultSet.getString("active_channel");
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch active channel from database", e);
            }
            return KnownChatEntities.GENERAL_CHANNEL.toString();
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<Set<String>> getWhitelistEnabledPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select player_name from whitelist_enabled_players;""")) {

                    statement.setBoolean(1, true);
                    final ResultSet resultSet = statement.executeQuery();
                    final Set<String> players = new HashSet<>();
                    while (resultSet.next()) {
                        players.add(resultSet.getString("player_name"));
                    }
                    return players;
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch whitelist enabled players from database", e);
            }
            return Set.of();
        }, plugin.getExecutorService());
    }

    @Override
    public void setWhitelistEnabledPlayer(@NotNull String playerName, boolean enabled) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(enabled ?
                        "INSERT INTO whitelist_enabled_players (`player_name`) VALUES (?)" :
                        "DELETE FROM whitelist_enabled_players WHERE player_name = ?;"
                )) {
                    statement.setString(1, playerName);

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to update whitelist enabled player to database: " + statement);
                    }
                    sendWhitelistEnabledUpdate(playerName, enabled);

                }
            } catch (SQLException e) {
                errWarn("Failed to update whitelist enabled player to database", e);
            }
        });
    }

    @Override
    public void setActivePlayerChannel(String playerName, String channelName) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO player_data
                            (`player_name`, `active_channel`)
                        VALUES
                            (?,?)
                        ON DUPLICATE KEY UPDATE `active_channel` = VALUES(`active_channel`);""")) {

                    statement.setString(1, playerName);
                    statement.setString(2, channelName);
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to update active channel to database: " + statement);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to update active channel to database", e);
            }
        }, plugin.getExecutorService());
    }


    @Override
    public CompletionStage<List<Channel>> getChannels() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select * from channels;""")) {

                    final ResultSet resultSet = statement.executeQuery();
                    final List<Channel> channels = new ArrayList<>();
                    while (resultSet.next()) {
                        channels.add(Channel.builder(resultSet.getString("name"))
                                .displayName(resultSet.getString("display_name"))
                                .rateLimit(resultSet.getInt("rate_limit"))
                                .rateLimitPeriod(resultSet.getInt("rate_limit_period"))
                                .discordWebhook(resultSet.getString("discord_webhook"))
                                .filtered(resultSet.getBoolean("filtered"))
                                .shownByDefault(resultSet.getBoolean("shown_by_default"))
                                .permissionEnabled(resultSet.getBoolean("needs_permission"))
                                .notificationSound(resultSet.getString("notification_sound"))
                                .build());
                    }
                    return channels;
                }
            } catch (SQLException e) {
                errWarn("Failed fetch channels from database", e);
            }
            return List.of();
        }, plugin.getExecutorService());
    }


    @Override
    public void sendChatMessage(@NotNull ChatMessage packet) {
        String publishChannel = DataKey.CHAT_CHANNEL.toString();
        if (packet.getReceiver().isChannel()) {//If it's a channel message we need to increment the rate limit
            final String chName = packet.getReceiver().getName();

            if (chName.equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString()))
                publishChannel = DataKey.GLOBAL_CHANNEL.withoutCluster();//Exception for staffchat: it's a global channel

            if (this.rateLimit.computeIfPresent(packet.getSender().getName(), (key, value) ->
                    new AbstractMap.SimpleEntry<>(value.getKey() + 1, System.currentTimeMillis())) == null) {
                //If the map doesn't contain the player, we add it
                this.rateLimit.put(packet.getSender().getName(), new AbstractMap.SimpleEntry<>(1, System.currentTimeMillis()));
            }
        }

        sendChatPluginMessage(publishChannel, packet);
        plugin.getChannelManager().sendGenericChat(packet);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void publishPlayerList(@NotNull List<String> playerNames) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.PLAYERLIST.toString());
        out.writeUTF(String.join("§", playerNames));

        sendPluginMessage(out.toByteArray());

        if (plugin.getPlayerListManager() != null)
            plugin.getPlayerListManager().updatePlayerList(playerNames);
    }

    protected void errWarn(String msg, Exception exception) {
        if (plugin.config.debug) {
            exception.printStackTrace();
            return;
        }
        plugin.getServer().getLogger().warning(msg);
    }

    @Override
    public void sendRejoin(@NotNull String playerName) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.REJOIN_CHANNEL.toString());
        out.writeUTF(playerName);

        sendPluginMessage(out.toByteArray());
    }
}
