package dev.lifesteal.scoreboard.placeholder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberFormatterTest {

    @Test
    void formatsWholeNumbersWithEnglishGrouping() {
        assertEquals("0", NumberFormatter.format(0));
        assertEquals("950", NumberFormatter.format(950));
        assertEquals("1,250", NumberFormatter.format(1_250));
        assertEquals("25,000", NumberFormatter.format(25_000));
        assertEquals("1,000,000", NumberFormatter.format(1_000_000));
    }
}
