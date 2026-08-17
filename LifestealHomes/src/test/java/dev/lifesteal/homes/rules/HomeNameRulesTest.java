package dev.lifesteal.homes.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeNameRulesTest {

    @Test
    void acceptsSimpleMinecraftFriendlyNames() {
        assertTrue(HomeNameRules.isValid("base", 16));
        assertTrue(HomeNameRules.isValid("Nether_2", 16));
        assertTrue(HomeNameRules.isValid("end-farm", 16));
    }

    @Test
    void rejectsWhitespaceFormattingAndLongNames() {
        assertFalse(HomeNameRules.isValid("", 16));
        assertFalse(HomeNameRules.isValid("my base", 16));
        assertFalse(HomeNameRules.isValid("<red>base", 16));
        assertFalse(HomeNameRules.isValid("abcdefghijklmnopq", 16));
    }

    @Test
    void keysAreCaseInsensitive() {
        assertEquals("my_home", HomeNameRules.key("My_Home"));
    }
}
