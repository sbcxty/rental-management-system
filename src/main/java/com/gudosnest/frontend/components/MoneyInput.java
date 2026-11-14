package com.gudosnest.frontend.components;

import com.gudosnest.backend.service.RuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strict reading and writing of a peso amount typed into a text field, with thousands commas:
 *   "1250000", "1,250,000", " 1,250,000.50 "  ->  1250000.00 / 1250000.50
 * Unlike {@link Formats#parseMoney}, a wrong value is not guessed at: the person is told what to fix.
 */
public final class MoneyInput {

    /** Digits with commas every 3 digits, or digits with no commas; then up to 2 decimals. */
    private static final Pattern AMOUNT = Pattern.compile("(\\d{1,3}(,\\d{3})+|\\d+)(\\.\\d{1,2})?");

    /** DECIMAL(12,2) holds at most 10 digits before the decimal point. */
    private static final int MAX_WHOLE_DIGITS = 10;

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)));

    private MoneyInput() {
    }

    /**
     * Reads an amount of 0 or more. Throws a {@link RuleException} with a message that names the
     * field (for example "Acquisition cost") when the text is empty or not a valid amount.
     */
    public static BigDecimal parse(String text, String fieldName) {
        String example = " For example 1,250,000 or 1,250,000.50.";
        String s = text == null ? "" : text.strip();
        if (s.startsWith("₱")) {
            s = s.substring(1).strip();
        }
        if (s.isEmpty()) {
            throw new RuleException("Enter the " + fieldName.toLowerCase() + "." + example);
        }
        if (s.startsWith("-")) {
            throw new RuleException(fieldName + " can't be negative.");
        }
        if (!s.matches("[0-9.,]+")) {
            throw new RuleException(fieldName + " can only have numbers, commas and a decimal point." + example);
        }
        int dot = s.indexOf('.');
        if (dot != s.lastIndexOf('.')) {
            throw new RuleException(fieldName + " can have only one decimal point." + example);
        }
        if (dot >= 0 && s.length() - dot - 1 > 2) {
            throw new RuleException(fieldName + " can have at most 2 decimal places." + example);
        }
        if (!AMOUNT.matcher(s).matches()) {
            throw new RuleException(fieldName + " isn't a valid amount. Put commas every 3 digits (or leave them out)"
                    + " and a number before and after the decimal point." + example);
        }
        String plain = s.replace(",", "");
        String whole = dot >= 0 ? plain.substring(0, plain.indexOf('.')) : plain;
        if (whole.replaceFirst("^0+(?=\\d)", "").length() > MAX_WHOLE_DIGITS) {
            throw new RuleException(fieldName + " is too large.");
        }
        return new BigDecimal(plain).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** "1,250,000.00" for putting a stored amount back into a text field (no peso sign). */
    public static String format(BigDecimal amount) {
        return amount == null ? "" : FORMAT.get().format(amount.setScale(2, RoundingMode.HALF_UP));
    }
}
