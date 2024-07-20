package dev.unnm3d.redischat.datamanagers.sqlmanagers;

import com.zaxxer.hikari.HikariDataSource;
import dev.unnm3d.redischat.RedisChat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

public class MySQLDataManager extends SQLDataManager {
    private HikariDataSource dataSource;

    public MySQLDataManager(RedisChat plugin) {
        super(plugin);
        initialize();
    }

    @Override
    protected void initialize() throws IllegalStateException {
        // Initialize the Hikari pooled connection
        dataSource = new HikariDataSource();

        dataSource.setDriverClassName(plugin.config.mysql.driverClass());
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
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
