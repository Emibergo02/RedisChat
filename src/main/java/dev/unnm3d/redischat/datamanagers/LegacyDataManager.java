package dev.unnm3d.redischat.datamanagers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.zaxxer.hikari.HikariDataSource;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class LegacyDataManager implements DataManager {
    private final RedisChat plugin;
    private HikariDataSource dataSource;
    private final ConcurrentHashMap<String, Map.Entry<Integer, Long>> rateLimit;
    private static final String[] TABLE_DDL = {"""
            create table if not exists mails
            (
                id             double      not null,
                recipient      varchar(16) null,
                serializedMail text        not null,
                primary key(id)
            );
            """, """
            create table if not exists player_data
            (
                player_name     VARCHAR(16)          not null,
                ignore_list     text                 null,
                reply_player    varchar(16)          null,
                is_spying       tinyint(1) default 0 not null,
                inv_serialized  mediumtext           null,
                item_serialized text                 null,
                ec_serialized   mediumtext           null,
                primary key(player_name),
                foreign key (reply_player) references player_data (player_name)
            );
            """, """
            create table if not exists ignored_players
            (
                player_name    varchar(16) not null,
                ignored_player varchar(16) not null,
                unique (player_name, ignored_player),
                foreign key (player_name) references player_data (player_name),
                foreign key (ignored_player) references player_data (player_name)
            );
            """};

    public LegacyDataManager(RedisChat plugin) {
        this.plugin = plugin;
        this.rateLimit = new ConcurrentHashMap<>();
        initialize();
        listenPluginMessages();
    }

    private void listenPluginMessages() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", (channel, player, message) -> {
            if (!channel.equals("BungeeCord")) return;
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals(DataKeys.PLAYERLIST.toString())) {
                String serializedPlayerList = in.readUTF();
                if (plugin.getPlayerListManager() != null)
                    plugin.getPlayerListManager().updatePlayerList(Arrays.asList(serializedPlayerList.split("§")));
            } else if (subchannel.equals(DataKeys.CHAT_CHANNEL.toString())) {
                String messageString = in.readUTF();
                plugin.getChatListener().receiveChatMessage(new ChatMessageInfo(messageString));
            }

        });
    }

    private void initialize() throws IllegalStateException {
        // Initialize the Hikari pooled connection
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:" + (plugin.config.mysql.driverClass().contains("mariadb") ? "mariadb" : "mysql")
                + "://"
                + plugin.config.mysql.host()
                + ":"
                + plugin.config.mysql.port()
                + "/"
                + plugin.config.mysql.database()
                + plugin.config.mysql.connectionParameters()
        );

        // Authenticate with the database
        dataSource.setUsername(plugin.config.mysql.username());
        dataSource.setPassword(plugin.config.mysql.password());

        // Set connection pool options
        dataSource.setMaximumPoolSize(plugin.config.mysql.poolSize());
        dataSource.setMinimumIdle(plugin.config.mysql.poolIdle());
        dataSource.setConnectionTimeout(plugin.config.mysql.poolTimeout());
        dataSource.setMaxLifetime(plugin.config.mysql.poolLifetime());
        dataSource.setKeepaliveTime(plugin.config.mysql.poolKeepAlive());

        // Set additional connection pool properties
        final Properties properties = new Properties();
        properties.putAll(
                Map.of("cachePrepStmts", "true",
                        "prepStmtCacheSize", "250",
                        "prepStmtCacheSqlLimit", "2048",
                        "useServerPrepStmts", "true",
                        "useLocalSessionState", "true",
                        "useLocalTransactionState", "true"
                ));
        properties.putAll(
                Map.of(
                        "rewriteBatchedStatements", "true",
                        "cacheResultSetMetadata", "true",
                        "cacheServerConfiguration", "true",
                        "elideSetAutoCommits", "true",
                        "maintainTimeStats", "false")
        );
        dataSource.setDataSourceProperties(properties);

        // Prepare database schema; make tables if they don't exist
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : TABLE_DDL) {
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

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public Optional<String> getReplyName(String requesterName) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT `reply_player`
                    FROM player_data
                    WHERE `player_name`=?""")) {
                statement.setString(1, requesterName);

                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return Optional.of(resultSet.getString("reply_player"));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to fetch a reply name from the database");
        }
        return Optional.empty();
    }

    @Override
    public void setReplyName(String nameReceiver, String requesterName) {
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
                    throw new SQLException("Failed to insert reply name into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isRateLimited(String playerName) {
        Map.Entry<Integer, Long> info = this.rateLimit.get(playerName);
        if (info != null)
            if (System.currentTimeMillis() - info.getValue() > plugin.config.rate_limit_time_seconds * 1000L) {
                this.rateLimit.remove(playerName);
                return false;
            } else {
                return true;
            }
        return false;
    }

    @Override
    public void setRateLimit(String playerName, int seconds) {
        if (this.rateLimit.computeIfPresent(playerName, (k, v) -> {
            v.setValue(v.getValue() + 1);
            return v;
        }) == null)
            this.rateLimit.put(playerName, new AbstractMap.SimpleEntry<>(1, System.currentTimeMillis()));
    }

    @Override
    public CompletionStage<Boolean> isSpying(String playerName) {
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
                Bukkit.getLogger().severe("Failed to fetch if a player is spying from the database");
            }
            return false;
        });
    }

    @Override
    public void setSpying(String playerName, boolean spy) {
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
                    throw new SQLException("Failed to insert spy toggling into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletionStage<Boolean> toggleIgnoring(String playerName, String ignoringName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        IF exists(select * from ignored_players where player_name = ? and ignored_player= ?) then
                            delete from ignored_players where player_name = ? and ignored_player= ? RETURNING false result;
                        else
                            insert into ignored_players (player_name, ignored_player) VALUES (?,?) RETURNING true result;
                        end if;""")) {

                    statement.setString(1, playerName);
                    statement.setString(2, ignoringName);
                    statement.setString(3, playerName);
                    statement.setString(4, ignoringName);
                    statement.setString(5, playerName);
                    statement.setString(6, ignoringName);
                    statement.executeUpdate();

                    final ResultSet resultSet = statement.getResultSet();
                    if (resultSet.next()) {
                        return resultSet.getBoolean("result");
                    }
                    throw new SQLException("Failed to insert player ignore into database");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    @Override
    public CompletionStage<Boolean> isIgnoring(String playerName, String ignoringName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select * from ignored_players
                        where player_name= ? and ignored_player = ?;""")) {

                    statement.setString(1, playerName);
                    statement.setString(2, ignoringName);

                    final ResultSet resultSet = statement.executeQuery();
                    return resultSet.next();
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to fetch if player is ignoring from the database");
            }
            return false;
        });
    }

    @Override
    public CompletionStage<List<String>> ignoringList(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select ignored_player from ignored_players
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    List<String> ignoredPlayers = new ArrayList<>();
                    while (resultSet.next()) {
                        ignoredPlayers.add(resultSet.getString("ignored_player"));
                    }
                    return ignoredPlayers;
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to fetch a ignoring list from the database");
            }
            return null;
        });
    }

    @Override
    public void addInventory(String name, ItemStack[] inv) {
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
                    throw new SQLException("Failed to insert serialized inventory into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void addItem(String name, ItemStack item) {
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
                    throw new SQLException("Failed to insert serialized item into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addEnderchest(String name, ItemStack[] inv) {
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
                    throw new SQLException("Failed to insert serialized enderchest into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletionStage<ItemStack[]> getPlayerInventory(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select inv_serialized from player_data
                        where player_name = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        String serializedInv = resultSet.getString("inv_serialized");
                        return serializedInv == null ? null : deserialize(serializedInv);
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to fetch a player inventory from the database");
            }
            return null;
        });
    }

    @Override
    public void sendChatMessage(ChatMessageInfo chatMessage) {
        if (plugin.getServer().getOnlinePlayers().size() == 0) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKeys.CHAT_CHANNEL.toString());
        out.writeUTF(chatMessage.serialize());
        plugin.getServer().getOnlinePlayers().iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        plugin.getChatListener().receiveChatMessage(chatMessage);
    }

    @Override
    public void publishPlayerList(List<String> playerNames) {
        if (plugin.getServer().getOnlinePlayers().size() == 0) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKeys.PLAYERLIST.toString());
        out.writeUTF(String.join("§", playerNames));

        plugin.getServer().getOnlinePlayers().iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

        if (plugin.getPlayerListManager() != null)
            plugin.getPlayerListManager().updatePlayerList(playerNames);
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
