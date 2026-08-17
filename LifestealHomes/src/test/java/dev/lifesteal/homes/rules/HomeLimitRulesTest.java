package dev.lifesteal.homes.rules;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeLimitRulesTest {

    @Test
    void usesDefaultWithoutRankPermission() {
        assertEquals(1, HomeLimitRules.resolve(1, 100, permission -> false));
    }

    @Test
    void highestGrantedLimitWins() {
        Set<String> permissions = Set.of("lifestealhomes.limit.3", "lifestealhomes.limit.10");

        assertEquals(10, HomeLimitRules.resolve(1, 100, permissions::contains));
    }

    @Test
    void aLowerRankNeverReducesTheDefault() {
        assertEquals(5, HomeLimitRules.resolve(
                5, 100, permission -> permission.equals("lifestealhomes.limit.3")));
    }

    @Test
    void unlimitedPermissionOverridesNumericLimits() {
        assertEquals(HomeLimitRules.UNLIMITED, HomeLimitRules.resolve(
                1,
                100,
                permission -> permission.equals("lifestealhomes.limit.unlimited")));
    }
}
