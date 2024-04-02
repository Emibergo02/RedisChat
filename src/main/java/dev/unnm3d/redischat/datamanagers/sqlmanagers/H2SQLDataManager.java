/*
 * This file is part of HuskHomes, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.unnm3d.redischat.datamanagers.sqlmanagers;


import dev.unnm3d.redischat.RedisChat;
import org.bukkit.inventory.ItemStack;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

@SuppressWarnings("DuplicatedCode")
public class H2SQLDataManager extends SQLDataManager {

    /**
     * Path to the H2 HuskHomesData.h2 file.
     */
    private final File databaseFile;

    /**
     * The name of the database file.
     */
    private static final String DATABASE_FILE_NAME = "RedisChatData.h2";

    private JdbcConnectionPool connectionPool;

    public H2SQLDataManager(@NotNull RedisChat plugin) {
        super(plugin);
        this.databaseFile = new File(plugin.getDataFolder(), DATABASE_FILE_NAME);
        initialize();
        listenPluginMessages();
    }

    /**
     * Fetch the auto-closeable connection from the H2 Connection Pool.
     *
     * @return The {@link Connection} to the H2 database
     * @throws SQLException if the connection fails for some reason
     */
    @Override
    protected Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    @Override
    public void initialize() throws IllegalStateException {
        // Prepare the database flat file
        final String url = String.format("jdbc:h2:%s", databaseFile.getAbsolutePath());
        this.connectionPool = JdbcConnectionPool.create(url + ";mode=MySQL", "sa", "sa");

        // Prepare database schema; make tables if they don't exist
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : getSQLSchema()) {
                    statement.execute(tableCreationStatement);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create H2 database tables", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize the H2 database", e);
        }
    }

    @Override
    public void setReplyName(@NotNull String nameReceiver, @NotNull String requesterName) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO PLAYER_DATA
                        (PLAYER_NAME, REPLY_PLAYER)
                    VALUES
                        (?,?);""")) {

                statement.setString(1, nameReceiver);
                statement.setString(2, requesterName);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert reply name into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSpying(@NotNull String playerName, boolean spy) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO PLAYER_DATA
                        (PLAYER_NAME, IS_SPYING)
                    VALUES
                        (?,?)""")) {

                statement.setString(1, playerName);
                statement.setBoolean(2, spy);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Failed to insert spy toggling into database");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addInventory(@NotNull String name, ItemStack[] inv) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO PLAYER_DATA
                        (PLAYER_NAME, INV_SERIALIZED)
                    VALUES
                        (?,?);""")) {

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
    public void addItem(@NotNull String name, ItemStack item) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO PLAYER_DATA
                        (PLAYER_NAME, ITEM_SERIALIZED)
                    VALUES
                        (?,?);""")) {

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
    public void addEnderchest(@NotNull String name, ItemStack[] inv) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO PLAYER_DATA
                        (PLAYER_NAME, EC_SERIALIZED)
                    VALUES
                        (?,?);""")) {

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
    public void close() {
        if (connectionPool != null) {
            connectionPool.dispose();
        }
    }

}
