package dev.lifesteal.balancetop.service;

import dev.lifesteal.balancetop.economy.VaultEconomyGateway;
import dev.lifesteal.balancetop.model.BalanceEntry;
import dev.lifesteal.balancetop.model.RankedBalanceEntry;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

/** Loads and caches the 100 richest known players from the regular economy. */
public final class BalanceLeaderboardService {

    public static final int MAX_ENTRIES = 100;

    private final Plugin plugin;
    private final Server server;
    private final VaultEconomyGateway economy;
    private final long cacheMillis;
    private final boolean includeZeroBalances;

    private List<RankedBalanceEntry> cached = List.of();
    private long cachedAtMillis = Long.MIN_VALUE;
    private long cachedEconomyRevision = Long.MIN_VALUE;

    public BalanceLeaderboardService(
            Plugin plugin,
            VaultEconomyGateway economy,
            long cacheMillis,
            boolean includeZeroBalances) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
        this.economy = Objects.requireNonNull(economy, "economy");
        this.cacheMillis = cacheMillis;
        this.includeZeroBalances = includeZeroBalances;
    }

    public Optional<List<RankedBalanceEntry>> load(boolean forceRefresh) {
        if (!economy.available()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        long economyRevision = economy.revision();
        if (!forceRefresh
                && cachedEconomyRevision == economyRevision
                && now - cachedAtMillis < cacheMillis) {
            return Optional.of(cached);
        }

        List<BalanceEntry> entries = new ArrayList<>();
        for (OfflinePlayer player : server.getOfflinePlayers()) {
            String name = player.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                double balance = economy.balance(player);
                entries.add(new BalanceEntry(player.getUniqueId(), name, balance));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "Could not read economy balance for " + name + ".",
                        exception);
            }
        }

        cached = BalanceRanking.top(entries, MAX_ENTRIES, includeZeroBalances);
        cachedAtMillis = now;
        cachedEconomyRevision = economyRevision;
        return Optional.of(cached);
    }
}
