package dev.lifesteal.souls.afk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfkZoneTest {

    @Test
    void centerUsesTheMiddleOfInclusiveBlockBounds() {
        assertEquals(100.5D, AfkZone.centerCoordinate(100, 100));
        assertEquals(110.5D, AfkZone.centerCoordinate(100, 120));
        assertEquals(-4.5D, AfkZone.centerCoordinate(-10, 0));
    }
}
