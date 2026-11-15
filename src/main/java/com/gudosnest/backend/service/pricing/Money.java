package com.gudosnest.backend.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * The one money format used everywhere: ₱27,400.00 (negative amounts as -₱2,480.00).
 * {@link #plain} is the same digits without the peso sign, for text fields.
 */
public final class Money {

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)));

    private Money() {
    }

    /** ₱27,400.00; null shows as ₱0.00. */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "₱0.00";
        }
        return (amount.signum() < 0 ? "-₱" : "₱") + plain(amount.abs());
    }

    /** 27,400.00 (no peso sign); null shows as an empty string. */
    public static String plain(BigDecimal amount) {
        return amount == null ? "" : FORMAT.get().format(amount.setScale(2, RoundingMode.HALF_UP));
    }
}
