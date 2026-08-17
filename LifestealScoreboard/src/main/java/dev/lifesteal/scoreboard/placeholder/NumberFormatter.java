package dev.lifesteal.scoreboard.placeholder;

import java.text.NumberFormat;
import java.util.Locale;

/** Consistent balance and integer formatting with English thousands separators. */
public final class NumberFormatter {

    private static final ThreadLocal<NumberFormat> INTEGER_FORMAT = ThreadLocal.withInitial(() -> {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        format.setGroupingUsed(true);
        format.setMaximumFractionDigits(0);
        return format;
    });
    private static final ThreadLocal<NumberFormat> BALANCE_FORMAT = ThreadLocal.withInitial(() -> {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format;
    });

    private NumberFormatter() {
    }

    public static String format(long value) {
        return INTEGER_FORMAT.get().format(value);
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        return BALANCE_FORMAT.get().format(value);
    }
}
