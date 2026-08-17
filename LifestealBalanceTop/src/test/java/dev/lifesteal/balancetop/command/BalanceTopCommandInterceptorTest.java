package dev.lifesteal.balancetop.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceTopCommandInterceptorTest {

    @Test
    void recognizesEssentialsAndNamespacedBalanceTopLabels() {
        assertTrue(BalanceTopCommandInterceptor.isBalanceTopLabel("baltop"));
        assertTrue(BalanceTopCommandInterceptor.isBalanceTopLabel("BALANCETOP"));
        assertTrue(BalanceTopCommandInterceptor.isBalanceTopLabel("essentials:baltop"));
        assertTrue(BalanceTopCommandInterceptor.isBalanceTopLabel("ebalancetop"));
        assertFalse(BalanceTopCommandInterceptor.isBalanceTopLabel("balance"));
    }
}
