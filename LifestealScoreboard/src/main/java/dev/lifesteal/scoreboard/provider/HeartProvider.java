package dev.lifesteal.scoreboard.provider;

import java.util.UUID;

/** Read-only heart source used by scoreboard placeholders. */
@FunctionalInterface
public interface HeartProvider {

    int getHearts(UUID playerId);
}
