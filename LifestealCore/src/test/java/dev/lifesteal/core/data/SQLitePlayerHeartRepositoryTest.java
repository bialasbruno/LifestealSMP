package dev.lifesteal.core.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLitePlayerHeartRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLitePlayerHeartRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SQLitePlayerHeartRepository(
                tempDir.resolve("data.db").toFile(),
                Logger.getLogger(SQLitePlayerHeartRepositoryTest.class.getName()));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void victimCanDropOnlyOneReviveTotemPerSeason() {
        UUID victim = UUID.randomUUID();
        UUID firstKiller = UUID.randomUUID();
        UUID secondKiller = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T00:00:00Z");

        assertTrue(repository.claimReviveTotemDrop("season-1", victim, firstKiller, now));
        assertFalse(repository.claimReviveTotemDrop("season-1", victim, secondKiller, now.plusSeconds(60)));
        assertTrue(repository.claimReviveTotemDrop("season-2", victim, secondKiller, now.plusSeconds(120)));
    }

    @Test
    void differentVictimsCanEachDropOnceInSameSeason() {
        UUID killer = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T00:00:00Z");

        assertTrue(repository.claimReviveTotemDrop("season-1", UUID.randomUUID(), killer, now));
        assertTrue(repository.claimReviveTotemDrop("season-1", UUID.randomUUID(), killer, now));
    }

    @Test
    void eliminationPersistsAndRestoreAtomicallySetsHearts() {
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T00:00:00Z");
        Instant bannedUntil = now.plusSeconds(3600);

        repository.upsertHearts(player, "FallenPlayer", 1);
        repository.saveElimination(player, "FallenPlayer", bannedUntil);

        EliminationRecord stored = repository.findElimination(player).orElseThrow();
        assertEquals("FallenPlayer", stored.lastKnownName());
        assertEquals(bannedUntil, stored.bannedUntil());
        assertEquals(
                stored,
                repository.findEliminationByName("fallenplayer").orElseThrow());
        assertEquals(1, repository.findActiveEliminatedNames(now).size());
        assertTrue(repository.findExpiredEliminations(now).isEmpty());

        repository.restoreEliminatedPlayer(player, "FallenPlayer", 10);

        assertEquals(10, repository.findHearts(player).orElseThrow());
        assertTrue(repository.findElimination(player).isEmpty());
    }

    @Test
    void expiredEliminationsAreListed() {
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T00:00:00Z");
        repository.saveElimination(player, "ExpiredPlayer", now.minusSeconds(1));

        assertEquals(1, repository.findExpiredEliminations(now).size());
        assertTrue(repository.findActiveEliminatedNames(now).isEmpty());
    }
}
