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
        boolean afkPvpDisabled,
        long afkRewardAmount,
        long afkRewardIntervalMillis,
        String afkWorldName,
        int afkMinimumX,
        int afkMinimumY,
        int afkMinimumZ,
        int afkMaximumX,
        int afkMaximumY,
        int afkMaximumZ,
        String balanceMessage,
        String playtimeRewardMessage,
        String killRewardMessage,
        String afkCountdownMessage,
        String afkRewardMessage,
        String afkTeleportedMessage,
        String afkUnavailableMessage,
        String afkPvpDisabledMessage,
        String noPermissionMessage,
        String playerOnlyMessage,
        String invalidCommandMessage) {

    public SoulsSettings {
        Objects.requireNonNull(balanceMessage, "balanceMessage");
        Objects.requireNonNull(playtimeRewardMessage, "playtimeRewardMessage");
        Objects.requireNonNull(killRewardMessage, "killRewardMessage");
        Objects.requireNonNull(afkWorldName, "afkWorldName");
        Objects.requireNonNull(afkCountdownMessage, "afkCountdownMessage");
        Objects.requireNonNull(afkRewardMessage, "afkRewardMessage");
        Objects.requireNonNull(afkTeleportedMessage, "afkTeleportedMessage");
        Objects.requireNonNull(afkUnavailableMessage, "afkUnavailableMessage");
        Objects.requireNonNull(afkPvpDisabledMessage, "afkPvpDisabledMessage");
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
                config.getBoolean("afk-zone.disable-pvp", true),
                afkAmount,
                TimeUnit.SECONDS.toMillis(afkSeconds),
                config.getString("afk-zone.world", "").trim(),
                Math.min(
                        config.getInt("afk-zone.minimum.x", 0),
                        config.getInt("afk-zone.maximum.x", 0)),
                Math.min(
                        config.getInt("afk-zone.minimum.y", 0),
                        config.getInt("afk-zone.maximum.y", 0)),
                Math.min(
                        config.getInt("afk-zone.minimum.z", 0),
                        config.getInt("afk-zone.maximum.z", 0)),
                Math.max(
                        config.getInt("afk-zone.minimum.x", 0),
                        config.getInt("afk-zone.maximum.x", 0)),
                Math.max(
                        config.getInt("afk-zone.minimum.y", 0),
                        config.getInt("afk-zone.maximum.y", 0)),
                Math.max(
                        config.getInt("afk-zone.minimum.z", 0),
                        config.getInt("afk-zone.maximum.z", 0)),
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
                        "messages.afk-countdown",
                        "<aqua>In <white>{time}</white> you will receive"
                                + " <white>{amount}</white> Souls.</aqua>"),
                config.getString(
                        "messages.afk-reward",
                        "<green>+{amount} Souls</green> <gray>for staying in the AFK zone.</gray>"),
                config.getString(
                        "messages.afk-teleported",
                        "<aqua>AFK zone:</aqua> <white>first reward in {time}</white>"),
                config.getString(
                        "messages.afk-unavailable",
                        "<red>The AFK zone is not configured or its world is unavailable.</red>"),
                config.getString(
                        "messages.afk-pvp-disabled",
                        "<red>PvP is disabled in the AFK zone.</red>"),
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
