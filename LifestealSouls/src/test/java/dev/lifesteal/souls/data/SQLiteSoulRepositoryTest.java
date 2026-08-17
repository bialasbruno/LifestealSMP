package dev.lifesteal.souls.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteSoulRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    private SQLiteSoulRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SQLiteSoulRepository(
                temporaryDirectory.resolve("souls.db").toFile(),
                Logger.getLogger("SQLiteSoulRepositoryTest"));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void playtimePaysFiftyOnlyAfterTheFullHour() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T12:00:00Z");

        PlaytimeRewardResult beforeHour = repository.addActivePlaytime(
                playerId, "Player", 3_599_000L, 3_600_000L, 50L, now, 1_000_000L);
        PlaytimeRewardResult fullHour = repository.addActivePlaytime(
                playerId, "Player", 1_000L, 3_600_000L, 50L, now.plusSeconds(1), 1_000_000L);

        assertEquals(0L, beforeHour.credited());
        assertEquals(3_599_000L, beforeHour.activeProgressMillis());
        assertEquals(50L, fullHour.credited());
        assertEquals(50L, fullHour.balance());
        assertEquals(0L, fullHour.activeProgressMillis());
        assertEquals(1L, fullHour.completedIntervals());
    }

    @Test
    void killCooldownIsDirectionalAndPersistsPerVictim() {
        UUID killerId = UUID.randomUUID();
        UUID victimId = UUID.randomUUID();
        Instant firstKill = Instant.parse("2026-08-17T12:00:00Z");

        KillRewardResult first = repository.rewardKill(
                killerId, "Killer", victimId, firstKill, 3_600_000L, 3L, 1_000_000L);
        KillRewardResult blocked = repository.rewardKill(
                killerId,
                "Killer",
                victimId,
                firstKill.plusSeconds(3_599),
                3_600_000L,
                3L,
                1_000_000L);
        KillRewardResult eligibleAgain = repository.rewardKill(
                killerId,
                "Killer",
                victimId,
                firstKill.plusSeconds(3_600),
                3_600_000L,
                3L,
                1_000_000L);

        assertTrue(first.rewarded());
        assertFalse(blocked.rewarded());
        assertEquals(1_000L, blocked.cooldownRemainingMillis());
        assertTrue(eligibleAgain.rewarded());
        assertEquals(6L, eligibleAgain.balance());
    }

    @Test
    void webshopTransactionIsIdempotentAndDetectsConflicts() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T12:00:00Z");

        PurchaseResult applied = repository.applyPurchase(
                "order-123", playerId, "Player", 500L, now, 1_000_000L);
        PurchaseResult duplicate = repository.applyPurchase(
                "order-123", playerId, "Player", 500L, now.plusSeconds(1), 1_000_000L);

        assertTrue(applied.applied());
        assertFalse(duplicate.applied());
        assertEquals(500L, duplicate.balance());
        assertThrows(
                PurchaseConflictException.class,
                () -> repository.applyPurchase(
                        "order-123", playerId, "Player", 501L, now, 1_000_000L));
    }

    @Test
    void spendingCannotCreateNegativeBalance() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        repository.add(
                playerId,
                "Player",
                10L,
                SoulTransactionType.ADMIN_ADD,
                "test",
                now,
                1_000_000L);

        Optional<SoulMutation> rejected = repository.tryDebit(
                playerId,
                "Player",
                11L,
                SoulTransactionType.ITEM_PURCHASE,
                "item",
                now.plusSeconds(1));
        Optional<SoulMutation> accepted = repository.tryDebit(
                playerId,
                "Player",
                7L,
                SoulTransactionType.ITEM_PURCHASE,
                "item",
                now.plusSeconds(2));

        assertTrue(rejected.isEmpty());
        assertEquals(3L, accepted.orElseThrow().balance());
    }

    @Test
    void historyContainsAuditableBalanceChanges() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        repository.add(
                playerId,
                "Player",
                10L,
                SoulTransactionType.ADMIN_ADD,
                "first",
                now,
                1_000_000L);
        repository.tryDebit(
                playerId,
                "Player",
                4L,
                SoulTransactionType.ADMIN_REMOVE,
                "second",
                now.plusSeconds(1));

        List<SoulTransaction> history = repository.history(playerId, 10);

        assertEquals(2, history.size());
        assertEquals(SoulTransactionType.ADMIN_REMOVE, history.get(0).type());
        assertEquals(-4L, history.get(0).amount());
        assertEquals(6L, history.get(0).balanceAfter());
        assertEquals("second", history.get(0).reference());
    }

    @Test
    void leaderboardOrdersPositiveBalancesDescending() {
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        repository.add(
                firstPlayer,
                "First",
                10L,
                SoulTransactionType.ADMIN_ADD,
                "test",
                now,
                1_000_000L);
        repository.add(
                secondPlayer,
                "Second",
                25L,
                SoulTransactionType.ADMIN_ADD,
                "test",
                now,
                1_000_000L);

        List<SoulAccount> leaderboard = repository.top(10);

        assertEquals(List.of(secondPlayer, firstPlayer),
                leaderboard.stream().map(SoulAccount::playerId).toList());
    }

    @Test
    void afkRewardIsRecordedWithItsOwnTransactionType() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T12:00:00Z");

        SoulMutation reward = repository.add(
                playerId,
                "Player",
                1L,
                SoulTransactionType.AFK_ZONE,
                "afk-zone",
                now,
                1_000_000L);
        List<SoulTransaction> history = repository.history(playerId, 10);

        assertEquals(1L, reward.amount());
        assertEquals(1L, reward.balance());
        assertEquals(SoulTransactionType.AFK_ZONE, history.getFirst().type());
    }
}
