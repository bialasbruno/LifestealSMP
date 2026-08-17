package dev.lifesteal.scoreboard.api;

import java.util.UUID;

/** Read-only currency contract for a future economy plugin. */
public interface CurrencyProvider {

    long getMoney(UUID playerId);

    long getSouls(UUID playerId);
}
