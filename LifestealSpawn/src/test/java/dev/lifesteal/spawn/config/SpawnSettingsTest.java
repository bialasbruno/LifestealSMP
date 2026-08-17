package dev.lifesteal.spawn.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnSettingsTest {

    private static final Logger LOGGER = Logger.getLogger("SpawnSettingsTest");

    @Test
    void defaultsProtectTheSpawnWorld() {
        SpawnSettings settings = SpawnSettings.load(new YamlConfiguration(), LOGGER);

        assertTrue(settings.voidRescueEnabled());
        assertEquals(Set.of("spawn"), settings.enabledWorldNames());
        assertEquals(5, settings.triggerOffsetBelowMinHeight());
        assertEquals("spawn", settings.destinationWorldName());
        assertTrue(settings.useWorldSpawn());
        assertTrue(settings.afkRescueEnabled());
        assertEquals(3D, settings.afkRescueTriggerY());
    }

    @Test
    void worldMatchingIsCaseInsensitive() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("void-rescue.enabled-worlds", List.of("Spawn", "Lobby_TWO"));
        config.set("void-rescue.destination.world", "Spawn");

        SpawnSettings settings = SpawnSettings.load(config, LOGGER);

        assertTrue(settings.isEnabledInWorld("SPAWN"));
        assertTrue(settings.isEnabledInWorld("lobby_two"));
        assertEquals("Spawn", settings.destinationWorldName());
    }

    @Test
    void invalidSafetyValuesUseDefaults() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("void-rescue.enabled-worlds", List.of("", "   "));
        config.set("void-rescue.trigger-offset-below-min-height", -1);
        config.set("void-rescue.destination.world", "   ");
        config.set("void-rescue.destination.y", Double.NaN);
        config.set("afk-rescue.trigger-y", 3_000D);
        config.set("sound.pitch", 3D);

        SpawnSettings settings = SpawnSettings.load(config, LOGGER);

        assertEquals(Set.of("spawn"), settings.enabledWorldNames());
        assertEquals(5, settings.triggerOffsetBelowMinHeight());
        assertEquals("spawn", settings.destinationWorldName());
        assertEquals(100D, settings.destinationY());
        assertEquals(3D, settings.afkRescueTriggerY());
        assertEquals(1.2F, settings.soundPitch());
    }
}
