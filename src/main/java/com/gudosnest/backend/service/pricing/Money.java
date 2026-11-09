package com.gudosnest.backend.service.pricing;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** The one money format used everywhere: ₱27,400.00 (negative amounts as -₱2,480.00). */
public final class Money {

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)));

    private Money() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "₱0.00";
        }
        String digits = FORMAT.get().format(amount.abs());
        return (amount.signum() < 0 ? "-₱" : "₱") + digits;
    }
}
