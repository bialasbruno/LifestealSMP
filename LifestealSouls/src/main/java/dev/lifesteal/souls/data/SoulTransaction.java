package dev.lifesteal.souls.data;

import java.time.Instant;
import java.util.UUID;

public record SoulTransaction(
        long id,
        UUID playerId,
        SoulTransactionType type,
        long amount,
        long balanceAfter,
        String reference,
        Instant createdAt) {
}
