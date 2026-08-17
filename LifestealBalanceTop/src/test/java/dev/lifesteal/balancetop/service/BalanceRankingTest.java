package dev.lifesteal.balancetop.service;

import dev.lifesteal.balancetop.model.BalanceEntry;
import dev.lifesteal.balancetop.model.LeaderboardSort;
import dev.lifesteal.balancetop.model.RankedBalanceEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalanceRankingTest {

    @Test
    void keepsOnlyTheRichestOneHundredPlayers() {
        List<BalanceEntry> entries = new ArrayList<>();
        for (int value = 1; value <= 125; value++) {
            entries.add(entry("Player" + value, value));
        }

        List<RankedBalanceEntry> ranked = BalanceRanking.top(entries, 100, false);

        assertEquals(100, ranked.size());
        assertEquals(125.0D, ranked.getFirst().entry().balance());
        assertEquals(26.0D, ranked.getLast().entry().balance());
        assertEquals(100, ranked.getLast().rank());
    }

    @Test
    void lowestFirstReversesOnlyTheSelectedTopOneHundred() {
        List<RankedBalanceEntry> ranked = BalanceRanking.top(List.of(
                entry("Rich", 300), entry("Middle", 200), entry("Low", 100)), 100, false);

        List<RankedBalanceEntry> sorted =
                BalanceRanking.sorted(ranked, LeaderboardSort.BALANCE_ASCENDING);

        assertEquals("Low", sorted.getFirst().entry().playerName());
        assertEquals(3, sorted.getFirst().rank());
        assertEquals("Rich", sorted.getLast().entry().playerName());
    }

    @Test
    void supportsAlphabeticalSortingWithoutChangingRanks() {
        List<RankedBalanceEntry> ranked = BalanceRanking.top(List.of(
                entry("Zed", 300), entry("Alice", 100), entry("Bob", 200)), 100, false);

        List<RankedBalanceEntry> sorted =
                BalanceRanking.sorted(ranked, LeaderboardSort.NAME_ASCENDING);

        assertEquals(List.of("Alice", "Bob", "Zed"), sorted.stream()
                .map(item -> item.entry().playerName())
                .toList());
        assertEquals(3, sorted.getFirst().rank());
    }

    @Test
    void excludesZeroAndInvalidBalancesByDefault() {
        List<RankedBalanceEntry> ranked = BalanceRanking.top(List.of(
                entry("Positive", 10),
                entry("Zero", 0),
                entry("Negative", -1),
                entry("Invalid", Double.NaN)), 100, false);

        assertEquals(List.of("Positive"), ranked.stream()
                .map(item -> item.entry().playerName())
                .toList());
    }

    private static BalanceEntry entry(String name, double balance) {
        return new BalanceEntry(UUID.nameUUIDFromBytes(name.getBytes()), name, balance);
    }
}
