package dev.lifesteal.souls.afk;

import dev.lifesteal.souls.config.SoulsSettings;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfkZoneTest {

    @Test
    void centerUsesTheMiddleOfInclusiveBlockBounds() {
        assertEquals(100.5D, AfkZone.centerCoordinate(100, 100));
        assertEquals(110.5D, AfkZone.centerCoordinate(100, 120));
        assertEquals(-4.5D, AfkZone.centerCoordinate(-10, 0));
    }

    @Test
    void destinationUsesTheConfiguredFixedLocationWhenEnabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("afk-zone.teleport.use-custom-location", true);
        config.set("afk-zone.teleport.x", 100.5D);
        config.set("afk-zone.teleport.y", 70D);
        config.set("afk-zone.teleport.z", -20.5D);
        config.set("afk-zone.teleport.yaw", 180D);
        config.set("afk-zone.teleport.pitch", 15D);
        SoulsSettings settings = SoulsSettings.load(
                config, Logger.getLogger("AfkZoneTest"));

        Location destination = new AfkZone().destination(null, settings, 45F);

        assertEquals(100.5D, destination.getX());
        assertEquals(70D, destination.getY());
        assertEquals(-20.5D, destination.getZ());
        assertEquals(180F, destination.getYaw());
        assertEquals(15F, destination.getPitch());
    }
}
