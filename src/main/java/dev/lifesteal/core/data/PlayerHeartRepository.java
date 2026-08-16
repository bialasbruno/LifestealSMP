package dev.lifesteal.core.data;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence boundary for player heart data. Kept separate from
 * {@link dev.lifesteal.core.heart.HeartService} so the storage backend (SQLite today) can be
 * swapped later without touching gameplay logic.
 */
public interface PlayerHeartRepository {

    /** Reads the stored max hearts for a player, if a record exists. */
    Optional<Integer> findHearts(UUID uuid);

    /** Creates or updates a player's stored max hearts and last known name. */
    void upsertHearts(UUID uuid, String lastKnownName, int hearts);

    /** Releases any underlying resources (e.g. the database connection). */
    void close();
}
