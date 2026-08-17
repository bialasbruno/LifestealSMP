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
        assertEquals(1L, settings.afkRewardAmount());
        assertEquals(120_000L, settings.afkRewardIntervalMillis());
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
}
