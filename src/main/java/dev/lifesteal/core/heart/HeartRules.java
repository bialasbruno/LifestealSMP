package dev.lifesteal.core.heart;

/**
 * Pure domain logic for the lifesteal heart mechanic.
 *
 * <p>This class intentionally has zero dependency on the Bukkit/Paper API so it can be
 * unit-tested with plain JUnit and reused later if a more complex death-attribution system
 * (e.g. "hit by a player, died from fall damage 8 seconds later") is introduced.</p>
 */
public final class HeartRules {

    private HeartRules() {
    }

    /**
     * Clamps a value between {@code min} and {@code max} (inclusive).
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * Applies a PvP death to a victim's current max hearts.
     *
     * @param currentHearts the victim's max hearts before the death
     * @param minimumHearts the configured minimum hearts a player can hold
     * @return the victim's max hearts after the death
     */
    public static int applyPvpDeath(int currentHearts, int minimumHearts) {
        if (currentHearts <= minimumHearts) {
            return minimumHearts;
        }
        return currentHearts - 1;
    }

    /**
     * Whether a Broken Heart item should drop for a PvP death.
     *
     * <p>A Broken Heart never drops if the victim was already at the configured minimum,
     * to prevent infinite farming of a player who has nothing left to lose.</p>
     *
     * @param heartsBeforeDeath the victim's max hearts before the death
     * @param minimumHearts     the configured minimum hearts a player can hold
     */
    public static boolean shouldDropBrokenHeart(int heartsBeforeDeath, int minimumHearts) {
        return heartsBeforeDeath > minimumHearts;
    }

    /** Whether a one-heart PvP death should temporarily eliminate the victim. */
    public static boolean shouldEliminateOnPvpDeath(int heartsBeforeDeath, int minimumHearts) {
        return heartsBeforeDeath <= minimumHearts;
    }

    /** Whether the victim is strong enough to be eligible for their seasonal Revive Totem drop. */
    public static boolean isReviveTotemDropEligible(int heartsBeforeDeath, int maximumHearts) {
        return heartsBeforeDeath >= maximumHearts;
    }

    /**
     * Whether a player currently below the maximum can consume a Heart item.
     */
    public static boolean canConsumeHeart(int currentHearts, int maximumHearts) {
        return currentHearts < maximumHearts;
    }

    /**
     * Applies the consumption of a Heart item to a player's current max hearts.
     * Returns the unchanged value if the player is already at the maximum.
     */
    public static int applyHeartConsumption(int currentHearts, int maximumHearts) {
        if (!canConsumeHeart(currentHearts, maximumHearts)) {
            return currentHearts;
        }
        return Math.min(currentHearts + 1, maximumHearts);
    }
}
