package dev.lifesteal.balancetop.model;

import java.util.Objects;
import java.util.UUID;

/** A known player and their regular Vault economy balance. */
public record BalanceEntry(UUID playerId, String playerName, double balance) {

    public BalanceEntry {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
