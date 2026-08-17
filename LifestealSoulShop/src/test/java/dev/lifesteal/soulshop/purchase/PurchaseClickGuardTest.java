package dev.lifesteal.soulshop.purchase;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseClickGuardTest {

    @Test
    void blocksOnlyRapidRepeatedClicks() {
        PurchaseClickGuard guard = new PurchaseClickGuard();
        UUID playerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        long start = 1_000_000_000L;

        assertTrue(guard.tryAcquire(playerId, start));
        assertFalse(guard.tryAcquire(playerId, start + Duration.ofMillis(299L).toNanos()));
        assertTrue(guard.tryAcquire(playerId, start + Duration.ofMillis(300L).toNanos()));
    }

    @Test
    void closingTheMenuReleasesTheGuard() {
        PurchaseClickGuard guard = new PurchaseClickGuard();
        UUID playerId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        assertTrue(guard.tryAcquire(playerId, 100L));
        guard.release(playerId);
        assertTrue(guard.tryAcquire(playerId, 101L));
    }
}
