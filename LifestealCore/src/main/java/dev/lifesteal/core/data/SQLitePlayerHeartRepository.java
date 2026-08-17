package dev.lifesteal.core.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite-backed {@link PlayerHeartRepository}. Stores data at {@code plugins/LifestealCore/data.db}.
 *
 * <p>All access goes through a single shared {@link Connection}, guarded by {@code synchronized}
 * methods. SQLite does not support truly concurrent writers on one connection, and this plugin
 * serializes gameplay writes through one executor and guards direct lifecycle/claim operations,
 * so simple synchronization is sufficient without needing a connection pool.</p>
 */
public final class SQLitePlayerHeartRepository implements PlayerHeartRepository {

    private final Connection connection;
    private final Logger logger;

    public SQLitePlayerHeartRepository(File databaseFile, Logger logger) {
        this.logger = logger;
        try {
            Class.forName("org.sqlite.JDBC");
            File parent = databaseFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Could not create data folder: " + parent);
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            initSchema();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite database at " + databaseFile, exception);
        }
    }

    private void initSchema() throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_hearts (
                        player_uuid TEXT PRIMARY KEY,
                        last_known_name TEXT,
                        hearts INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS revive_totem_drops (
                        season_id TEXT NOT NULL,
                        victim_uuid TEXT NOT NULL,
                        killer_uuid TEXT NOT NULL,
                        dropped_at INTEGER NOT NULL,
                        PRIMARY KEY (season_id, victim_uuid)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_eliminations (
                        player_uuid TEXT PRIMARY KEY,
                        last_known_name TEXT NOT NULL,
                        banned_until INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_player_eliminations_name
                    ON player_eliminations(last_known_name COLLATE NOCASE)
                    """);
        }
    }

    @Override
    public synchronized Optional<Integer> findHearts(UUID uuid) {
        String sql = "SELECT hearts FROM player_hearts WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("hearts"));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read hearts for " + uuid, exception);
        }
    }

    @Override
    public synchronized void upsertHearts(UUID uuid, String lastKnownName, int hearts) {
        try {
            upsertHeartsInternal(uuid, lastKnownName, hearts);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save hearts for " + uuid, exception);
        }
    }

    private void upsertHeartsInternal(UUID uuid, String lastKnownName, int hearts) throws SQLException {
        String sql = """
                INSERT INTO player_hearts (player_uuid, last_known_name, hearts)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    last_known_name = excluded.last_known_name,
                    hearts = excluded.hearts
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, lastKnownName);
            statement.setInt(3, hearts);
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized boolean claimReviveTotemDrop(
            String seasonId, UUID victimUuid, UUID killerUuid, Instant droppedAt) {
        String sql = """
                INSERT OR IGNORE INTO revive_totem_drops
                    (season_id, victim_uuid, killer_uuid, dropped_at)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, seasonId);
            statement.setString(2, victimUuid.toString());
            statement.setString(3, killerUuid.toString());
            statement.setLong(4, droppedAt.toEpochMilli());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to claim Revive Totem drop for " + victimUuid + " in season " + seasonId,
                    exception);
        }
    }

    @Override
    public synchronized void saveElimination(UUID uuid, String lastKnownName, Instant bannedUntil) {
        String sql = """
                INSERT INTO player_eliminations (player_uuid, last_known_name, banned_until)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    last_known_name = excluded.last_known_name,
                    banned_until = excluded.banned_until
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, lastKnownName);
            statement.setLong(3, bannedUntil.toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save elimination for " + uuid, exception);
        }
    }

    @Override
    public synchronized Optional<EliminationRecord> findElimination(UUID uuid) {
        String sql = """
                SELECT player_uuid, last_known_name, banned_until
                FROM player_eliminations
                WHERE player_uuid = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readElimination(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read elimination for " + uuid, exception);
        }
    }

    @Override
    public synchronized Optional<EliminationRecord> findEliminationByName(String lastKnownName) {
        String sql = """
                SELECT player_uuid, last_known_name, banned_until
                FROM player_eliminations
                WHERE last_known_name = ? COLLATE NOCASE
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, lastKnownName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readElimination(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read elimination for " + lastKnownName, exception);
        }
    }

    @Override
    public synchronized List<EliminationRecord> findExpiredEliminations(Instant now) {
        String sql = """
                SELECT player_uuid, last_known_name, banned_until
                FROM player_eliminations
                WHERE banned_until <= ?
                ORDER BY banned_until
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now.toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EliminationRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(readElimination(resultSet));
                }
                return records;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list expired eliminations", exception);
        }
    }

    @Override
    public synchronized List<String> findActiveEliminatedNames(Instant now) {
        String sql = """
                SELECT last_known_name
                FROM player_eliminations
                WHERE banned_until > ?
                ORDER BY last_known_name COLLATE NOCASE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now.toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> names = new ArrayList<>();
                while (resultSet.next()) {
                    names.add(resultSet.getString("last_known_name"));
                }
                return names;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list active eliminations", exception);
        }
    }

    @Override
    public synchronized void restoreEliminatedPlayer(UUID uuid, String lastKnownName, int hearts) {
        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                upsertHeartsInternal(uuid, lastKnownName, hearts);
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM player_eliminations WHERE player_uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to restore eliminated player " + uuid, exception);
        }
    }

    private EliminationRecord readElimination(ResultSet resultSet) throws SQLException {
        return new EliminationRecord(
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("last_known_name"),
                Instant.ofEpochMilli(resultSet.getLong("banned_until")));
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            logger.warning("Failed to close SQLite connection: " + exception.getMessage());
        }
    }
}
