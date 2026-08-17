package dev.lifesteal.souls.data;

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

/** SQLite storage for balances, playtime progress, cooldowns and the audit log. */
public final class SQLiteSoulRepository implements SoulRepository {

    private final Connection connection;
    private final Logger logger;

    public SQLiteSoulRepository(File databaseFile, Logger logger) {
        this.logger = logger;
        try {
            Class.forName("org.sqlite.JDBC");
            File parent = databaseFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Could not create data folder: " + parent);
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            initializeSchema();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new IllegalStateException(
                    "Failed to initialize Souls database at " + databaseFile, exception);
        }
    }

    private void initializeSchema() throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS soul_accounts (
                        player_uuid TEXT PRIMARY KEY,
                        last_known_name TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0 CHECK (balance >= 0),
                        active_progress_millis INTEGER NOT NULL DEFAULT 0
                            CHECK (active_progress_millis >= 0)
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_soul_accounts_name
                    ON soul_accounts(last_known_name COLLATE NOCASE)
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS soul_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        transaction_type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        balance_after INTEGER NOT NULL,
                        reference TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_soul_transactions_player
                    ON soul_transactions(player_uuid, id DESC)
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS soul_kill_cooldowns (
                        killer_uuid TEXT NOT NULL,
                        victim_uuid TEXT NOT NULL,
                        last_rewarded_at INTEGER NOT NULL,
                        PRIMARY KEY (killer_uuid, victim_uuid)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS soul_webshop_purchases (
                        transaction_id TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        processed_at INTEGER NOT NULL
                    )
                    """);
        }
    }

    @Override
    public synchronized SoulAccount loadOrCreate(UUID playerId, String lastKnownName) {
        return inTransaction(
                "load or create account for " + playerId,
                () -> loadOrCreateInternal(playerId, lastKnownName));
    }

    @Override
    public synchronized Optional<SoulAccount> find(UUID playerId) {
        String sql = """
                SELECT player_uuid, last_known_name, balance, active_progress_millis
                FROM soul_accounts
                WHERE player_uuid = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readAccount(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find Souls account for " + playerId, exception);
        }
    }

    @Override
    public synchronized Optional<SoulAccount> findByName(String lastKnownName) {
        String sql = """
                SELECT player_uuid, last_known_name, balance, active_progress_millis
                FROM soul_accounts
                WHERE last_known_name = ? COLLATE NOCASE
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, lastKnownName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readAccount(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to find Souls account for " + lastKnownName, exception);
        }
    }

    @Override
    public synchronized List<SoulAccount> top(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Leaderboard limit must be between 1 and 100");
        }
        String sql = """
                SELECT player_uuid, last_known_name, balance, active_progress_millis
                FROM soul_accounts
                WHERE balance > 0
                ORDER BY balance DESC, last_known_name COLLATE NOCASE
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SoulAccount> accounts = new ArrayList<>();
                while (resultSet.next()) {
                    accounts.add(readAccount(resultSet));
                }
                return accounts;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read Souls leaderboard", exception);
        }
    }

