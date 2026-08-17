package dev.lifesteal.scoreboard.render;

import dev.lifesteal.scoreboard.placeholder.PlaceholderTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreboardLineLayoutTest {

    @Test
    void duplicateVisualLinesReceiveUniqueEntries() {
        ScoreboardLineLayout layout = ScoreboardLineLayout.create(List.of(
                PlaceholderTemplate.compile("Same"),
                PlaceholderTemplate.compile("Same")));

        assertEquals("Same", layout.lines().get(0).template().raw());
        assertEquals("Same", layout.lines().get(1).template().raw());
        assertNotEquals(layout.lines().get(0).entry(), layout.lines().get(1).entry());
    }

    @Test
    void blankLinesRemainSeparateAndOrdered() {
        ScoreboardLineLayout layout = ScoreboardLineLayout.create(List.of(
                PlaceholderTemplate.compile("Top"),
                PlaceholderTemplate.compile(""),
                PlaceholderTemplate.compile("Bottom")));

        assertEquals(List.of("Top", "", "Bottom"), layout.lines().stream()
                .map(line -> line.template().raw())
                .toList());
        assertEquals(List.of(3, 2, 1), layout.lines().stream()
                .map(ScoreboardLineLayout.LineDefinition::score)
                .toList());
    }

    @Test
    void rejectsMoreThanMinecraftSidebarLimit() {
        List<PlaceholderTemplate> templates = java.util.stream.IntStream.range(0, 16)
                .mapToObj(index -> PlaceholderTemplate.compile(Integer.toString(index)))
                .toList();

        assertThrows(IllegalArgumentException.class, () -> ScoreboardLineLayout.create(templates));
    }
}
