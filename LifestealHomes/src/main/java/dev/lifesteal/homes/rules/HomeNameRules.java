package dev.lifesteal.homes.rules;

import java.util.Locale;

public final class HomeNameRules {

    private HomeNameRules() {}

    public static boolean isValid(String name, int maximumLength) {
        if (name == null || name.isBlank() || name.length() > maximumLength) {
            return false;
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_' && character != '-') {
                return false;
            }
        }
        return true;
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
