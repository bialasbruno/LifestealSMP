package dev.lifesteal.souls.api;

import java.util.UUID;

/** Public API used by other LifestealSMP plugins. */
public interface LifestealSoulsApi {

    /** Returns the player's current Souls balance. */
    long getSouls(UUID playerId);

    /**
     * Atomically spends Souls without allowing a negative balance.
     *
     * @return {@code true} when the full amount was deducted
     */
    boolean trySpend(UUID playerId, long amount, String reason);
}
