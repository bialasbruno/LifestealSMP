package dev.lifesteal.souls.command;

import java.util.UUID;

record PlayerTarget(UUID playerId, String lastKnownName) {
}
