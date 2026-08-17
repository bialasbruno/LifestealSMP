package dev.lifesteal.homes.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteHomeRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    private SQLiteHomeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SQLiteHomeRepository(temporaryDirectory.resolve("homes.db"));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void savesAndLoadsHomesPerPlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        repository.save(home(first, "base", "Base", 1.0D, 100L));
        repository.save(home(first, "farm", "Farm", 2.0D, 101L));
        repository.save(home(second, "base", "Base", 3.0D, 102L));

        assertEquals(2, repository.count(first));
        assertEquals(1, repository.count(second));
        assertEquals("Base", repository.find(first, "base").orElseThrow().name());
        assertEquals(2, repository.findAll(first).size());
    }

    @Test
    void updatingAHomeKeepsItsCreationTime() {
        UUID playerId = UUID.randomUUID();
        repository.save(home(playerId, "base", "Base", 1.0D, 100L));
        repository.save(new StoredHome(
                playerId, "base", "BASE", "world_nether", 99.0D, 70.0D, 5.0D,
                10.0F, 20.0F, 999L, 200L));

        StoredHome loaded = repository.find(playerId, "base").orElseThrow();
        assertEquals(1, repository.count(playerId));
        assertEquals("BASE", loaded.name());
        assertEquals("world_nether", loaded.worldName());
        assertEquals(99.0D, loaded.x());
        assertEquals(100L, loaded.createdAt());
        assertEquals(200L, loaded.updatedAt());
    }

    @Test
    void deletesOnlyTheRequestedHome() {
        UUID playerId = UUID.randomUUID();
        repository.save(home(playerId, "base", "Base", 1.0D, 100L));
        repository.save(home(playerId, "farm", "Farm", 2.0D, 100L));

        assertTrue(repository.delete(playerId, "base"));
        assertFalse(repository.delete(playerId, "base"));
        assertTrue(repository.find(playerId, "base").isEmpty());
        assertTrue(repository.find(playerId, "farm").isPresent());
    }

    private StoredHome home(UUID playerId, String key, String name, double x, long time) {
        return new StoredHome(
                playerId, key, name, "world", x, 64.0D, 10.0D, 0.0F, 0.0F, time, time);
    }
}
