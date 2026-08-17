package dev.lifesteal.scoreboard.api;

import java.util.UUID;

/** Read-only provider for the server's regular Vault economy balance. */
@FunctionalInterface
public interface BalanceProvider {

    double getBalance(UUID playerId);
}
