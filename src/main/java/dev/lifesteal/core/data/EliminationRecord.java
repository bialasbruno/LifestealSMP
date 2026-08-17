package dev.lifesteal.core.data;

import java.time.Instant;
import java.util.UUID;

/** A persisted temporary elimination created by a one-heart PvP death. */
public record EliminationRecord(UUID playerUuid, String lastKnownName, Instant bannedUntil) {
}
