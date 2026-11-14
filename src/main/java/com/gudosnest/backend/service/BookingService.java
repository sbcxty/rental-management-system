package com.gudosnest.backend.service;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.BookingStatus;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.repository.BookingRepository;
import com.gudosnest.backend.repository.MoneyRepository;
import com.gudosnest.backend.service.pricing.Price;
import com.gudosnest.backend.service.refund.RefundPolicy;
import com.gudosnest.backend.structures.AvailabilityCalendar;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// PATTERN: Facade
// Booking, cancelling, approving and rejecting each touch several tables: Bookings, Payments,
// Refunds, FinancialTransactions and Units. The screens call one method here, for example
// BookingService.book(...) or BookingService.cancel(...), and this class runs every step in one
// database transaction and applies the rules (availability, 48-hour approval, refund tiers).
// The UI never has to know which tables change or in what order.
public class BookingService {

    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);
    public static final LocalTime CHECK_OUT_TIME = LocalTime.of(11, 0);
    public static final int APPROVAL_HOURS = 48;

    private final BookingRepository bookings = new BookingRepository();
    private final MoneyRepository money = new MoneyRepository();
    private final Clock clock;

    public BookingService() {
        this(Clock.systemDefaultZone());
    }

    /** Tests can pass a fixed clock. */
    public BookingService(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    // ------------------------------------------------------------------------------- reads

    public List<Booking> forOwner(int ownerId) {
        return bookings.forOwner(ownerId);
    }

    public List<Booking> forRenter(int renterId) {
        return bookings.forRenter(renterId);
    }

    public Booking byId(int bookingId) {
        return bookings.byId(bookingId).orElseThrow(() -> new RuleException("That booking no longer exists."));
    }

    /** The unit's approved stays, as a TreeMap-backed calendar for fast overlap checks. */
    public AvailabilityCalendar<Integer> calendarFor(int unitId) {
        AvailabilityCalendar<Integer> calendar = new AvailabilityCalendar<>();
        for (Booking b : bookings.approvedForUnit(unitId)) {
            calendar.add(b.getCheckIn(), b.getCheckOut(), b.getId());
        }
        return calendar;
    }

    public boolean isAvailable(int unitId, LocalDate checkIn, LocalDate checkOut) {
        return calendarFor(unitId).isFree(checkIn.atTime(CHECK_IN_TIME), checkOut.atTime(CHECK_OUT_TIME));
    }

    // ---------------------------------------------------------------------- housekeeping

    /**
     * Brings statuses up to date. Run when the app opens, whenever a page opens, and every few
     * minutes from a timer (see Portal):
     *  - pending bookings past their 48 hours (or past check-in) expire, with an automatic full refund;
     *  - approved stays whose check-out time has passed are completed automatically;
     *  - units with a guest checked in become occupied, others unoccupied.
     * Safe to run any number of times. Returns true if anything changed.
     */
    public boolean refreshStatuses() {
        LocalDateTime now = now();
        List<Integer> expired = bookings.expiredPendingIds(now);
        for (int id : expired) {
            Db.transaction(() -> {
                Booking b = byId(id);
                bookings.setStatus(id, BookingStatus.EXPIRED);
                automaticRefund(b, now);
                return null;
            });
        }
        // Completing ended stays and freeing their units are saved together, or not at all
        int changed = Db.transaction(() -> bookings.completeEnded(now) + bookings.refreshOccupancy(now));
        return !expired.isEmpty() || changed > 0;
    }

    // ------------------------------------------------------------------------- renter

    /** Checks the request and returns its price, without saving anything. */
    public Price quote(Unit unit, LocalDate checkIn, LocalDate checkOut, int guests) {
        validateStay(unit, checkIn, checkOut, guests);
        return Price.forStay(unit.getDailyRate(), checkOut.toEpochDay() - checkIn.toEpochDay(), unit.getCleaningFee());
    }

    /**
     * Pay first: saves the booking (pending, owner has 48 hours) and its paid payment together.
     * Returns the new booking.
     */
    public Booking book(int renterId, Unit unit, LocalDate checkIn, LocalDate checkOut, int guests, String requests) {
        Price price = quote(unit, checkIn, checkOut, guests);
        LocalDateTime in = checkIn.atTime(CHECK_IN_TIME);
        LocalDateTime out = checkOut.atTime(CHECK_OUT_TIME);
        if (!calendarFor(unit.getId()).isFree(in, out)) {
            throw new RuleException("Those dates are already booked. Choose other dates.");
        }
        boolean alreadyRequested = bookings.forRenter(renterId).stream()
                .anyMatch(b -> b.getUnitId() == unit.getId() && b.getStatus() == BookingStatus.PENDING
                        && b.getCheckIn().isBefore(out) && b.getCheckOut().isAfter(in));
        if (alreadyRequested) {
            throw new RuleException("You already have a pending booking for these dates.");
        }
        LocalDateTime now = now();
        String cleanRequests = requests == null || requests.isBlank() ? null : requests.trim();
        int id = Db.transaction(() -> {
            int bookingId = bookings.insert(renterId, unit.getId(), now, now.plusHours(APPROVAL_HOURS), in, out,
                    guests, cleanRequests, unit.getDailyRate(), unit.getCleaningFee(), price.discount(), price.total());
            money.insertPaidPayment(bookingId, price.total(), "rental_payment",
                    ReceiptPaths.payment(bookingId, now), now);
            return bookingId;
        });
        return byId(id);
    }

    /**
     * Renter cancels. Pending: cancelled at once, full automatic refund. Approved and before
     * check-in: cancelled at once; the refund tier comes from RefundPolicy and, if it refunds
     * anything, waits for the owner's review. Returns the quote that was applied.
     */
    public RefundPolicy.Quote cancelByRenter(int bookingId, int renterId, String reason) {
        Booking b = byId(bookingId);
        if (b.getRenterId() != renterId) {
            throw new RuleException("You can only cancel your own bookings.");
        }
        LocalDateTime now = now();
        RefundPolicy.Quote quote = renterCancellationQuote(b, now);
        Db.transaction(() -> {
            bookings.cancel(bookingId, "renter", reason, now);
            if (b.getStatus() == BookingStatus.PENDING) {
                automaticRefund(b, now);
            } else if (quote.refundsAnything()) {
                money.insertRefund(b.getPaymentId(), quote.tier(), quote.fee(), quote.refund(), "requested",
                        now, null, null);
            }
            return null;
        });
        return quote;
    }

    /** What a renter would get back if they cancelled now (also checks that they may cancel). */
    public RefundPolicy.Quote renterCancellationQuote(Booking b, LocalDateTime now) {
        if (b.getStatus() == BookingStatus.PENDING) {
            return new RefundPolicy.AutomaticRefund().quote(b.getTotal());
        }
        if (b.getStatus() != BookingStatus.APPROVED) {
            throw new RuleException("Only pending or upcoming bookings can be cancelled.");
        }
        if (!now.isBefore(b.getCheckIn())) {
            throw new RuleException("This stay has already started, so it can't be cancelled.");
        }
        return RefundPolicy.forCancellation(now.toLocalDate(), b.getCheckIn().toLocalDate()).quote(b.getTotal());
    }

    public RefundPolicy.Quote renterCancellationQuote(Booking b) {
        return renterCancellationQuote(b, now());
    }

    /** Renter confirms they have checked out: the stay is completed and the unit is free again. */
    public void confirmCheckout(int bookingId, int renterId) {
        Booking b = byId(bookingId);
        if (b.getRenterId() != renterId) {
            throw new RuleException("You can only check out of your own stays.");
        }
        complete(b);
    }

    // -------------------------------------------------------------------------- owner

    /** Approves a pending booking, records the income, and rejects pending bookings that overlap it. */
    public List<Booking> approve(int bookingId, int ownerId) {
        Booking b = ownedPending(bookingId, ownerId);
        LocalDateTime now = now();
        if (!calendarFor(b.getUnitId()).isFree(b.getCheckIn(), b.getCheckOut())) {
            throw new RuleException("Another approved booking already covers these dates. Reject this one instead.");
        }
        List<Booking> overlapping = bookings.pendingOverlapping(b.getUnitId(), b.getCheckIn(), b.getCheckOut(), bookingId);
        Db.transaction(() -> {
            bookings.setStatus(bookingId, BookingStatus.APPROVED);
            money.insertTransaction(b.getPropertyId(), b.getPaymentId(), "rental_income", b.getTotal(), now, null);
            for (Booking other : overlapping) {
                bookings.setStatus(other.getId(), BookingStatus.REJECTED);
                automaticRefund(other, now);
            }
            return null;
        });
        bookings.refreshOccupancy(now);
        return overlapping;
    }

    /** Rejects a pending booking; the renter gets everything back automatically. */
    public void reject(int bookingId, int ownerId) {
        Booking b = ownedPending(bookingId, ownerId);
        LocalDateTime now = now();
        Db.transaction(() -> {
            bookings.setStatus(bookingId, BookingStatus.REJECTED);
            automaticRefund(b, now);
            return null;
        });
    }

    /** Owner cancels an approved booking before check-in: full automatic refund, income reversed. */
    public void cancelByOwner(int bookingId, int ownerId, String reason) {
        Booking b = byId(bookingId);
        if (b.getOwnerId() != ownerId) {
            throw new RuleException("You can only cancel bookings on your own properties.");
        }
        LocalDateTime now = now();
        if (b.getStatus() != BookingStatus.APPROVED || !now.isBefore(b.getCheckIn())) {
            throw new RuleException("Only upcoming bookings can be cancelled.");
        }
        Db.transaction(() -> {
            bookings.cancel(bookingId, "owner", reason, now);
            automaticRefund(b, now);
            return null;
        });
    }

    /** Owner marks a stay as completed (the guest has left). */
    public void markCompleted(int bookingId, int ownerId) {
        Booking b = byId(bookingId);
        if (b.getOwnerId() != ownerId) {
            throw new RuleException("You can only complete bookings on your own properties.");
        }
        complete(b);
    }

    // ------------------------------------------------------------------------- helpers

    private void complete(Booking b) {
        LocalDateTime now = now();
        if (b.getStatus() != BookingStatus.APPROVED || now.isBefore(b.getCheckIn())) {
            throw new RuleException("Only a stay that has started can be completed.");
        }
        bookings.complete(b.getId(), now);
        bookings.refreshOccupancy(now);
    }

    private Booking ownedPending(int bookingId, int ownerId) {
        Booking b = byId(bookingId);
        if (b.getOwnerId() != ownerId) {
            throw new RuleException("You can only review bookings on your own properties.");
        }
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new RuleException("This booking is no longer pending.");
        }
        if (!now().isBefore(b.getExpiresAt())) {
            throw new RuleException("The 48 hours to respond have passed, so this booking has expired and was refunded.");
        }
        return b;
    }

    /**
     * Full refund with no review, processed now. If the booking had already been counted as income
     * (it was approved), a refund line is added to the ledger so income stays correct.
     * Must run inside a transaction.
     */
    private void automaticRefund(Booking b, LocalDateTime now) {
        RefundPolicy.Quote quote = new RefundPolicy.AutomaticRefund().quote(b.getTotal());
        money.insertRefund(b.getPaymentId(), quote.tier(), quote.fee(), quote.refund(), "processed",
                now, now, ReceiptPaths.refund(b.getPaymentId()));
        if (b.getStatus() == BookingStatus.APPROVED) {
            money.insertTransaction(b.getPropertyId(), b.getPaymentId(), "refund", quote.refund(), now, null);
        }
    }

    private void validateStay(Unit unit, LocalDate checkIn, LocalDate checkOut, int guests) {
        LocalDate today = LocalDate.now(clock);
        if (checkIn == null || checkOut == null) {
            throw new RuleException("Choose check-in and check-out dates.");
        }
        if (!checkIn.isAfter(today)) {
            throw new RuleException("Check-in must be tomorrow or later.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new RuleException("Check-out must be at least one night after check-in.");
        }
        if (checkOut.toEpochDay() - checkIn.toEpochDay() > 30) {
            throw new RuleException("Stays can be up to 30 nights.");
        }
        if (guests < 1 || guests > unit.getCapacity()) {
            throw new RuleException(unit.getName() + " fits up to " + unit.getCapacity()
                    + (unit.getCapacity() == 1 ? " guest." : " guests."));
        }
    }
}
