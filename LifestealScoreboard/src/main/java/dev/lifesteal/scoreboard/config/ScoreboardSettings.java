package dev.lifesteal.scoreboard.config;

import dev.lifesteal.scoreboard.placeholder.PlaceholderTemplate;
import dev.lifesteal.scoreboard.render.ScoreboardLineLayout;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Validated immutable view of the scoreboard configuration. */
public record ScoreboardSettings(
        boolean enabled,
        long updateIntervalTicks,
        PlaceholderTemplate title,
        ScoreboardLineLayout lineLayout) {

    public static final long DEFAULT_INTERVAL_TICKS = 20L;
    public static final long MIN_INTERVAL_TICKS = 10L;
    public static final long MAX_INTERVAL_TICKS = 1_200L;

    private static final String DEFAULT_TITLE =
            "<gradient:#ff3b3b:#ff8c42><bold>LIFESTEAL SMP</bold></gradient>";
    private static final List<String> DEFAULT_LINES = List.of(
            "",
            "<red>❤</red> Hearts: <white>%lifesteal_hearts%</white>",
            "<green>$</green> Money: <white>%lifesteal_money%</white>",
            "<aqua>✦</aqua> Souls: <white>%lifesteal_souls%</white>",
            "",
            "<yellow>⚔</yellow> Kills: <white>%lifesteal_kills%</white>",
            "<gray>☠</gray> Deaths: <white>%lifesteal_deaths%</white>",
            "",
            "<green>Online:</green> <white>%server_online%/%server_max%</white>",
            "<gray>play.lifesteal.pl</gray>");
    private static final MiniMessage STRICT_MINI_MESSAGE =
            MiniMessage.builder().strict(true).build();

    public static ScoreboardSettings load(FileConfiguration config, Logger logger) {
        boolean enabled = readBoolean(config, logger);
        long interval = readInterval(config, logger);
        PlaceholderTemplate title = readTitle(config, logger);
        List<PlaceholderTemplate> lines = readLines(config, logger);
        return new ScoreboardSettings(
                enabled, interval, title, ScoreboardLineLayout.create(lines));
    }

    private static boolean readBoolean(FileConfiguration config, Logger logger) {
        Object value = config.get("enabled");
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean enabled) {
            return enabled;
        }
        logger.warning("'enabled' must be true or false; using true.");
        return true;
    }

    private static long readInterval(FileConfiguration config, Logger logger) {
        Object value = config.get("update.interval-ticks");
        if (value == null) {
            return DEFAULT_INTERVAL_TICKS;
        }
        if (!(value instanceof Number number)) {
            logger.warning("'update.interval-ticks' must be an integer; using 20.");
            return DEFAULT_INTERVAL_TICKS;
        }
        long interval = number.longValue();
        if (interval < MIN_INTERVAL_TICKS || interval > MAX_INTERVAL_TICKS) {
            logger.warning("'update.interval-ticks' must be between 10 and 1200; using 20.");
            return DEFAULT_INTERVAL_TICKS;
        }
        return interval;
    }

    private static PlaceholderTemplate readTitle(FileConfiguration config, Logger logger) {
        Object value = config.get("scoreboard.title");
        if (value == null) {
            return compileValidated(DEFAULT_TITLE);
        }
        if (!(value instanceof String title)) {
            logger.warning("'scoreboard.title' must be text; using the default title.");
            return compileValidated(DEFAULT_TITLE);
        }
        try {
            return compileValidated(title);
        } catch (RuntimeException exception) {
            logger.warning("Invalid MiniMessage in 'scoreboard.title'; using the default title: "
                    + exception.getMessage());
            return compileValidated(DEFAULT_TITLE);
        }
    }

    private static List<PlaceholderTemplate> readLines(
            FileConfiguration config, Logger logger) {
        Object value = config.get("scoreboard.lines");
        if (value == null) {
            return compileDefaults();
        }
        if (!(value instanceof List<?> configuredLines)) {
            logger.warning("'scoreboard.lines' must be a list; using the default lines.");
            return compileDefaults();
        }

        List<PlaceholderTemplate> lines = new ArrayList<>();
        int configuredCount = configuredLines.size();
        if (configuredCount > ScoreboardLineLayout.MAX_LINES) {
            logger.warning("A sidebar supports at most 15 lines; extra configured lines were ignored.");
        }
        int limit = Math.min(configuredCount, ScoreboardLineLayout.MAX_LINES);
        for (int index = 0; index < limit; index++) {
            Object lineValue = configuredLines.get(index);
            if (!(lineValue instanceof String line)) {
                logger.warning("Scoreboard line " + (index + 1) + " is not text and was ignored.");
                continue;
            }
            try {
                lines.add(compileValidated(line));
            } catch (RuntimeException exception) {
                logger.warning("Invalid MiniMessage on scoreboard line " + (index + 1)
                        + "; the line was ignored: " + exception.getMessage());
            }
        }
        if (!configuredLines.isEmpty() && lines.isEmpty()) {
            logger.warning("No valid scoreboard lines remained; using the default lines.");
            return compileDefaults();
        }
        return List.copyOf(lines);
    }

    private static PlaceholderTemplate compileValidated(String value) {
        PlaceholderTemplate template = PlaceholderTemplate.compile(value);
        STRICT_MINI_MESSAGE.deserialize(template.render(key -> "0"));
        return template;
    }

    private static List<PlaceholderTemplate> compileDefaults() {
        return DEFAULT_LINES.stream().map(ScoreboardSettings::compileValidated).toList();
    }
}
