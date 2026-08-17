package dev.lifesteal.homes.rules;

import java.util.function.Predicate;

public final class HomeLimitRules {

    public static final int UNLIMITED = Integer.MAX_VALUE;

    private HomeLimitRules() {}

    public static int resolve(int defaultLimit, int maximumPermissionLimit, Predicate<String> hasPermission) {
        if (hasPermission.test("lifestealhomes.limit.unlimited")) {
            return UNLIMITED;
        }

        int result = Math.max(0, defaultLimit);
        for (int limit = 1; limit <= Math.max(1, maximumPermissionLimit); limit++) {
            if (hasPermission.test("lifestealhomes.limit." + limit)) {
                result = Math.max(result, limit);
            }
        }
        return result;
    }
}
