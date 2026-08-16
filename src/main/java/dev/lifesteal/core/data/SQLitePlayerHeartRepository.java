package dev.lifesteal.core.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite-backed {@link PlayerHeartRepository}. Stores data at {@code plugins/LifestealCore/data.db}.
 *
 * <p>All access goes through a single shared {@link Connection}, guarded by {@code synchronized}
 * methods. SQLite does not support truly concurrent writers on one connection, and this plugin
 * only ever touches the database from asynchronous scheduler tasks, so simple synchronization
 * is sufficient for v0.1 without needing a connection pool.</p>
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
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save hearts for " + uuid, exception);
        }
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
