package dev.lifesteal.scoreboard.provider;

import dev.lifesteal.scoreboard.api.BalanceProvider;

import java.util.UUID;

/** Zero-value balance provider used when Vault has no active economy provider. */
public final class FallbackBalanceProvider implements BalanceProvider {

    public static final FallbackBalanceProvider INSTANCE = new FallbackBalanceProvider();

    private FallbackBalanceProvider() {
    }

    @Override
    public double getBalance(UUID playerId) {
        return 0.0D;
    }
}
