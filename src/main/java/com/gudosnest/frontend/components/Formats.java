package com.gudosnest.frontend.components;

import com.gudosnest.backend.service.pricing.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * One place for how money, dates, times and booking references look on screen.
 *   ₱27,400.00   Nov 18, 2025   3:00 PM   Ref #GN-2025-8841
 */
public final class Formats {

    private Formats() {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    /** ₱27,400.00 (see Money, the single money format used by backend and UI). */
    public static String money(BigDecimal amount) {
        return amount == null ? "" : Money.format(amount);
    }

    /**
     * Reads an amount the person typed ("2800", "₱2,800.00", "2,800.5"). Returns null when it is
     * empty or not a number, so the caller can say what to fix.
     */
    public static BigDecimal parseMoney(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.replace("₱", "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "2800.00" for putting a stored amount back into a text field. */
    public static String plainMoney(BigDecimal amount) {
        return amount == null ? "" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Whole pesos for chart labels: ₱68k, ₱1.2M, ₱950. */
    public static String moneyShort(BigDecimal amount) {
        double v = amount.doubleValue();
        double abs = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (abs >= 1_000_000) {
            return sign + "₱" + trim(abs / 1_000_000) + "M";
        }
        if (abs >= 1_000) {
            return sign + "₱" + trim(abs / 1_000) + "k";
        }
        return sign + "₱" + Math.round(abs);
    }

    private static String trim(double value) {
        return value >= 100 || value == Math.floor(value) ? String.valueOf(Math.round(value))
                : String.format(Locale.US, "%.1f", value);
    }

    /** "72%" */
    public static String percent(double share) {
        return Math.round(share * 100) + "%";
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String date(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE.format(dateTime);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime == null ? "" : TIME.format(dateTime);
    }

    /** "Monday, November 17, 2025" */
    public static String longDate(LocalDate date) {
        return DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH).format(date);
    }

    /** "Nov 18, 2025 · 3:00 PM" */
    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : date(dateTime) + " · " + time(dateTime);
    }

    /** "November 2025" */
    public static String monthYear(LocalDate date) {
        return MONTH_YEAR.format(date);
    }

    /** "Nov" */
    public static String shortMonth(LocalDate date) {
        return SHORT_MONTH.format(date);
    }

    /** "Nov 18 – Nov 22, 2025", or "Dec 28, 2025 – Jan 2, 2026" across years. */
    public static String dateRange(LocalDate start, LocalDate end) {
        if (start.getYear() == end.getYear()) {
            return MONTH_DAY.format(start) + " – " + DATE.format(end);
        }
        return DATE.format(start) + " – " + DATE.format(end);
    }

    /** "GN-2025-8841": year the booking was made, then the booking id. */
    public static String refCode(int bookingId, LocalDateTime createdAt) {
        return String.format("GN-%d-%04d", createdAt.getYear(), bookingId);
    }

    /** "Ref #GN-2025-8841" */
    public static String ref(int bookingId, LocalDateTime createdAt) {
        return "Ref #" + refCode(bookingId, createdAt);
    }

    /** "1 night", "4 nights" */
    public static String nights(long nights) {
        return nights + (nights == 1 ? " night" : " nights");
    }

    /** "1 guest", "3 guests" */
    public static String guests(int count) {
        return count + (count == 1 ? " guest" : " guests");
    }
}
