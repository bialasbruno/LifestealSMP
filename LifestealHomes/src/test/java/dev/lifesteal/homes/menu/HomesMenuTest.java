package dev.lifesteal.homes.menu;

import dev.lifesteal.homes.rules.HomeLimitRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomesMenuTest {

    @Test
    void paginatesLargeRankLimits() {
        assertEquals(1, HomesMenu.pageCount(0, 1));
        assertEquals(1, HomesMenu.pageCount(36, 36));
        assertEquals(2, HomesMenu.pageCount(0, 37));
        assertEquals(3, HomesMenu.pageCount(90, 90));
    }

    @Test
    void unlimitedPlayersAlwaysSeeOneFreeSlot() {
        assertEquals(1, HomesMenu.pageCount(0, HomeLimitRules.UNLIMITED));
        assertEquals(2, HomesMenu.pageCount(36, HomeLimitRules.UNLIMITED));
    }
}
