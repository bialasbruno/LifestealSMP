package dev.lifesteal.balancetop.config;

import org.bukkit.configuration.file.FileConfiguration;

/** Validated runtime settings for the balance leaderboard. */
public record BalanceTopSettings(
        long cacheMillis,
        boolean includeZeroBalances,
        String noPermissionMessage,
        String playerOnlyMessage,
        String economyUnavailableMessage,
        String usageMessage) {

    public static BalanceTopSettings load(FileConfiguration config) {
        long cacheSeconds = Math.max(1L, config.getLong("leaderboard.cache-seconds", 30L));
        return new BalanceTopSettings(
                Math.multiplyExact(cacheSeconds, 1_000L),
                config.getBoolean("leaderboard.include-zero-balances", false),
                config.getString("messages.no-permission",
                        "<red>You do not have permission to use this command.</red>"),
                config.getString("messages.player-only",
                        "<red>This command can only be used by a player.</red>"),
                config.getString("messages.economy-unavailable",
                        "<red>The economy is currently unavailable. Please try again later.</red>"),
                config.getString("messages.usage", "<red>Usage: /baltop</red>"));
    }
}
