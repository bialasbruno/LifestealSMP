package dev.lifesteal.souls.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulsSettingsTest {

    @Test
    void defaultsMatchTheAgreedEconomy() {
        SoulsSettings settings = SoulsSettings.load(
                new YamlConfiguration(), Logger.getLogger("SoulsSettingsTest"));

        assertTrue(settings.playtimeEnabled());
        assertEquals(50L, settings.playtimeRewardAmount());
        assertEquals(3_600_000L, settings.playtimeRewardIntervalMillis());
        assertTrue(settings.killRewardEnabled());
        assertEquals(3L, settings.killRewardAmount());
        assertEquals(3_600_000L, settings.killCooldownMillis());
        assertFalse(settings.afkZoneEnabled());
        assertTrue(settings.afkPvpDisabled());
        assertTrue(settings.afkPortalRepelEnabled());
        assertEquals(1.15D, settings.afkPortalHorizontalStrength());
        assertEquals(0.35D, settings.afkPortalVerticalStrength());
        assertEquals(1L, settings.afkRewardAmount());
        assertEquals(120_000L, settings.afkRewardIntervalMillis());
        assertEquals("", settings.afkWorldName());
        assertFalse(settings.afkUseCustomTeleportLocation());
    }

    @Test
    void invalidRewardValuesUseSafeFallbacks() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("balance.maximum", 100L);
        config.set("playtime.reward-amount", 101L);
        config.set("player-kill.reward-amount", 0L);
        config.set("playtime.flush-interval-seconds", 1L);

        SoulsSettings settings = SoulsSettings.load(
                config, Logger.getLogger("SoulsSettingsTest"));

        assertEquals(50L, settings.playtimeRewardAmount());
        assertEquals(3L, settings.killRewardAmount());
        assertEquals(60_000L, settings.flushIntervalMillis());
    }

    @Test
    void afkCuboidCoordinatesAreNormalized() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("afk-zone.world", "world");
        config.set("afk-zone.disable-pvp", false);
        config.set("afk-zone.minimum.x", 20);
        config.set("afk-zone.minimum.y", 90);
        config.set("afk-zone.minimum.z", 40);
        config.set("afk-zone.maximum.x", 10);
        config.set("afk-zone.maximum.y", 70);
        config.set("afk-zone.maximum.z", 30);

        SoulsSettings settings = SoulsSettings.load(
                config, Logger.getLogger("SoulsSettingsTest"));

        assertEquals("world", settings.afkWorldName());
        assertFalse(settings.afkPvpDisabled());
        assertEquals(10, settings.afkMinimumX());
        assertEquals(70, settings.afkMinimumY());
        assertEquals(30, settings.afkMinimumZ());
        assertEquals(20, settings.afkMaximumX());
        assertEquals(90, settings.afkMaximumY());
        assertEquals(40, settings.afkMaximumZ());
    }

    @Test
    void customAfkTeleportLocationIsLoadedIndependentlyFromTheCuboid() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("afk-zone.teleport.use-custom-location", true);
        config.set("afk-zone.teleport.x", 12.5D);
        config.set("afk-zone.teleport.y", 75D);
        config.set("afk-zone.teleport.z", -8.5D);
        config.set("afk-zone.teleport.yaw", 90D);
        config.set("afk-zone.teleport.pitch", -10D);

        SoulsSettings settings = SoulsSettings.load(
                config, Logger.getLogger("SoulsSettingsTest"));

        assertTrue(settings.afkUseCustomTeleportLocation());
        assertEquals(12.5D, settings.afkTeleportX());
        assertEquals(75D, settings.afkTeleportY());
        assertEquals(-8.5D, settings.afkTeleportZ());
        assertEquals(90F, settings.afkTeleportYaw());
        assertEquals(-10F, settings.afkTeleportPitch());
    }

    @Test
    void customPortalRepelStrengthIsLoaded() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("afk-zone.nether-portal-repel.enabled", false);
        config.set("afk-zone.nether-portal-repel.horizontal-strength", 1.8D);
        config.set("afk-zone.nether-portal-repel.vertical-strength", 0.6D);

        SoulsSettings settings = SoulsSettings.load(
                config, Logger.getLogger("SoulsSettingsTest"));

        assertFalse(settings.afkPortalRepelEnabled());
        assertEquals(1.8D, settings.afkPortalHorizontalStrength());
        assertEquals(0.6D, settings.afkPortalVerticalStrength());
    }
}
