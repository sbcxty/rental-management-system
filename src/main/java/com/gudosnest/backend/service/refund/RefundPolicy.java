package com.gudosnest.backend.service.refund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

// PATTERN: Strategy
// How much a renter gets back depends on how many days before check-in they cancel. Each tier is
// its own strategy class with the same method; RefundPolicy.forCancellation() picks the strategy,
// and the caller just asks it for a quote. The tiers live in exactly one place, so the Cancellations
// page, the booking dialog and the BookingService always agree.
public interface RefundPolicy {

    /** The 20% fee kept on a partial refund. */
    BigDecimal CANCELLATION_FEE_RATE = new BigDecimal("0.20");

    /** Tier names as stored in Refunds.policy_tier ("none" is never stored: no refund row). */
    String FULL = "full";
    String PARTIAL = "partial";
    String NONE = "none";
    String AUTOMATIC = "automatic";

    /**
     * The policy text. Every screen shows these exact lines (title, then text), so the wording
     * can never differ between screens.
     */
    List<String[]> TEXT = List.of(
            new String[]{"Full refund:", "cancelled 7 or more days before check-in."},
            new String[]{"Partial refund:", "cancelled 3 to 6 days before check-in. A 20% cancellation fee of the booking total is deducted."},
            new String[]{"No refund:", "cancelled within 2 days of check-in."});

    /** The approval rule shown under every Pay button and on pending bookings. */
    String APPROVAL_NOTE = "Owner approves within 48 hours, or you're refunded in full.";

    /** Result of applying a policy to a booking total. */
    record Quote(String tier, String tierLabel, BigDecimal fee, BigDecimal refund) {
        public boolean refundsAnything() {
            return refund.signum() > 0;
        }
    }

    Quote quote(BigDecimal total);

    /** Picks the tier for a renter cancelling an approved booking on {@code today}. */
    static RefundPolicy forCancellation(LocalDate today, LocalDate checkInDate) {
        long days = daysBefore(today, checkInDate);
        if (days >= 7) {
            return new FullRefund();
        }
        if (days >= 3) {
            return new PartialRefund();
        }
        return new NoRefund();
    }

    /** Calendar days from today to the check-in date. */
    static long daysBefore(LocalDate today, LocalDate checkInDate) {
        return ChronoUnit.DAYS.between(today, checkInDate);
    }

    // ---------------------------------------------------------------------------------------

    final class FullRefund implements RefundPolicy {
        @Override
        public Quote quote(BigDecimal total) {
            return new Quote(FULL, "Full refund", BigDecimal.ZERO, total);
        }
    }

    final class PartialRefund implements RefundPolicy {
        @Override
        public Quote quote(BigDecimal total) {
            BigDecimal fee = total.multiply(CANCELLATION_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            return new Quote(PARTIAL, "Partial refund", fee, total.subtract(fee));
        }
    }

    final class NoRefund implements RefundPolicy {
        @Override
        public Quote quote(BigDecimal total) {
            return new Quote(NONE, "No refund", total, BigDecimal.ZERO);
        }
    }

    /** Pending booking cancelled, rejected or expired, or owner cancelled: everything back, no review. */
    final class AutomaticRefund implements RefundPolicy {
        @Override
        public Quote quote(BigDecimal total) {
            return new Quote(AUTOMATIC, "Full refund", BigDecimal.ZERO, total);
        }
    }
}
