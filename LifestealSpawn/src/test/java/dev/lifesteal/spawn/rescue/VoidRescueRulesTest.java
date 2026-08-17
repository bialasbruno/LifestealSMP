package dev.lifesteal.spawn.rescue;

import dev.lifesteal.spawn.config.SpawnSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoidRescueRulesTest {

    private static final Logger LOGGER = Logger.getLogger("VoidRescueRulesTest");

    @Test
    void triggersAtConfiguredDistanceBelowMinimumHeight() {
        SpawnSettings settings = SpawnSettings.load(new YamlConfiguration(), LOGGER);

        assertFalse(VoidRescueRules.shouldRescue(settings, "spawn", -64, -68.99D));
        assertTrue(VoidRescueRules.shouldRescue(settings, "spawn", -64, -69D));
        assertTrue(VoidRescueRules.shouldRescue(settings, "SPAWN", -64, -100D));
    }

    @Test
    void ignoresWorldsOutsideTheConfiguredList() {
        SpawnSettings settings = SpawnSettings.load(new YamlConfiguration(), LOGGER);

        assertFalse(VoidRescueRules.shouldRescue(settings, "world", -64, -100D));
    }

    @Test
    void disabledFeatureNeverTriggers() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("void-rescue.enabled", false);
        SpawnSettings settings = SpawnSettings.load(config, LOGGER);

        assertFalse(VoidRescueRules.shouldRescue(settings, "spawn", -64, -100D));
    }
}
