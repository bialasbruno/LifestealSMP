package dev.lifesteal.balancetop.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Stable display formatting for regular economy balances. */
public final class BalanceFormatter {

    private BalanceFormatter() {}

    public static String format(double balance) {
        DecimalFormat formatter = new DecimalFormat(
                "#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return formatter.format(balance);
    }
}
