package dev.lifesteal.scoreboard.render;

import dev.lifesteal.scoreboard.config.ScoreboardSettings;
import dev.lifesteal.scoreboard.placeholder.PlaceholderContext;
import dev.lifesteal.scoreboard.placeholder.PlaceholderResolver;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Objects;

/** One reusable objective and scoreboard for a single online player. */
final class PlayerSidebar {

    private static final String OBJECTIVE_NAME = "ls_sidebar";

    private final Scoreboard previousScoreboard;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final LineState[] lineStates = new LineState[ScoreboardLineLayout.MAX_LINES];
    private String lastRenderedTitle;

    PlayerSidebar(Player player, ScoreboardManager scoreboardManager) {
        previousScoreboard = player.getScoreboard();
        scoreboard = scoreboardManager.getNewScoreboard();
        objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME, Criteria.DUMMY, Component.empty(), RenderType.INTEGER);
        objective.numberFormat(NumberFormat.blank());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    void attach(Player player) {
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }
    }

    void render(
            Player player,
            ScoreboardSettings settings,
            PlaceholderResolver placeholders,
            MiniMessage miniMessage) {
        PlaceholderContext context = new PlaceholderContext(player);
        String renderedTitle = placeholders.resolve(settings.title(), context);
        if (!Objects.equals(renderedTitle, lastRenderedTitle)) {
            objective.displayName(miniMessage.deserialize(renderedTitle));
            lastRenderedTitle = renderedTitle;
        }

        var lines = settings.lineLayout().lines();
        for (int index = lines.size(); index < lineStates.length; index++) {
            LineState stale = lineStates[index];
            if (stale != null) {
                scoreboard.resetScores(stale.entry);
                lineStates[index] = null;
            }
        }

        for (int index = 0; index < lines.size(); index++) {
            ScoreboardLineLayout.LineDefinition definition = lines.get(index);
            LineState state = lineStates[index];
            if (state == null || !state.entry.equals(definition.entry())) {
                if (state != null) {
                    scoreboard.resetScores(state.entry);
                }
                Score score = objective.getScore(definition.entry());
                score.numberFormat(NumberFormat.blank());
                state = new LineState(definition.entry(), score);
                lineStates[index] = state;
            }
            if (!state.score.isScoreSet() || state.score.getScore() != definition.score()) {
                state.score.setScore(definition.score());
            }

            String renderedLine = placeholders.resolve(definition.template(), context);
            if (!Objects.equals(renderedLine, state.lastRenderedText)) {
                state.score.customName(miniMessage.deserialize(renderedLine));
                state.lastRenderedText = renderedLine;
            }
        }
    }

    void close(Player player) {
        if (player.getScoreboard() == scoreboard) {
            player.setScoreboard(previousScoreboard);
        }
        destroy();
    }

    void destroy() {
        objective.unregister();
    }

    private static final class LineState {

        private final String entry;
        private final Score score;
        private String lastRenderedText;

        private LineState(String entry, Score score) {
            this.entry = entry;
            this.score = score;
        }
    }
}
