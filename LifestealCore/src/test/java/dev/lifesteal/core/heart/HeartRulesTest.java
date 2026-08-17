package dev.lifesteal.core.heart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartRulesTest {

    private static final int MIN = 1;
    private static final int MAX = 20;

    @Test
    void tenHeartsPvpDeathBecomesNineAndDrops() {
        assertEquals(9, HeartRules.applyPvpDeath(10, MIN));
        assertTrue(HeartRules.shouldDropBrokenHeart(10, MIN));
    }

    @Test
    void twoHeartsPvpDeathBecomesOneAndDrops() {
        assertEquals(1, HeartRules.applyPvpDeath(2, MIN));
        assertTrue(HeartRules.shouldDropBrokenHeart(2, MIN));
    }

    @Test
    void oneHeartPvpDeathStaysAtOneAndDoesNotDrop() {
        assertEquals(1, HeartRules.applyPvpDeath(1, MIN));
        assertFalse(HeartRules.shouldDropBrokenHeart(1, MIN));
        assertTrue(HeartRules.shouldEliminateOnPvpDeath(1, MIN));
    }

    @Test
    void heartsNeverGoBelowConfiguredMinimum() {
        // Defensive: even if state somehow drifted below the minimum, the floor still holds.
        assertEquals(MIN, HeartRules.applyPvpDeath(MIN, MIN));
        assertEquals(MIN, HeartRules.applyPvpDeath(0, MIN));
    }

    @Test
    void nineteenHeartsPlusHeartBecomesTwenty() {
        assertTrue(HeartRules.canConsumeHeart(19, MAX));
        assertEquals(20, HeartRules.applyHeartConsumption(19, MAX));
    }

    @Test
    void twentyHeartsPlusHeartStaysAtTwentyAndIsRejected() {
        assertFalse(HeartRules.canConsumeHeart(20, MAX));
        assertEquals(20, HeartRules.applyHeartConsumption(20, MAX));
    }

    @Test
    void clampRespectsBounds() {
        assertEquals(MIN, HeartRules.clamp(-5, MIN, MAX));
        assertEquals(MAX, HeartRules.clamp(999, MIN, MAX));
        assertEquals(14, HeartRules.clamp(14, MIN, MAX));
    }

    @Test
    void onlyMaximumHeartVictimIsEligibleForReviveTotem() {
        assertFalse(HeartRules.isReviveTotemDropEligible(19, MAX));
        assertTrue(HeartRules.isReviveTotemDropEligible(20, MAX));
    }

    @Test
    void victimAboveMinimumIsNotEliminated() {
        assertFalse(HeartRules.shouldEliminateOnPvpDeath(2, MIN));
    }
}
