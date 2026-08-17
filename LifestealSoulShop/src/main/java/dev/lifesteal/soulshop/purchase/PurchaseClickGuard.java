package dev.lifesteal.soulshop.purchase;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Prevents an accidental double click from being interpreted as two purchases. */
public final class PurchaseClickGuard {

    private static final long COOLDOWN_NANOS = Duration.ofMillis(300L).toNanos();

    private final Map<UUID, Long> lastPurchases = new HashMap<>();

    public boolean tryAcquire(UUID playerId, long nowNanos) {
        Long previous = lastPurchases.get(playerId);
        if (previous != null && nowNanos - previous < COOLDOWN_NANOS) {
            return false;
        }
        lastPurchases.put(playerId, nowNanos);
        return true;
    }

    public void release(UUID playerId) {
        lastPurchases.remove(playerId);
    }
}
