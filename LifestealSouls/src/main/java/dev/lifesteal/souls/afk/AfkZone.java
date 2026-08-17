package dev.lifesteal.souls.afk;

import dev.lifesteal.souls.config.SoulsSettings;
import org.bukkit.Location;

/** Inclusive, block-aligned cuboid configured as the dedicated AFK zone. */
public final class AfkZone {

    public boolean isConfigured(SoulsSettings settings) {
        return settings.afkZoneEnabled() && !settings.afkWorldName().isBlank();
    }

    public boolean contains(Location location, SoulsSettings settings) {
        if (!isConfigured(settings)
                || !location.getWorld().getName().equalsIgnoreCase(settings.afkWorldName())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= settings.afkMinimumX()
                && x <= settings.afkMaximumX()
                && y >= settings.afkMinimumY()
                && y <= settings.afkMaximumY()
                && z >= settings.afkMinimumZ()
                && z <= settings.afkMaximumZ();
    }
}
