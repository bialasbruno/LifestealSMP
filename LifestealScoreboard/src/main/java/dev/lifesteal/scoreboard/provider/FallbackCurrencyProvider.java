package dev.lifesteal.scoreboard.provider;

import dev.lifesteal.scoreboard.api.CurrencyProvider;

import java.util.UUID;

/** Zero-value currency provider used until an economy plugin registers a real provider. */
public final class FallbackCurrencyProvider implements CurrencyProvider {

    public static final FallbackCurrencyProvider INSTANCE = new FallbackCurrencyProvider();

    private FallbackCurrencyProvider() {
    }

    @Override
    public long getMoney(UUID playerId) {
        return 0L;
    }

    @Override
    public long getSouls(UUID playerId) {
        return 0L;
    }
}
