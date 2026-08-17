package dev.lifesteal.souls.data;

public record KillRewardResult(
        boolean rewarded, long credited, long balance, long cooldownRemainingMillis) {
}
