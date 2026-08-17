package dev.lifesteal.core.api;

import java.util.UUID;

/** Public read-only API for other LifestealSMP plugins. */
public interface LifestealCoreApi {

    /** Returns the player's current maximum health measured in hearts, not raw health points. */
    int getHearts(UUID playerId);
}
