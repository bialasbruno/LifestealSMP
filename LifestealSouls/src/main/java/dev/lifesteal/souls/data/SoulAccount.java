package dev.lifesteal.souls.data;

import java.util.Objects;
import java.util.UUID;

public record SoulAccount(
        UUID playerId,
        String lastKnownName,
        long balance,
        long activeProgressMillis) {

    public SoulAccount {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(lastKnownName, "lastKnownName");
        if (balance < 0L || activeProgressMillis < 0L) {
            throw new IllegalArgumentException("Account values cannot be negative");
        }
    }
}
