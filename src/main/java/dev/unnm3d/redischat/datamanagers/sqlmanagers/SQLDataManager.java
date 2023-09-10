package dev.unnm3d.redischat.datamanagers.sqlmanagers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.channels.Channel;
import dev.unnm3d.redischat.channels.PlayerChannel;
import dev.unnm3d.redischat.chat.ChatMessageInfo;
import dev.unnm3d.redischat.datamanagers.DataKeys;
import dev.unnm3d.redischat.mail.Mail;
import org.bukkit.Bukkit;
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
            create table if not exists player_data
            (
                player_name     varchar(16)     not null primary key,
                ignore_list     TEXT            default NULL,
                reply_player    varchar(16)     default NULL,
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
                    foreign key (channel_name) references channels (name),
                constraint player_channels_player_data_player_name_fk
                    foreign key (player_name) references player_data (player_name)
            );
            """, """
            create table if not exists ignored_players
            (
                player_name    varchar(16) not null,
                ignored_player varchar(16) not null,
                unique (player_name, ignored_player)
            );
            """};
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void listenPluginMessages() {
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
                plugin.getChannelManager().sendLocalChatMessage(new ChatMessageInfo(messageString));
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                Bukkit.getLogger().warning("Failed to fetch a reply name from the database");
            }
            return Optional.empty();
        });
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
                    throw new SQLException("Failed to insert reply name into database");
                }
            }
        } catch (SQLException e) {
            if (plugin.config.debug) {
                e.printStackTrace();
            }
            plugin.getServer().getLogger().warning("Failed to insert reply name into database");
        }
    }

    @Override
    public boolean isRateLimited(@NotNull String playerName, @NotNull Channel channel) {
        Map.Entry<Integer, Long> info = this.rateLimit.get(playerName);
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch if player is spying from the database");
            }
            return false;
        });
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
                    throw new SQLException("Failed to insert spy toggling into database");
                }
            }
        } catch (SQLException e) {
            if (plugin.config.debug) {
                e.printStackTrace();
            }
            plugin.getServer().getLogger().warning("Failed to insert spy toggling into database");
        }
    }

    @Override
    public CompletionStage<Boolean> toggleIgnoring(@NotNull String playerName, @NotNull String ignoringName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        IF exists(select * from ignored_players where player_name = ? and ignored_player= ?) then
                            delete from ignored_players where player_name = ? and ignored_player= ? RETURNING false result;
                        else
                            insert into ignored_players (player_name, ignored_player) VALUES (?,?) RETURNING true result;
                        end if
                        """)) {

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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to insert player ignore into database");
            }
            return false;
        });
    }

    @Override
    public CompletionStage<Boolean> isIgnoring(@NotNull String playerName, @NotNull String ignoringName) {
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch player ignore from the database");
            }
            return false;
        });
    }

    @Override
    public CompletionStage<List<String>> ignoringList(@NotNull String playerName) {
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch ignored players from the database");
            }
            return null;
        });
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
                    throw new SQLException("Failed to insert serialized inventory into database");
                }
            }
        } catch (SQLException e) {
            if (plugin.config.debug) {
                e.printStackTrace();
            }
            plugin.getServer().getLogger().warning("Failed to insert serialized inventory into database");
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
                    throw new SQLException("Failed to insert serialized item into database");
                }
            }
        } catch (SQLException e) {
            if (plugin.config.debug) {
                e.printStackTrace();
            }
            plugin.getServer().getLogger().warning("Failed to insert serialized item into database");
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
                    throw new SQLException("Failed to insert serialized enderchest into database");
                }
            }
        } catch (SQLException e) {
            if (plugin.config.debug) {
                e.printStackTrace();
            }
            plugin.getServer().getLogger().warning("Failed to insert serialized enderchest into database");
        }
    }

    @Override
    public void clearInvShareCache() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE player_data SET inv_serialized = NULL, item_serialized = NULL, ec_serialized = NULL;
                    """)) {
                statement.executeUpdate();

            }
        } catch (SQLException e) {
            if (plugin.config.debug) {
                e.printStackTrace();
            }
            plugin.getServer().getLogger().warning("Failed to clear inv share cache");
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
                        return serializedItem == null ? null : deserialize(serializedItem)[0];
                    }
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch a player item from the database");
            }
            return null;
        });
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
                        return serializedInv == null ? null : deserialize(serializedInv);
                    }
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch serialized inventory from the database");
            }
            return null;
        });
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
                        return serializedEc == null ? null : deserialize(serializedEc);
                    }
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch serialized enderchest from the database");
            }
            return null;
        });
    }

    @Override
    public CompletionStage<List<Mail>> getPlayerPrivateMail(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select id, serializedMail from mails
                        where recipient = ?;""")) {

                    statement.setString(1, playerName);

                    final ResultSet resultSet = statement.executeQuery();
                    List<Mail> mails = new ArrayList<>();
                    while (resultSet.next()) {
                        mails.add(new Mail(
                                resultSet.getDouble("id"),
                                resultSet.getString("serializedMail")));
                    }
                    return mails;
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch serialized private mails from the database");
            }
            return List.of();
        });
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
                        throw new SQLException("Failed to insert serialized private mail into database");
                    }
                    return true;
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to insert serialized private mail into database");
            }
            return false;
        });
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
                        throw new SQLException("Failed to insert serialized public mail into database");
                    }
                    return true;
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to insert serialized public mail into database");
            }
            return false;
        });
    }

    @Override
    public CompletionStage<List<Mail>> getPublicMails() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        select id, serializedMail from mails
                        where recipient = '-Public';""")) {

                    final ResultSet resultSet = statement.executeQuery();
                    List<Mail> mails = new ArrayList<>();
                    while (resultSet.next()) {
                        mails.add(new Mail(
                                resultSet.getDouble("id"),
                                resultSet.getString("serializedMail")));
                    }
                    return mails;
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch serialized public mails from the database");
            }
            return List.of();
        });
    }

    @Override
    public void registerChannel(@NotNull Channel channel) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO channels
                            (`name`,`format`,`rate_limit`,`rate_limit_period`,`proximity_distance`,`discordWebhook`,`filtered`,`notificationSound`)
                        VALUES
                            (?,?,?,?,?,?,?,?);""")) {

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
                        throw new SQLException("Failed to register channel to database");
                    }
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
        });
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
                        throw new SQLException("Failed to unregister channel to database");
                    }
                    return true;
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to unregister channel from database");
            }
            return false;
        });
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch active channel from database");
            }
            return "public";
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to fetch channel statuses from database");
            }
            return List.of();
        });
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
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed fetch channels from database");
            }
            return List.of();
        });
    }

    @Override
    public void setPlayerChannelStatuses(@NotNull String playerName, @NotNull Map<String, String> channelStatuses) {
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO player_channels (player_name, channel_name, status) VALUES
                        """ +
                        String.join(",", Collections.nCopies(channelStatuses.size(), " (?,?,?)")) +
                        """
                                ON DUPLICATE KEY UPDATE status = VALUES(status);
                                """)) {
                    int i = 0;
                    for (Map.Entry<String, String> stringStringEntry : channelStatuses.entrySet()) {
                        statement.setString(i * 3 + 1, playerName);
                        statement.setString(i * 3 + 2, stringStringEntry.getKey());
                        statement.setInt(i * 3 + 3, Integer.parseInt(stringStringEntry.getValue()));
                        i++;
                    }
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Failed to register channel to database");
                    }
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to register channel to database");
            }
            return null;
        });
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
                        throw new SQLException("Failed to register channel to database");
                    }
                }
            } catch (SQLException e) {
                if (plugin.config.debug) {
                    e.printStackTrace();
                }
                plugin.getServer().getLogger().warning("Failed to register channel to database");
            }
            return null;
        });
    }


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void sendChatMessage(@NotNull ChatMessageInfo chatMessage) {
        if (plugin.getServer().getOnlinePlayers().size() == 0) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKeys.CHAT_CHANNEL.toString());
        out.writeUTF(chatMessage.serialize());
        plugin.getServer().getOnlinePlayers().iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        plugin.getChannelManager().sendLocalChatMessage(chatMessage);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void publishPlayerList(@NotNull List<String> playerNames) {
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
    public void sendRejoin(@NotNull String playerName) {
        if (plugin.getServer().getOnlinePlayers().size() == 0) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(DataKeys.REJOIN_CHANNEL.toString());
        out.writeUTF(playerName);

        plugin.getServer().getOnlinePlayers().iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
