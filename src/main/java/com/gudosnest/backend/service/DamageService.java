package com.gudosnest.backend.service;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.repository.MoneyRepository;
import com.gudosnest.backend.repository.OwnerOpsRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Damage reports, for both portals. The owner files a report on a stay and can dismiss it; the
 * renter pays it. A report left unpaid for 48 hours suspends the renter: they can't book until
 * every overdue report is paid or dismissed. The suspension isn't stored; it is worked out from
 * the reports each time, so it lifts by itself.
 */
public class DamageService {

    /** Longest damage description the owner can write. */
    public static final int MAX_DESCRIPTION = 1000;

    private final OwnerOpsRepository ops = new OwnerOpsRepository();
    private final MoneyRepository money = new MoneyRepository();

    // ---------------------------------------------------------------------- lists

    public List<DamageReport> forOwner(int ownerId) {
        return ops.damageReports(ownerId);
    }

    public List<DamageReport> forRenter(int renterId) {
        return ops.damageReportsForRenter(renterId);
    }

    public DamageReport byId(int reportId, List<DamageReport> reports) {
        return reports.stream().filter(r -> r.id() == reportId).findFirst()
                .orElseThrow(() -> new RuleException("That report no longer exists."));
    }

    /** The renter's unpaid reports past their 48 hours. Any at all means the renter can't book. */
    public List<DamageReport> overdue(int renterId) {
        LocalDateTime now = LocalDateTime.now();
        return forRenter(renterId).stream().filter(r -> r.isOverdue(now)).toList();
    }

    public boolean isSuspended(int renterId) {
        return !overdue(renterId).isEmpty();
    }

    /** Stops a suspended renter from booking (called by BookingService.book). */
    public void checkCanBook(int renterId) {
        List<DamageReport> late = overdue(renterId);
        if (!late.isEmpty()) {
            throw new RuleException("Booking is paused: " + (late.size() == 1 ? "a damage report is" : late.size()
                    + " damage reports are") + " unpaid after " + DamageReport.PAY_WITHIN_HOURS
                    + " hours. Pay it on the Damage reports page to book again.");
        }
    }

    // ---------------------------------------------------------------------- owner

    /** Bookings a report can be filed against: the stay has started (staying, check-out due, completed). */
    public List<Booking> reportableBookings(List<Booking> ownerBookings) {
        LocalDateTime now = LocalDateTime.now();
        return ownerBookings.stream()
                .filter(b -> isReportable(b, now))
                .sorted((a, b) -> b.getCheckOut().compareTo(a.getCheckOut()))
                .toList();
    }

    private static boolean isReportable(Booking b, LocalDateTime now) {
        return switch (b.getStage(now)) {
            case STAYING, CHECKOUT_DUE, COMPLETED -> true;
            default -> false;
        };
    }

    public void file(int ownerId, Booking booking, String description, String photoPath, BigDecimal fee) {
        if (booking == null) {
            throw new RuleException("Choose the booking the damage happened during.");
        }
        if (booking.getOwnerId() != ownerId) {
            throw new RuleException("That booking is on another owner's property.");
        }
        if (!isReportable(booking, LocalDateTime.now())) {
            throw new RuleException("Damage can only be reported on a stay that has started or finished.");
        }
        if (description == null || description.isBlank()) {
            throw new RuleException("Describe the damage.");
        }
        if (description.trim().length() > MAX_DESCRIPTION) {
            throw new RuleException("Keep the description to " + MAX_DESCRIPTION + " characters or fewer.");
        }
        if (fee == null || fee.signum() <= 0) {
            throw new RuleException("Enter the fee to charge (above ₱0.00).");
        }
        ops.insertDamageReport(booking.getId(), description.trim(), photoPath, fee, LocalDateTime.now().withNano(0));
    }

    /** The owner drops an unpaid report; if it was overdue, the renter can book again. */
    public void dismiss(int ownerId, int reportId) {
        DamageReport r = byId(reportId, forOwner(ownerId));
        requireOpen(r);
        ops.dismissDamageReport(r.id(), LocalDateTime.now().withNano(0));
    }

    // ---------------------------------------------------------------------- renter

    /**
     * The renter pays: one transaction adds the paid damage-fee payment, marks the report paid and
     * records the income on the owner's property. Paying late is allowed (it lifts the suspension).
     */
    public DamageReport pay(int renterId, int reportId) {
        DamageReport r = byId(reportId, forRenter(renterId));
        requireOpen(r);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Db.transaction(() -> {
            int paymentId = money.insertPaidPayment(r.booking().getId(), r.requestedFee(), "damage_fee",
                    ReceiptPaths.damageFee(r.id()), now);
            ops.chargeDamageReport(r.id(), paymentId, now);
            money.insertTransaction(r.booking().getPropertyId(), paymentId, "damage_fee_income", r.requestedFee(), now,
                    "Damage report #" + r.id());
            return null;
        });
        return byId(reportId, forRenter(renterId));
    }

    private static void requireOpen(DamageReport r) {
        if (!r.isOpen()) {
            throw new RuleException(r.isPaid() ? "This report has already been paid." : "This report was dismissed.");
        }
    }
}
