package dev.lifesteal.scoreboard.api;

import java.util.UUID;

/** Read-only currency contract for optional economy plugins. */
public interface CurrencyProvider {

    long getMoney(UUID playerId);

    long getSouls(UUID playerId);
}
