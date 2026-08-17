package dev.lifesteal.souls.data;

public record PlaytimeRewardResult(
        long balance,
        long credited,
        long activeProgressMillis,
        long completedIntervals) {
}
