package dev.lifesteal.soulitems.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoulPickaxeDefinitionTest {

    @Test
    void usesTheAgreedEnchantments() {
        assertEquals(5, SoulPickaxeDefinition.EFFICIENCY_LEVEL);
        assertEquals(3, SoulPickaxeDefinition.FORTUNE_LEVEL);
        assertEquals(3, SoulPickaxeDefinition.UNBREAKING_LEVEL);
        assertEquals(1, SoulPickaxeDefinition.MENDING_LEVEL);
    }
}
