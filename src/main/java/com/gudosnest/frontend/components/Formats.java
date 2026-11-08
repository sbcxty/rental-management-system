package com.gudosnest.frontend.components;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        String text = "₱" + MONEY.format(amount.abs());
        return amount.signum() < 0 ? "-" + text : text;
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
