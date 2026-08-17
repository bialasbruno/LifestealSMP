package dev.lifesteal.balancetop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalanceFormatterTest {

    @Test
    void formatsWholeAndFractionalBalances() {
        assertEquals("1,250", BalanceFormatter.format(1_250.0D));
        assertEquals("1,250.5", BalanceFormatter.format(1_250.50D));
        assertEquals("1,250.57", BalanceFormatter.format(1_250.567D));
    }
}
