package dev.lifesteal.soulitems.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulItemsSettingsTest {

    private static final Logger LOGGER = Logger.getLogger("SoulItemsSettingsTest");

    @Test
    void defaultsProvideACompleteSoulPickaxeDescription() {
        SoulItemsSettings settings = SoulItemsSettings.load(new YamlConfiguration(), LOGGER);

        assertTrue(settings.soulPickaxeName().contains("Soul Pickaxe"));
        assertEquals(3, settings.soulPickaxeLore().size());
    }

    @Test
    void customNameAndLoreAreLoaded() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("soul-pickaxe.name", "<aqua>Custom Pickaxe</aqua>");
        config.set("soul-pickaxe.lore", List.of("<gray>First line</gray>", ""));

        SoulItemsSettings settings = SoulItemsSettings.load(config, LOGGER);

        assertEquals("<aqua>Custom Pickaxe</aqua>", settings.soulPickaxeName());
        assertEquals(List.of("<gray>First line</gray>", ""), settings.soulPickaxeLore());
    }

    @Test
    void explicitlyEmptyLoreIsAllowed() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("soul-pickaxe.lore", List.of());

        SoulItemsSettings settings = SoulItemsSettings.load(config, LOGGER);

        assertTrue(settings.soulPickaxeLore().isEmpty());
    }
}
