package com.gudosnest.frontend.components;

import com.gudosnest.backend.service.RuleException;
import com.gudosnest.backend.service.pricing.Money;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Reads a peso amount typed into a text field, with or without thousands commas:
 *   "2800", "2,800", " 1,250,000.50 "  ->  2800.00 / 2800.00 / 1250000.50
 * A wrong value is never guessed at: the person is told what to fix. To put a stored amount back
 * into a field, use {@link Money#plain} ("1,250,000.00").
 */
public final class MoneyInput {

    /** Largest amount a DECIMAL(10,2) column holds: rates, fees, prices. */
    public static final BigDecimal MAX = new BigDecimal("99999999.99");

    /** Largest amount a DECIMAL(12,2) column holds: a property's acquisition cost. */
    public static final BigDecimal MAX_COST = new BigDecimal("9999999999.99");

    /** Digits with a comma every 3 digits, or no commas at all; then up to 2 decimals. */
    private static final Pattern AMOUNT = Pattern.compile("(\\d{1,3}(,\\d{3})+|\\d+)(\\.\\d{1,2})?");

    private static final String EXAMPLE = " For example 2,800 or 2,800.50.";

    private MoneyInput() {
    }

    /** A required amount up to {@link #MAX}. */
    public static BigDecimal parse(String text, String fieldName) {
        return parse(text, fieldName, MAX);
    }

    /** An optional amount up to {@link #MAX}: an empty field means 0. */
    public static BigDecimal parseOrZero(String text, String fieldName) {
        return text == null || text.isBlank() ? BigDecimal.ZERO.setScale(2) : parse(text, fieldName, MAX);
    }

    /**
     * A required amount of 0 up to {@code max}. Throws a {@link RuleException} whose message names
     * the field (for example "Nightly rate") when the text is empty or not a valid amount.
     */
    public static BigDecimal parse(String text, String fieldName, BigDecimal max) {
        String s = text == null ? "" : text.strip();
        if (s.isEmpty()) {
            throw new RuleException("Enter the " + fieldName.toLowerCase() + "." + EXAMPLE);
        }
        if (s.startsWith("-")) {
            throw new RuleException(fieldName + " can't be negative.");
        }
        if (!s.matches("[0-9.,]+")) {
            throw new RuleException(fieldName + " can only have numbers, commas and a decimal point." + EXAMPLE);
        }
        int dot = s.indexOf('.');
        if (dot != s.lastIndexOf('.')) {
            throw new RuleException(fieldName + " can have only one decimal point." + EXAMPLE);
        }
        if (dot >= 0 && s.length() - dot - 1 > 2) {
            throw new RuleException(fieldName + " can have at most 2 decimal places." + EXAMPLE);
        }
        if (!AMOUNT.matcher(s).matches()) {
            throw new RuleException(fieldName + " isn't a valid amount. Put commas every 3 digits (or leave them out)"
                    + " and a number before and after the decimal point." + EXAMPLE);
        }
        BigDecimal amount = new BigDecimal(s.replace(",", "")).setScale(2);
        if (amount.compareTo(max) > 0) {
            throw new RuleException(fieldName + " is too large. The most it can be is " + Money.plain(max) + ".");
        }
        return amount;
    }
}
