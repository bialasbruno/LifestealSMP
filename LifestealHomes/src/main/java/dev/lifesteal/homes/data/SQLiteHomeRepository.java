package dev.lifesteal.homes.data;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SQLiteHomeRepository implements HomeRepository {

    private final Connection connection;

    public SQLiteHomeRepository(Path databaseFile) {
        try {
            Class.forName("org.sqlite.JDBC");
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) {
                parent.toFile().mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
            configure();
            createSchema();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new IllegalStateException("Could not initialize homes database", exception);
        }
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA busy_timeout=5000");
        }
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS homes (
                        player_uuid TEXT NOT NULL,
                        home_key TEXT NOT NULL,
                        display_name TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, home_key)
                    )
                    """);
        }
    }

    @Override
    public synchronized List<StoredHome> findAll(UUID playerId) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? ORDER BY display_name COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                List<StoredHome> homes = new ArrayList<>();
                while (result.next()) {
                    homes.add(read(result));
                }
                return List.copyOf(homes);
            }
        } catch (SQLException exception) {
            throw databaseError("load homes", exception);
        }
    }

    @Override
    public synchronized Optional<StoredHome> find(UUID playerId, String key) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? AND home_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("load home", exception);
        }
    }

    @Override
    public synchronized int count(UUID playerId) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw databaseError("count homes", exception);
        }
    }

    @Override
    public synchronized void save(StoredHome home) {
        String sql = """
                INSERT INTO homes (
                    player_uuid, home_key, display_name, world_name,
                    x, y, z, yaw, pitch, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid, home_key) DO UPDATE SET
                    display_name = excluded.display_name,
                    world_name = excluded.world_name,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch,
                    updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, home.playerId().toString());
            statement.setString(2, home.key());
            statement.setString(3, home.name());
            statement.setString(4, home.worldName());
            statement.setDouble(5, home.x());
            statement.setDouble(6, home.y());
            statement.setDouble(7, home.z());
            statement.setFloat(8, home.yaw());
            statement.setFloat(9, home.pitch());
            statement.setLong(10, home.createdAt());
            statement.setLong(11, home.updatedAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("save home", exception);
        }
    }

    @Override
    public synchronized boolean delete(UUID playerId, String key) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, key);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw databaseError("delete home", exception);
        }
    }

    private StoredHome read(ResultSet result) throws SQLException {
        return new StoredHome(
                UUID.fromString(result.getString("player_uuid")),
                result.getString("home_key"),
                result.getString("display_name"),
                result.getString("world_name"),
                result.getDouble("x"),
                result.getDouble("y"),
                result.getDouble("z"),
                result.getFloat("yaw"),
                result.getFloat("pitch"),
                result.getLong("created_at"),
                result.getLong("updated_at"));
    }

    private IllegalStateException databaseError(String operation, SQLException exception) {
        return new IllegalStateException("Could not " + operation, exception);
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw databaseError("close homes database", exception);
        }
    }
}
