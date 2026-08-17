package dev.lifesteal.scoreboard.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardSettingsTest {

    private static final Logger LOGGER =
            Logger.getLogger(ScoreboardSettingsTest.class.getName());

    @Test
    void invalidValuesUseSafeFallbacks() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", "not-a-boolean");
        config.set("update.interval-ticks", 1);
        config.set("scoreboard.title", "<red>Unclosed");
        config.set("scoreboard.lines", List.of("<green>Valid</green>"));

        ScoreboardSettings settings = ScoreboardSettings.load(config, LOGGER);

        assertTrue(settings.enabled());
        assertEquals(ScoreboardSettings.DEFAULT_INTERVAL_TICKS, settings.updateIntervalTicks());
        assertEquals(1, settings.lineLayout().lines().size());
    }

    @Test
    void linesAreTruncatedToMinecraftSidebarLimit() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("scoreboard.lines", IntStream.range(0, 20)
                .mapToObj(index -> "Line " + index)
                .toList());

        ScoreboardSettings settings = ScoreboardSettings.load(config, LOGGER);

        assertEquals(15, settings.lineLayout().lines().size());
        assertEquals("Line 0", settings.lineLayout().lines().getFirst().template().raw());
        assertEquals("Line 14", settings.lineLayout().lines().getLast().template().raw());
    }
}
