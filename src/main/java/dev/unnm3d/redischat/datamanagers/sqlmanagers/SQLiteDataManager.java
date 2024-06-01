package dev.unnm3d.redischat.datamanagers.sqlmanagers;

import com.zaxxer.hikari.HikariDataSource;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class SQLiteDataManager extends SQLDataManager {
    private HikariDataSource dataSource;

    public SQLiteDataManager(RedisChat plugin) {
        super(plugin);
        initialize();
    }

    @Override
    protected void initialize() throws IllegalStateException {
        // Initialize the Hikari pooled connection
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/" + plugin.config.mysql.database() + ".db");

        // Prepare database schema; make tables if they don't exist
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : getSQLSchema()) {
                    statement.execute(tableCreationStatement);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create database tables. Make sure you're running MySQL v8.0+"
                        + "and that your connecting user account has privileges to create tables.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to establish a connection to the MySQL database. "
                    + "Please check the supplied database credentials in the config file", e);
        }
    }

    @Override
    public void setReplyName(@NotNull String nameReceiver, @NotNull String requesterName) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT OR REPLACE INTO player_data
                        (`player_name`, `reply_player`)
                    VALUES
                        (?,?);
                    """)) {

                statement.setString(1, nameReceiver);
                statement.setString(2, requesterName);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert reply name into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert reply name into database", e);
        }
    }

    @Override
    public void setSpying(@NotNull String playerName, boolean spy) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT OR REPLACE INTO player_data
                        (`player_name`, `is_spying`)
                    VALUES
                        (?,?);
                    """)) {

                statement.setString(1, playerName);
                statement.setBoolean(2, spy);

                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert spy toggling into database: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to insert spy toggling into database", e);
        }
    }

    @Override
    public void setMutedEntities(@NotNull String entityKey, @NotNull Set<String> entitiesValue) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(entitiesValue.isEmpty() ? """
                    DELETE FROM muted_entities
                    WHERE entity_key = ?;""" : """
                    INSERT OR REPLACE INTO muted_entities
                        (`entity_key`, `entities_value`)
                    VALUES
                        (?,?);
                    """)) {

                statement.setString(1, entityKey);
                if (!entitiesValue.isEmpty()) {
                    statement.setString(2, String.join(",", entitiesValue));
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
    public void setPlayerPlaceholders(@NotNull String playerName, @NotNull Map<String, String> placeholders) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT OR REPLACE INTO player_placeholders
                            (`player_name`, `placeholders`)
                        VALUES
                            (?,?);
                        """)) {

                    statement.setString(1, playerName);
                    statement.setString(2, serializePlayerPlaceholders(placeholders));

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
                    INSERT OR REPLACE INTO player_data
                        (`player_name`, `inv_serialized`)
                    VALUES
                        (?,?);""")) {

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
                    INSERT OR REPLACE INTO player_data
                        (`player_name`, `item_serialized`)
                    VALUES
                        (?,?);""")) {

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
                    INSERT OR REPLACE INTO player_data
                        (`player_name`, `ec_serialized`)
                    VALUES
                        (?,?);""")) {

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
                    UPDATE player_data SET 
                    (inv_serialized, item_serialized, ec_serialized) = (NULL, NULL, NULL);""")) {
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to clear inv share cache: " + statement);
                }
            }
        } catch (SQLException e) {
            errWarn("Failed to clear inv share cache", e);
        }
    }

    @Override
    public CompletionStage<Boolean> setMailRead(@NotNull String playerName, @NotNull Mail mail) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(mail.isRead() ? """
                        INSERT OR IGNORE INTO read_mails
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
    public void registerChannel(@NotNull Channel channel) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT OR REPLACE INTO channels
                            (`name`,`format`,`rate_limit`,`rate_limit_period`,`proximity_distance`,`discordWebhook`,`filtered`,`notificationSound`)
                        VALUES
                            (?,?,?,?,?,?,?,?);
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

                }
            } catch (SQLException e) {
                if(e.getMessage().contains("Duplicate entry")) {
                    Bukkit.getLogger().warning("Channel " + channel.getName() + "already exists in database");
                }
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
            }
        }, plugin.getExecutorService());
    }

    @Override
    public void setPlayerChannelStatuses(@NotNull String playerName, @NotNull Map<String, String> channelStatuses) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT OR REPLACE INTO player_channels (`player_name`, `channel_name`, `status`) VALUES
                        """ +
                        String.join(",", Collections.nCopies(channelStatuses.size(), "(?,?,?)")) + ";"
                )) {
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
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
