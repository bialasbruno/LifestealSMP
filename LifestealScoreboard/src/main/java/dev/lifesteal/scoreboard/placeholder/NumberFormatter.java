package dev.lifesteal.scoreboard.placeholder;

import java.text.NumberFormat;
import java.util.Locale;

/** Consistent integer formatting with English thousands separators. */
public final class NumberFormatter {

    private static final ThreadLocal<NumberFormat> INTEGER_FORMAT = ThreadLocal.withInitial(() -> {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        format.setGroupingUsed(true);
        format.setMaximumFractionDigits(0);
        return format;
    });

    private NumberFormatter() {
    }

    public static String format(long value) {
        return INTEGER_FORMAT.get().format(value);
    }
}
