package dev.lifesteal.souls.afk;

import dev.lifesteal.souls.config.SoulsSettings;
import org.bukkit.Location;
import org.bukkit.World;

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

    public Location center(World world, SoulsSettings settings, float yaw) {
        return new Location(
                world,
                centerCoordinate(settings.afkMinimumX(), settings.afkMaximumX()),
                centerCoordinate(settings.afkMinimumY(), settings.afkMaximumY()),
                centerCoordinate(settings.afkMinimumZ(), settings.afkMaximumZ()),
                yaw,
                0.0F);
    }

    public Location destination(World world, SoulsSettings settings, float fallbackYaw) {
        if (!settings.afkUseCustomTeleportLocation()) {
            return center(world, settings, fallbackYaw);
        }
        return new Location(
                world,
                settings.afkTeleportX(),
                settings.afkTeleportY(),
                settings.afkTeleportZ(),
                settings.afkTeleportYaw(),
                settings.afkTeleportPitch());
    }

    static double centerCoordinate(int minimum, int maximum) {
        return minimum + (((long) maximum - minimum + 1L) / 2.0D);
    }
}
