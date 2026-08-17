package dev.lifesteal.scoreboard.render;

import dev.lifesteal.scoreboard.placeholder.PlaceholderTemplate;

import java.util.ArrayList;
import java.util.List;

/** Stable internal entries and scores for sidebar lines, independent of their visual text. */
public final class ScoreboardLineLayout {

    public static final int MAX_LINES = 15;

    private final List<LineDefinition> lines;

    private ScoreboardLineLayout(List<LineDefinition> lines) {
        this.lines = lines;
    }

    public static ScoreboardLineLayout create(List<PlaceholderTemplate> templates) {
        if (templates.size() > MAX_LINES) {
            throw new IllegalArgumentException("A sidebar supports at most 15 lines");
        }
        List<LineDefinition> lines = new ArrayList<>(templates.size());
        for (int index = 0; index < templates.size(); index++) {
            lines.add(new LineDefinition(
                    hiddenEntry(index), templates.size() - index, templates.get(index)));
        }
        return new ScoreboardLineLayout(List.copyOf(lines));
    }

    public List<LineDefinition> lines() {
        return lines;
    }

    private static String hiddenEntry(int index) {
        return "\u00a7" + Character.forDigit(index, 16) + "\u00a7r";
    }

    public record LineDefinition(String entry, int score, PlaceholderTemplate template) {
    }
}
