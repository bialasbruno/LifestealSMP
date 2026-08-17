package dev.lifesteal.sell.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellActionTest {

    @Test
    void noArgumentsOpenTheGui() {
        assertEquals(SellAction.OPEN_GUI, SellAction.from(new String[0]));
    }

    @Test
    void handArgumentSellsTheHeldMaterial() {
        assertEquals(
                SellAction.SELL_HELD_MATERIAL,
                SellAction.from(new String[] {"HaNd"}));
    }

    @Test
    void helpAndUnknownArgumentsShowHelp() {
        assertEquals(SellAction.SHOW_HELP, SellAction.from(new String[] {"help"}));
        assertEquals(SellAction.SHOW_HELP, SellAction.from(new String[] {"unknown"}));
        assertEquals(SellAction.SHOW_HELP, SellAction.from(new String[] {"hand", "extra"}));
    }
}
