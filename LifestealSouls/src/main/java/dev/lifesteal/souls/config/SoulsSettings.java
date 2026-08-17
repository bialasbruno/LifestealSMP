package dev.lifesteal.souls.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public record SoulsSettings(
        long maximumBalance,
        boolean playtimeEnabled,
        long playtimeRewardAmount,
        long playtimeRewardIntervalMillis,
        long idleTimeoutMillis,
        long flushIntervalMillis,
        boolean killRewardEnabled,
        long killRewardAmount,
        long killCooldownMillis,
        boolean afkZoneEnabled,
        long afkRewardAmount,
        long afkRewardIntervalMillis,
        String balanceMessage,
        String playtimeRewardMessage,
        String killRewardMessage,
        String noPermissionMessage,
        String playerOnlyMessage,
        String invalidCommandMessage) {

    public SoulsSettings {
        Objects.requireNonNull(balanceMessage, "balanceMessage");
        Objects.requireNonNull(playtimeRewardMessage, "playtimeRewardMessage");
        Objects.requireNonNull(killRewardMessage, "killRewardMessage");
        Objects.requireNonNull(noPermissionMessage, "noPermissionMessage");
        Objects.requireNonNull(playerOnlyMessage, "playerOnlyMessage");
        Objects.requireNonNull(invalidCommandMessage, "invalidCommandMessage");
    }

    public static SoulsSettings load(FileConfiguration config, Logger logger) {
        long maximumBalance = readLong(
                config, logger, "balance.maximum", 1L, Long.MAX_VALUE, 1_000_000_000L);
        long playtimeAmount = readLong(
                config,
                logger,
                "playtime.reward-amount",
                1L,
                maximumBalance,
                Math.min(50L, maximumBalance));
        long playtimeSeconds = readLong(
                config, logger, "playtime.reward-interval-seconds", 60L, 604_800L, 3_600L);
        long idleSeconds = readLong(
                config, logger, "playtime.idle-timeout-seconds", 30L, 86_400L, 300L);
        long flushSeconds = readLong(
                config, logger, "playtime.flush-interval-seconds", 5L, 600L, 60L);
        long killAmount = readLong(
                config,
                logger,
                "player-kill.reward-amount",
                1L,
                maximumBalance,
                Math.min(3L, maximumBalance));
        long killCooldownSeconds = readLong(
                config,
                logger,
                "player-kill.same-victim-cooldown-seconds",
                1L,
                604_800L,
                3_600L);
        long afkAmount = readLong(
                config,
                logger,
                "afk-zone.reward-amount",
                1L,
                maximumBalance,
                1L);
        long afkSeconds = readLong(
                config, logger, "afk-zone.reward-interval-seconds", 60L, 86_400L, 120L);

        return new SoulsSettings(
                maximumBalance,
                config.getBoolean("playtime.enabled", true),
                playtimeAmount,
                TimeUnit.SECONDS.toMillis(playtimeSeconds),
                TimeUnit.SECONDS.toMillis(idleSeconds),
                TimeUnit.SECONDS.toMillis(flushSeconds),
                config.getBoolean("player-kill.enabled", true),
                killAmount,
                TimeUnit.SECONDS.toMillis(killCooldownSeconds),
                config.getBoolean("afk-zone.enabled", false),
                afkAmount,
                TimeUnit.SECONDS.toMillis(afkSeconds),
                config.getString(
                        "messages.balance",
                        "<aqua>Souls:</aqua> <white>{balance}</white>"),
                config.getString(
                        "messages.playtime-reward",
                        "<green>+{amount} Souls</green> for active playtime."),
                config.getString(
                        "messages.kill-reward",
                        "<green>+{amount} Souls</green> for killing {victim}."),
                config.getString(
                        "messages.no-permission",
                        "<red>You do not have permission to use this command.</red>"),
                config.getString(
                        "messages.player-only",
                        "<red>This command can only be used by a player.</red>"),
                config.getString(
                        "messages.invalid-command",
                        "<red>Invalid command. Use /soulsadmin help.</red>"));
    }

    private static long readLong(
            FileConfiguration config,
            Logger logger,
            String path,
            long minimum,
            long maximum,
            long fallback) {
        long value = config.getLong(path, fallback);
        if (value < minimum || value > maximum) {
            logger.warning("'" + path + "' must be between " + minimum + " and " + maximum
                    + "; using " + fallback + '.');
            return fallback;
        }
        return value;
    }
}
