package dev.lifesteal.balancetop.model;

import java.util.Comparator;

/** Sort modes available from the leaderboard GUI. */
public enum LeaderboardSort {
    BALANCE_DESCENDING("Balance: highest first"),
    BALANCE_ASCENDING("Balance: lowest first"),
    NAME_ASCENDING("Name: A-Z"),
    NAME_DESCENDING("Name: Z-A");

    private static final Comparator<RankedBalanceEntry> BY_NAME = Comparator.comparing(
            entry -> entry.entry().playerName(), String.CASE_INSENSITIVE_ORDER);

    private final String displayName;

    LeaderboardSort(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public LeaderboardSort next() {
        LeaderboardSort[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public Comparator<RankedBalanceEntry> comparator() {
        return switch (this) {
            case BALANCE_DESCENDING -> Comparator.comparingInt(RankedBalanceEntry::rank);
            case BALANCE_ASCENDING -> Comparator.comparingInt(RankedBalanceEntry::rank).reversed();
            case NAME_ASCENDING -> BY_NAME.thenComparingInt(RankedBalanceEntry::rank);
            case NAME_DESCENDING -> BY_NAME.reversed().thenComparingInt(RankedBalanceEntry::rank);
        };
    }
}
