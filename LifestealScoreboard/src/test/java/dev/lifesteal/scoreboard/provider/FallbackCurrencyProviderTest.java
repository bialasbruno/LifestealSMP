package dev.lifesteal.scoreboard.provider;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackCurrencyProviderTest {

    @Test
    void moneyAndSoulsDefaultToZero() {
        UUID playerId = UUID.randomUUID();

        assertEquals(0L, FallbackCurrencyProvider.INSTANCE.getMoney(playerId));
        assertEquals(0L, FallbackCurrencyProvider.INSTANCE.getSouls(playerId));
    }
}
