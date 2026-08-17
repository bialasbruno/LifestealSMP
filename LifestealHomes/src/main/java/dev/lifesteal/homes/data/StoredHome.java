package dev.lifesteal.homes.data;

import java.util.UUID;

public record StoredHome(
        UUID playerId,
        String key,
        String name,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        long createdAt,
        long updatedAt) {}
