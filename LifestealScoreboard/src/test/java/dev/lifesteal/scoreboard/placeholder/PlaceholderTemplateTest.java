package dev.lifesteal.scoreboard.placeholder;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderTemplateTest {

    @Test
    void replacesKnownPlaceholdersAndPreservesUnknownOnes() {
        PlaceholderTemplate template = PlaceholderTemplate.compile(
                "%player_name%: %lifesteal_hearts% / %unknown_value%");
        Map<String, String> values = Map.of(
                "player_name", "Bruno",
                "lifesteal_hearts", "10");

        assertEquals(
                "Bruno: 10 / %unknown_value%",
                template.render(values::get));
        assertTrue(template.dynamic());
    }

    @Test
    void staticTemplatesReuseTheirOriginalString() {
        PlaceholderTemplate template = PlaceholderTemplate.compile("Static footer");

        assertSame(template.raw(), template.render(key -> "unused"));
        assertFalse(template.dynamic());
    }

    @Test
    void replacesRepeatedPlaceholderOccurrences() {
        PlaceholderTemplate template = PlaceholderTemplate.compile(
                "%server_online%/%server_online%");

        assertEquals("17/17", template.render(key -> "17"));
    }
}
