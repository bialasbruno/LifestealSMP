package dev.lifesteal.homes.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HomesSettingsTest {

    @Test
    void providesCompleteDefaults() {
        HomesSettings settings = HomesSettings.load(new YamlConfiguration());

        assertEquals(1, settings.defaultLimit());
        assertEquals(3, settings.teleportDelaySeconds());
        assertEquals(Material.ENDER_PEARL, settings.menu().homeMaterial());
    }

    @Test
    void loadsCustomLimitsAndMenuMaterials() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("limits.default", 3);
        config.set("limits.maximum-permission-limit", 25);
        config.set("menu.home-material", "COMPASS");

        HomesSettings settings = HomesSettings.load(config);

        assertEquals(3, settings.defaultLimit());
        assertEquals(25, settings.maximumPermissionLimit());
        assertEquals(Material.COMPASS, settings.menu().homeMaterial());
    }

    @Test
    void rejectsInvalidConfiguration() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("teleport.movement-tolerance", -1.0D);

        assertThrows(IllegalArgumentException.class, () -> HomesSettings.load(config));
    }
}
