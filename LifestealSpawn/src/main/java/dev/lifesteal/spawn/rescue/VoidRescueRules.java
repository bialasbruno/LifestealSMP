package dev.lifesteal.spawn.rescue;

import dev.lifesteal.spawn.config.SpawnSettings;

public final class VoidRescueRules {

    private VoidRescueRules() {}

    public static boolean shouldRescue(
            SpawnSettings settings, String worldName, int minimumHeight, double playerY) {
        if (!settings.voidRescueEnabled() || !settings.isEnabledInWorld(worldName)) {
            return false;
        }
        double triggerY = minimumHeight - settings.triggerOffsetBelowMinHeight();
        return playerY <= triggerY;
    }
}
