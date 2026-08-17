package dev.lifesteal.scoreboard.provider;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackBalanceProviderTest {

    @Test
    void balanceDefaultsToZero() {
        assertEquals(0.0D, FallbackBalanceProvider.INSTANCE.getBalance(UUID.randomUUID()));
    }
}
