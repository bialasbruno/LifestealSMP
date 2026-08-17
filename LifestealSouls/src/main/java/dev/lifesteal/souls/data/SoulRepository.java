package dev.lifesteal.souls.data;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SoulRepository extends AutoCloseable {

    SoulAccount loadOrCreate(UUID playerId, String lastKnownName);

    Optional<SoulAccount> find(UUID playerId);

    Optional<SoulAccount> findByName(String lastKnownName);

    List<SoulAccount> top(int limit);

    SoulMutation add(
            UUID playerId,
            String lastKnownName,
            long amount,
            SoulTransactionType type,
            String reference,
            Instant createdAt,
            long maximumBalance);

    Optional<SoulMutation> tryDebit(
            UUID playerId,
            String lastKnownName,
            long amount,
            SoulTransactionType type,
            String reference,
            Instant createdAt);

    SoulMutation setBalance(
            UUID playerId,
            String lastKnownName,
            long balance,
            Instant createdAt,
            long maximumBalance);

    PlaytimeRewardResult addActivePlaytime(
            UUID playerId,
            String lastKnownName,
            long elapsedMillis,
            long rewardIntervalMillis,
            long rewardAmount,
            Instant createdAt,
            long maximumBalance);

    KillRewardResult rewardKill(
            UUID killerId,
            String killerName,
            UUID victimId,
            Instant createdAt,
            long cooldownMillis,
            long rewardAmount,
            long maximumBalance);

    PurchaseResult applyPurchase(
            String transactionId,
            UUID playerId,
            String lastKnownName,
            long amount,
            Instant createdAt,
            long maximumBalance);

    List<SoulTransaction> history(UUID playerId, int limit);

    @Override
    void close();
}