    @Override
    public synchronized SoulMutation add(
            UUID playerId,
            String lastKnownName,
            long amount,
            SoulTransactionType type,
            String reference,
            Instant createdAt,
            long maximumBalance) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        return inTransaction("credit Souls for " + playerId, () -> {
            SoulAccount account = loadOrCreateInternal(playerId, lastKnownName);
            long balance = checkedCredit(account.balance(), amount, maximumBalance);
            updateAccount(playerId, lastKnownName, balance, account.activeProgressMillis());
            insertTransaction(playerId, type, amount, balance, reference, createdAt);
            return new SoulMutation(balance, amount);
        });
    }

    @Override
    public synchronized Optional<SoulMutation> tryDebit(
            UUID playerId,
            String lastKnownName,
            long amount,
            SoulTransactionType type,
            String reference,
            Instant createdAt) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        return inTransaction("debit Souls for " + playerId, () -> {
            SoulAccount account = loadOrCreateInternal(playerId, lastKnownName);
            if (account.balance() < amount) {
                return Optional.empty();
            }
            long balance = account.balance() - amount;
            updateAccount(playerId, lastKnownName, balance, account.activeProgressMillis());
            insertTransaction(playerId, type, -amount, balance, reference, createdAt);
            return Optional.of(new SoulMutation(balance, -amount));
        });
    }

    @Override
    public synchronized SoulMutation setBalance(
            UUID playerId,
            String lastKnownName,
            long balance,
            Instant createdAt,
            long maximumBalance) {
        if (balance < 0L) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (balance > maximumBalance) {
            throw new BalanceLimitException(maximumBalance);
        }
        return inTransaction("set Souls balance for " + playerId, () -> {
            SoulAccount account = loadOrCreateInternal(playerId, lastKnownName);
            long difference = balance - account.balance();
            updateAccount(playerId, lastKnownName, balance, account.activeProgressMillis());
            if (difference != 0L) {
                insertTransaction(
                        playerId,
                        SoulTransactionType.ADMIN_SET,
                        difference,
                        balance,
                        "admin-set",
                        createdAt);
            }
            return new SoulMutation(balance, difference);
        });
    }

    @Override
    public synchronized PlaytimeRewardResult addActivePlaytime(
            UUID playerId,
            String lastKnownName,
            long elapsedMillis,
            long rewardIntervalMillis,
            long rewardAmount,
            Instant createdAt,
            long maximumBalance) {
        if (elapsedMillis < 0L || rewardIntervalMillis <= 0L || rewardAmount <= 0L) {
            throw new IllegalArgumentException("Invalid playtime reward values");
        }
        return inTransaction("save active playtime for " + playerId, () -> {
            SoulAccount account = loadOrCreateInternal(playerId, lastKnownName);
            long totalProgress = Math.addExact(account.activeProgressMillis(), elapsedMillis);
            long completedIntervals = totalProgress / rewardIntervalMillis;
            long remainingProgress = totalProgress % rewardIntervalMillis;
            long requestedCredit = Math.multiplyExact(completedIntervals, rewardAmount);
            long balance = account.balance();
            long credited = Math.min(requestedCredit, maximumBalance - balance);
            balance = Math.addExact(balance, credited);
            updateAccount(playerId, lastKnownName, balance, remainingProgress);
            if (credited > 0L) {
                insertTransaction(
                        playerId,
                        SoulTransactionType.PLAYTIME,
                        credited,
                        balance,
                        completedIntervals + " active interval(s)",
                        createdAt);
            }
            return new PlaytimeRewardResult(
                    balance, credited, remainingProgress, completedIntervals);
        });
    }

    @Override
    public synchronized KillRewardResult rewardKill(
            UUID killerId,
            String killerName,
            UUID victimId,
            Instant createdAt,
            long cooldownMillis,
            long rewardAmount,
            long maximumBalance) {
        if (killerId.equals(victimId)) {
            throw new IllegalArgumentException("Killer and victim must be different players");
        }
        if (cooldownMillis <= 0L || rewardAmount <= 0L) {
            throw new IllegalArgumentException("Invalid kill reward values");
        }
        return inTransaction("reward player kill for " + killerId, () -> {
            SoulAccount account = loadOrCreateInternal(killerId, killerName);
            long now = createdAt.toEpochMilli();
            Long lastRewardedAt = findKillRewardTime(killerId, victimId);
            if (lastRewardedAt != null) {
                long eligibleAt = Math.addExact(lastRewardedAt, cooldownMillis);
                if (eligibleAt > now) {
                    return new KillRewardResult(false, 0L, account.balance(), eligibleAt - now);
                }
            }

            long credited = Math.min(rewardAmount, maximumBalance - account.balance());
            if (credited <= 0L) {
                return new KillRewardResult(false, 0L, account.balance(), 0L);
            }
            long balance = Math.addExact(account.balance(), credited);
            upsertKillRewardTime(killerId, victimId, now);
            updateAccount(killerId, killerName, balance, account.activeProgressMillis());
            insertTransaction(
                    killerId,
                    SoulTransactionType.PLAYER_KILL,
                    credited,
                    balance,
                    victimId.toString(),
                    createdAt);
            return new KillRewardResult(true, credited, balance, 0L);
        });
    }

    @Override
    public synchronized PurchaseResult applyPurchase(
            String transactionId,
            UUID playerId,
            String lastKnownName,
            long amount,
            Instant createdAt,
            long maximumBalance) {
        if (transactionId == null || transactionId.isBlank() || transactionId.length() > 128) {
            throw new IllegalArgumentException("Transaction ID must contain 1-128 characters");
        }
        if (amount <= 0L) {
            throw new IllegalArgumentException("Purchase amount must be positive");
        }
        return inTransaction("apply webshop purchase " + transactionId, () -> {
            SoulAccount account = loadOrCreateInternal(playerId, lastKnownName);
            ExistingPurchase existing = findPurchase(transactionId);
            if (existing != null) {
                if (!existing.playerId().equals(playerId) || existing.amount() != amount) {
                    throw new PurchaseConflictException(transactionId);
                }
                return new PurchaseResult(false, account.balance());
            }

            long balance = checkedCredit(account.balance(), amount, maximumBalance);
            insertPurchase(transactionId, playerId, amount, createdAt);
            updateAccount(playerId, lastKnownName, balance, account.activeProgressMillis());
            insertTransaction(
                    playerId,
                    SoulTransactionType.WEBSHOP_PURCHASE,
                    amount,
                    balance,
                    transactionId,
                    createdAt);
            return new PurchaseResult(true, balance);
        });
    }

    @Override
    public synchronized List<SoulTransaction> history(UUID playerId, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("History limit must be between 1 and 100");
        }
        String sql = """
                SELECT id, player_uuid, transaction_type, amount, balance_after, reference, created_at
                FROM soul_transactions
                WHERE player_uuid = ?
                ORDER BY id DESC
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SoulTransaction> transactions = new ArrayList<>();
                while (resultSet.next()) {
                    transactions.add(new SoulTransaction(
                            resultSet.getLong("id"),
                            UUID.fromString(resultSet.getString("player_uuid")),
                            SoulTransactionType.valueOf(resultSet.getString("transaction_type")),
                            resultSet.getLong("amount"),
                            resultSet.getLong("balance_after"),
                            resultSet.getString("reference"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at"))));
                }
                return transactions;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read Souls history for " + playerId, exception);
        }
    }

    private SoulAccount loadOrCreateInternal(UUID playerId, String lastKnownName) throws SQLException {
        String normalizedName = normalizeName(playerId, lastKnownName);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO soul_accounts
                    (player_uuid, last_known_name, balance, active_progress_millis)
                VALUES (?, ?, 0, 0)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    last_known_name = excluded.last_known_name
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, normalizedName);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_uuid, last_known_name, balance, active_progress_millis
                FROM soul_accounts
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Account disappeared after upsert for " + playerId);
                }
                return readAccount(resultSet);
            }
        }
    }

    private void updateAccount(
            UUID playerId, String lastKnownName, long balance, long activeProgressMillis)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE soul_accounts
                SET last_known_name = ?, balance = ?, active_progress_millis = ?
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, normalizeName(playerId, lastKnownName));
            statement.setLong(2, balance);
            statement.setLong(3, activeProgressMillis);
            statement.setString(4, playerId.toString());
            statement.executeUpdate();
        }
    }

    private void insertTransaction(
            UUID playerId,
            SoulTransactionType type,
            long amount,
            long balanceAfter,
            String reference,
            Instant createdAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO soul_transactions
                    (player_uuid, transaction_type, amount, balance_after, reference, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.name());
            statement.setLong(3, amount);
            statement.setLong(4, balanceAfter);
            statement.setString(5, reference);
            statement.setLong(6, createdAt.toEpochMilli());
            statement.executeUpdate();
        }
    }

    private Long findKillRewardTime(UUID killerId, UUID victimId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT last_rewarded_at
                FROM soul_kill_cooldowns
                WHERE killer_uuid = ? AND victim_uuid = ?
                """)) {
            statement.setString(1, killerId.toString());
            statement.setString(2, victimId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("last_rewarded_at") : null;
            }
        }
    }

    private void upsertKillRewardTime(UUID killerId, UUID victimId, long rewardedAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO soul_kill_cooldowns (killer_uuid, victim_uuid, last_rewarded_at)
                VALUES (?, ?, ?)
                ON CONFLICT(killer_uuid, victim_uuid) DO UPDATE SET
                    last_rewarded_at = excluded.last_rewarded_at
                """)) {
            statement.setString(1, killerId.toString());
            statement.setString(2, victimId.toString());
            statement.setLong(3, rewardedAt);
            statement.executeUpdate();
        }
    }

    private ExistingPurchase findPurchase(String transactionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_uuid, amount
                FROM soul_webshop_purchases
                WHERE transaction_id = ?
                """)) {
            statement.setString(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new ExistingPurchase(
                                UUID.fromString(resultSet.getString("player_uuid")),
                                resultSet.getLong("amount"))
                        : null;
            }
        }
    }

    private void insertPurchase(
            String transactionId, UUID playerId, long amount, Instant createdAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO soul_webshop_purchases
                    (transaction_id, player_uuid, amount, processed_at)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, transactionId);
            statement.setString(2, playerId.toString());
            statement.setLong(3, amount);
            statement.setLong(4, createdAt.toEpochMilli());
            statement.executeUpdate();
        }
    }

    private SoulAccount readAccount(ResultSet resultSet) throws SQLException {
        return new SoulAccount(
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("last_known_name"),
                resultSet.getLong("balance"),
                resultSet.getLong("active_progress_millis"));
    }

    private long checkedCredit(long balance, long amount, long maximumBalance) {
        long updated = Math.addExact(balance, amount);
        if (updated > maximumBalance) {
            throw new BalanceLimitException(maximumBalance);
        }
        return updated;
    }

    private String normalizeName(UUID playerId, String lastKnownName) {
        if (lastKnownName == null || lastKnownName.isBlank()) {
            return playerId.toString();
        }
        return lastKnownName.length() > 64 ? lastKnownName.substring(0, 64) : lastKnownName;
    }

    private <T> T inTransaction(String action, SqlWork<T> work) {
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.execute();
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to " + action, exception);
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            logger.warning("Failed to close Souls database: " + exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute() throws SQLException;
    }

    private record ExistingPurchase(UUID playerId, long amount) {
    }
}
