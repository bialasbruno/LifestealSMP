package dev.lifesteal.balancetop.service;

import dev.lifesteal.balancetop.model.BalanceEntry;
import dev.lifesteal.balancetop.model.LeaderboardSort;
import dev.lifesteal.balancetop.model.RankedBalanceEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Pure ranking and presentation sorting rules. */
public final class BalanceRanking {

    private BalanceRanking() {}

    public static List<RankedBalanceEntry> top(
            Collection<BalanceEntry> entries, int limit, boolean includeZeroBalances) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        Comparator<BalanceEntry> rankingOrder = Comparator
                .comparingDouble(BalanceEntry::balance)
                .reversed()
                .thenComparing(BalanceEntry::playerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(BalanceEntry::playerId);

        List<BalanceEntry> ordered = entries.stream()
                .filter(entry -> Double.isFinite(entry.balance()))
                .filter(entry -> includeZeroBalances || entry.balance() > 0.0D)
                .sorted(rankingOrder)
                .limit(limit)
                .toList();

        List<RankedBalanceEntry> ranked = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            ranked.add(new RankedBalanceEntry(index + 1, ordered.get(index)));
        }
        return List.copyOf(ranked);
    }

    public static List<RankedBalanceEntry> sorted(
            List<RankedBalanceEntry> topEntries, LeaderboardSort sort) {
        return topEntries.stream().sorted(sort.comparator()).toList();
    }
}
