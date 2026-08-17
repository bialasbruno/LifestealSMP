package dev.lifesteal.souls.data;

public final class BalanceLimitException extends IllegalArgumentException {

    public BalanceLimitException(long maximumBalance) {
        super("The operation would exceed the maximum balance of " + maximumBalance);
    }
}
