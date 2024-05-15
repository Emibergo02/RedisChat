package dev.unnm3d.redischat.datamanagers.sqlmanagers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.channels.PlayerChannel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.chat.KnownChatEntities;
import dev.unnm3d.redischat.datamanagers.DataKey;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
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

public abstract class SQLDataManager implements DataManager {
    protected final RedisChat plugin;
    private final ConcurrentHashMap<String, Map.Entry<Integer, Long>> rateLimit;

    protected SQLDataManager(RedisChat plugin) {
        this.plugin = plugin;
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
                format              TEXT            default 'No format -> %message%',
                rate_limit          int             default 5,
                rate_limit_period   int             default 3,
                proximity_distance  int             default -1,
                discordWebhook      varchar(128)    default '',
                filtered            BOOLEAN         default 1,
                notificationSound   varchar(32)     default NULL
            );
            """, """
            create table if not exists player_channels
            (
                player_name  varchar(16)   not null,
                channel_name varchar(16)   not null,
                status       int default 0 not null,
                primary key (player_name, channel_name),
                constraint player_channels_channels_name_fk
                    foreign key (channel_name) references channels (name)
            );
            """, """
            create table if not exists muted_entities
            (
                entity_key      varchar(24) not null,
                entities_value  TEXT        not null,
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

    @SuppressWarnings("UnstableApiUsage")
    protected void listenPluginMessages() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", (channel, player, message) -> {
            if (!channel.equals("BungeeCord")) return;

            final ByteArrayDataInput in = ByteStreams.newDataInput(message);
            final String subchannel = in.readUTF();
            final String messageString = in.readUTF();

            if (subchannel.equals(DataKey.CHAT_CHANNEL.toString())) {
                if (plugin.config.debug) {
                    plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
                }
                plugin.getChannelManager().sendAndKeepLocal(ChatMessageInfo.deserialize(messageString));
            } else if (subchannel.equals(DataKey.GLOBAL_CHANNEL.withoutCluster())) {
                if (plugin.config.debug) {
                    plugin.getLogger().info("R1) Received message from redis: " + System.currentTimeMillis());
                }
                plugin.getChannelManager().sendAndKeepLocal(ChatMessageInfo.deserialize(messageString));
            } else if (subchannel.equals(DataKey.PLAYERLIST.toString())) {
                if (plugin.getPlayerListManager() != null)
                    plugin.getPlayerListManager().updatePlayerList(Arrays.asList(messageString.split("§")));
            } else if (subchannel.equals(DataKey.CHANNEL_UPDATE.toString())) {
                if (messageString.startsWith("delete§")) {
                    plugin.getChannelManager().updateChannel(messageString.substring(7), null);
                } else {
                    final Channel ch = Channel.deserialize(messageString);
                    plugin.getChannelManager().updateChannel(ch.getName(), ch);
                }
            } else if (subchannel.equals(DataKey.MAIL_UPDATE_CHANNEL.toString())) {
                plugin.getMailGUIManager().receiveMailUpdate(messageString);
            }else if (subchannel.equals(DataKey.MUTED_UPDATE.toString())) {
                plugin.getChannelManager().getMuteManager().serializedUpdate(messageString);
            } else if (subchannel.equals(DataKey.PLAYER_PLACEHOLDERS_UPDATE.toString())) {
                plugin.getPlaceholderManager().updatePlayerPlaceholders(messageString);
            } else if (subchannel.equals(DataKey.WHITELIST_ENABLED_UPDATE.toString())) {
                if (messageString.startsWith("D§")) {
                    plugin.getChannelManager().getMuteManager().whitelistEnabledUpdate(messageString.substring(2), false);
                } else {
                    plugin.getChannelManager().getMuteManager().whitelistEnabledUpdate(messageString, true);
                }
            }

        });
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
        if (info != null)
            if (System.currentTimeMillis() - info.getValue() > channel.getRateLimitPeriod() * 1000L) {
                this.rateLimit.remove(playerName);
                return false;
            } else {
                return true;
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
                    ON DUPLICATE KEY UPDATE `inv_serialized` = VALUES(`inv_serialized`);""")) {

                statement.setString(1, name);
                statement.setString(2, serialize(inv));
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert serialized inventory into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert serialized inventory into database", e);
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
    public CompletionStage<ItemStack> getPlayerItem(@NotNull String playerName) {
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
            return null;
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
            return null;
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
            return null;
        }, plugin.getExecutorService());
    }

    @Override
    public CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select id, serializedMail, player_name from mails
                        left join read_mails on mails.id = read_mails.mail_id and read_mails.player_name = ?
                        where recipient = ?;""")) {

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
                    mail.setCategory(Mail.MailCategory.SENT);
                    statement.setDouble(4, mail.getId() + 0.001);
                    statement.setString(5, mail.getSender());
                    statement.setString(6, mail.serialize());

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to insert serialized private mail into database: " + statement);
                    }
                    sendMailUpdate(mail);
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
                errWarn("Failed to fetch serialized public mails from the database", e);
            }
            return List.of();
        }, plugin.getExecutorService());
    }

    @Override
    public void setMailRead(@NotNull String playerName, @NotNull Mail mail) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT IGNORE INTO read_mails
                            (`player_name`, `mail_id`)
                        VALUES
                            (?,?);
                        """)) {

                    statement.setString(1, playerName);
                    statement.setDouble(2, mail.getId());
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to insert read mail into database: " + statement);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to insert read mail into database", e);
            }
        }, plugin.getExecutorService());
    }
    @Override
    public void deleteMail(@NotNull Mail mail) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        DELETE mails,read_mails 
                        FROM mails INNER JOIN read_mails 
                        ON mails.id = read_mails.mail_id and mails.id = ?;
                        """)) {

                    statement.setDouble(1, mail.getId());
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to delete mail from database: " + statement);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to delete mail from database", e);
            }
        }, plugin.getExecutorService());
    }

    @Override
    public void registerChannel(@NotNull Channel channel) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO channels
                            (`name`,`format`,`rate_limit`,`rate_limit_period`,`proximity_distance`,`discordWebhook`,`filtered`,`notificationSound`)
                        VALUES
                            (?,?,?,?,?,?,?,?)
                        ON DUPLICATE KEY UPDATE
                            `format` = VALUES(`format`),
                            `rate_limit` = VALUES(`rate_limit`),
                            `rate_limit_period` = VALUES(`rate_limit_period`),
                            `proximity_distance` = VALUES(`proximity_distance`),
                            `discordWebhook` = VALUES(`discordWebhook`),
                            `filtered` = VALUES(`filtered`),
                            `notificationSound` = VALUES(`notificationSound`);
                            """)) {

                    statement.setString(1, channel.getName());
                    statement.setString(2, channel.getFormat());
                    statement.setInt(3, channel.getRateLimit());
                    statement.setInt(4, channel.getRateLimitPeriod());
                    statement.setInt(5, channel.getProximityDistance());
                    statement.setString(6, channel.getDiscordWebhook());
                    statement.setBoolean(7, channel.isFiltered());
                    final String soundString = channel.getNotificationSound() == null ? null : channel.getNotificationSound().toString();
                    statement.setString(8, soundString);
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to register channel to database: " + statement);
                    }

                    sendChannelUpdate(channel.getName(), channel);

                    return true;
                }
            } catch (SQLException e) {
                if (e instanceof JdbcSQLIntegrityConstraintViolationException)
                    Bukkit.getLogger().warning("Channel " + channel.getName() + "already exists in database");
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
            }
            return false;
        }, plugin.getExecutorService());
    }

    private void sendChannelUpdate(String channelName, @Nullable Channel channel) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.CHANNEL_UPDATE.toString());

        if (channel == null) {
            out.writeUTF("delete§" + channelName);
        } else {
            out.writeUTF(channel.serialize());
        }

        sendPluginMessage(out.toByteArray());
    }

    public void sendPlayerPlaceholdersUpdate(String serializedPlaceholders) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.PLAYER_PLACEHOLDERS_UPDATE.toString());
        out.writeUTF(serializedPlaceholders);
        sendPluginMessage(out.toByteArray());
    }

    private void sendMailUpdate(@NotNull Mail mail) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.MAIL_UPDATE_CHANNEL.toString());
        out.writeUTF(mail.serialize());

        sendPluginMessage(out.toByteArray());
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
    public CompletionStage<@Nullable String> getActivePlayerChannel(@NotNull String playerName, Map<String, Channel> registeredChannels) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select channel_name, status from player_channels
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();

                    while (resultSet.next()) {
                        if (resultSet.getInt("status") == 1)
                            return resultSet.getString("channel_name");
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch active channel from database", e);
            }
            return "public";
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
                    final ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Forward");
                    out.writeUTF("ALL");
                    out.writeUTF(DataKey.WHITELIST_ENABLED_UPDATE.toString());
                    out.writeUTF(enabled ? playerName : "D§" + playerName);
                    sendPluginMessage(out.toByteArray());
                }
            } catch (SQLException e) {
                errWarn("Failed to update whitelist enabled player to database", e);
            }
        });
    }


    @Override
    public CompletionStage<List<PlayerChannel>> getPlayerChannelStatuses(@NotNull String playerName, Map<String, Channel> registeredChannels) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select channel_name, status from player_channels
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    final List<PlayerChannel> playerChannels = new ArrayList<>();
                    while (resultSet.next()) {
                        Channel channel = registeredChannels.get(resultSet.getString("channel_name"));
                        if (channel != null)
                            playerChannels.add(new PlayerChannel(
                                    channel,
                                    resultSet.getInt("status")));
                    }
                    return playerChannels;
                }
            } catch (SQLException e) {
                errWarn("Failed to fetch channel statuses from database", e);
            }
            return List.of();
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
                        String notificationSoundString = resultSet.getString("notificationSound");
                        channels.add(new Channel(
                                resultSet.getString("name"),
                                resultSet.getString("format"),
                                resultSet.getInt("rate_limit"),
                                resultSet.getInt("rate_limit_period"),
                                resultSet.getInt("proximity_distance"),
                                resultSet.getString("discordWebhook"),
                                resultSet.getBoolean("filtered"),
                                notificationSoundString == null ? null : Sound.valueOf(notificationSoundString)));
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
    public void setPlayerChannelStatuses(@NotNull String playerName, @NotNull Map<String, String> channelStatuses) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO player_channels (`player_name`, `channel_name`, `status`) VALUES
                                                
                        """ +
                        String.join(",", Collections.nCopies(channelStatuses.size(), "(?,?,?)")) +
                        """
                                                        
                                ON DUPLICATE KEY UPDATE status = VALUES(`status`);
                                """)) {
                    int i = 0;
                    for (Map.Entry<String, String> stringStringEntry : channelStatuses.entrySet()) {
                        statement.setString(i * 3 + 1, playerName);
                        statement.setString(i * 3 + 2, stringStringEntry.getKey());
                        statement.setInt(i * 3 + 3, Integer.parseInt(stringStringEntry.getValue()));
                        i++;
                    }
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to update channel status to database: " + statement);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to update channel status to database", e);
            }
            return null;
        }, plugin.getExecutorService());
    }

    @Override
    public void removePlayerChannelStatus(@NotNull String playerName, @NotNull String channelName) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM player_channels
                        WHERE player_name = ? and channel_name = ?;""")) {

                    statement.setString(1, playerName);
                    statement.setString(2, channelName);

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to delete channel from database: " + statement);
                    }
                }
            } catch (SQLException e) {
                errWarn("Failed to register channel to database", e);
            }
            return null;
        }, plugin.getExecutorService());
    }

    private void errWarn(String msg, Exception exception) {
        if (plugin.config.debug) {
            exception.printStackTrace();
            return;
        }
        plugin.getServer().getLogger().warning(msg);
    }


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void sendChatMessage(@NotNull ChatMessageInfo packet) {
        String publishChannel = DataKey.CHAT_CHANNEL.toString();
        if (packet.getReceiver().isChannel()) {//If it's a channel message we need to increment the rate limit
            final String chName = packet.getReceiver().getName();

            if (chName.equals(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString()))
                publishChannel = DataKey.GLOBAL_CHANNEL.withoutCluster();//Exception for staffchat: it's a global channel

            final Map.Entry<Integer, Long> info = this.rateLimit.get(packet.getSender().getName());
            if (info != null) {
                this.rateLimit.put(packet.getSender().getName(), new AbstractMap.SimpleEntry<>(info.getKey() + 1, System.currentTimeMillis()));
            } else {
                this.rateLimit.put(packet.getSender().getName(), new AbstractMap.SimpleEntry<>(1, System.currentTimeMillis()));
            }
        }

        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(publishChannel);
        out.writeUTF(packet.serialize());

        sendPluginMessage(out.toByteArray());
        plugin.getChannelManager().sendAndKeepLocal(packet);
    }

    private void sendPluginMessage(byte[] byteArray) {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) return;
        plugin.getServer().getOnlinePlayers().iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", byteArray);
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

    public void sendMutedEntityUpdate(@NotNull String entityKey, @NotNull Set<String> entitiesValue) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKey.MUTED_UPDATE.toString());
        out.writeUTF(entityKey + ";" + String.join(",", entitiesValue));

        sendPluginMessage(out.toByteArray());
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
