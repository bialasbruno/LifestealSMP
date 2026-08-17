package dev.lifesteal.core.data;

import java.time.Instant;
import java.util.List;
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

    /**
     * Atomically claims the one allowed Revive Totem drop for a victim in a season.
     *
     * @return {@code true} only for the first claim of {@code victimUuid} in {@code seasonId}
     */
    boolean claimReviveTotemDrop(
            String seasonId, UUID victimUuid, UUID killerUuid, Instant droppedAt);

    /** Creates or replaces the active temporary elimination for a player. */
    void saveElimination(UUID uuid, String lastKnownName, Instant bannedUntil);

    /** Finds an active or not-yet-resolved elimination by UUID. */
    Optional<EliminationRecord> findElimination(UUID uuid);

    /** Finds an active or not-yet-resolved elimination by the player's last known name. */
    Optional<EliminationRecord> findEliminationByName(String lastKnownName);

    /** Lists eliminations whose ban time has elapsed. */
    List<EliminationRecord> findExpiredEliminations(Instant now);

    /** Lists names of players whose elimination is still active. */
    List<String> findActiveEliminatedNames(Instant now);

    /** Atomically restores a player's hearts and removes their active elimination. */
    void restoreEliminatedPlayer(UUID uuid, String lastKnownName, int hearts);

    /** Releases any underlying resources (e.g. the database connection). */
    void close();
}
