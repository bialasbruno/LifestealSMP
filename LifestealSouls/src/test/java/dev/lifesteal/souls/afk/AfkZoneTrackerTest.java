package dev.lifesteal.souls.afk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfkZoneTrackerTest {

    @Test
    void formatsCountdownAsMinutesAndSeconds() {
        assertEquals("02:00", AfkZoneTracker.formatTime(120L));
        assertEquals("01:59", AfkZoneTracker.formatTime(119L));
        assertEquals("00:01", AfkZoneTracker.formatTime(1L));
    }
}
