package dev.lifesteal.balancetop.model;

/** A balance entry with its permanent place in the server top 100. */
public record RankedBalanceEntry(int rank, BalanceEntry entry) {

    public RankedBalanceEntry {
        if (rank < 1) {
            throw new IllegalArgumentException("Rank must be positive");
        }
    }
}
