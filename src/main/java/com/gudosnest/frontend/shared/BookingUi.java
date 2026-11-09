package com.gudosnest.frontend.shared;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.service.BookingService;
import com.gudosnest.backend.service.pricing.Price;
import com.gudosnest.backend.service.refund.RefundPolicy;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Receipt;
import com.gudosnest.frontend.components.ReceiptDialog;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/** Booking pieces shown the same way in both portals: status pill, details, payment summary. */
public final class BookingUi {

    private BookingUi() {
    }

    public static Badge.Tone tone(Booking.Stage stage) {
        return switch (stage) {
            case PENDING -> Badge.Tone.NEUTRAL;
            case UPCOMING -> Badge.Tone.INFO;
            case STAYING, CHECKOUT_DUE -> Badge.Tone.STRONG;
            default -> Badge.Tone.MUTED;
        };
    }

    public static Badge badge(Booking b) {
        Booking.Stage stage = b.getStage();
        return new Badge(stage.label(), tone(stage));
    }

    public static AppTable.Pill pill(Booking b) {
        Booking.Stage stage = b.getStage();
        return new AppTable.Pill(stage.label(), tone(stage));
    }

    /** "Nov 18 – Nov 22, 2026" */
    public static String dates(Booking b) {
        return Formats.dateRange(b.getCheckIn().toLocalDate(), b.getCheckOut().toLocalDate());
    }

    /** "The Stone Courtyard Villa · Courtyard Suite A" or "Highland Glass Cabin · Whole house" */
    public static String place(Booking b) {
        return b.getPropertyName() + " · " + b.getUnitLabel();
    }

    /** Time left for the owner to respond to a pending booking: "Respond within 31h". */
    public static String respondWithin(Booking b) {
        long hours = Math.max(0, Duration.between(LocalDateTime.now(), b.getExpiresAt()).toHours());
        return hours < 1 ? "Respond within the hour" : "Respond within " + hours + "h";
    }

    /** Check-in / Check-out / Duration / Guests in a tinted strip. */
    public static JComponent stayStrip(Booking b) {
        Card strip = Widgets.tintBox(new MigLayout("insets 14 16 14 16, gap 24, fillx",
                "[grow,fill][grow,fill][grow,fill][grow,fill]"));
        strip.add(stat("Check-in", Formats.time(b.getCheckIn()) + ", " + Formats.date(b.getCheckIn())));
        strip.add(stat("Check-out", Formats.time(b.getCheckOut()) + ", " + Formats.date(b.getCheckOut())));
        strip.add(stat("Duration", Formats.nights(b.getNights())));
        strip.add(stat("Guests", Formats.guests(b.getGuests())));
        return strip;
    }

    private static JComponent stat(String label, String value) {
        JPanel p = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        p.add(Ui.caption(label));
        p.add(Ui.label(value, Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
        return p;
    }

    /** Itemized payment: nights × rate, cleaning fee, discount, total paid. */
    public static JComponent paymentSummary(Booking b) {
        Card box = new Card(new MigLayout("insets 16, gap 0 8, fillx, wrap 1", "[grow,fill]"));
        for (Price.Line line : Price.forStay(b.getNightlyRate(), b.getNights(), b.getCleaningFee()).lines()) {
            String amount = (line.discount() ? "-" : "") + Formats.money(line.amount());
            box.add(Ui.row(line.label(), amount));
        }
        box.add(Ui.divider(), "gaptop 4");
        JPanel total = Ui.transparent("insets 0, fillx", "[grow][right]");
        total.add(Ui.label("Total paid", Fonts.bodyBold(), Colors.TEXT));
        total.add(Ui.label(Formats.money(b.getTotal()), Fonts.sectionTitle(), Colors.TEXT));
        box.add(total);
        return box;
    }

    /** A line about the refund on a cancelled/rejected/expired booking, or null if none. */
    public static String refundLine(Booking b) {
        if (!b.hasRefund()) {
            return b.getStage() == Booking.Stage.CANCELLED ? "No refund (cancelled within 2 days of check-in)." : null;
        }
        String amount = Formats.money(b.getRefundAmount());
        return switch (b.getRefundStatus()) {
            case "processed" -> "Refunded " + amount + (RefundPolicy.AUTOMATIC.equals(b.getRefundTier())
                    ? " automatically." : " after review.");
            case "requested" -> "Refund of " + amount + " requested; waiting for the owner's review.";
            default -> "Refund of " + amount + " was declined by the owner.";
        };
    }

    /** One sentence about where the booking stands, for the banner at the top of booking dialogs. */
    public static String statusSentence(Booking b, boolean forOwner) {
        return switch (b.getStage()) {
            case PENDING -> forOwner
                    ? "Approve or reject by " + Formats.dateTime(b.getExpiresAt())
                    + ". After that it expires and the renter is refunded in full."
                    : "The owner has until " + Formats.dateTime(b.getExpiresAt())
                    + " to approve. If it isn't approved, you're refunded in full.";
            case UPCOMING -> "Approved. Check-in is " + Formats.dateTime(b.getCheckIn()) + ".";
            case STAYING -> "Checked in. Check-out is " + Formats.dateTime(b.getCheckOut()) + ".";
            case CHECKOUT_DUE -> forOwner
                    ? "Check-out time has passed. Mark the stay as completed once the guest has left."
                    : "Check-out time has passed. Confirm your check-out below.";
            case COMPLETED -> "Completed on " + Formats.date(b.getCheckoutConfirmedAt()) + ".";
            case CANCELLED -> "Cancelled by the " + b.getCancelledBy() + " on " + Formats.date(b.getCancelledAt()) + ".";
            case REJECTED -> "The owner didn't approve this booking.";
            case EXPIRED -> "The owner didn't respond within " + BookingService.APPROVAL_HOURS + " hours.";
        };
    }

    /** Banner: tinted box with the status sentence and (if any) the refund line. */
    public static JComponent banner(Booking b, boolean forOwner) {
        Card box = Widgets.tintBox(new MigLayout("insets 14 16 14 16, gap 0 4, fillx, wrap 1", "[grow,fill]"));
        box.add(Ui.label(b.getStage().label(), Fonts.bodyBold(), Colors.TEXT));
        box.add(Widgets.wrap(statusSentence(b, forOwner), Fonts.caption(), Colors.TEXT), "wmin 0");
        String refund = refundLine(b);
        if (refund != null) {
            box.add(Widgets.wrap(refund, Fonts.caption(), Colors.TEXT), "wmin 0");
        }
        if (b.getCancellationReason() != null && !b.getCancellationReason().isBlank()) {
            box.add(Widgets.wrap("Reason: " + b.getCancellationReason(), Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        }
        return box;
    }

    /** Opens the payment receipt for a booking. */
    public static void showPaymentReceipt(Component parent, Booking b) {
        ReceiptDialog.show(parent, Receipt.forPayment(b), Formats.refCode(b.getId(), b.getCreatedAt()) + ".png");
    }

    /** Opens the refund receipt (only for processed refunds). */
    public static void showRefundReceipt(Component parent, Booking b, LocalDateTime processedAt) {
        ReceiptDialog.show(parent, Receipt.forRefund(b, processedAt),
                "refund-" + Formats.refCode(b.getId(), b.getCreatedAt()) + ".png");
    }

    /** Label that shows money paid or refunded on list rows. */
    public static JLabel amountLabel(BigDecimal amount) {
        return Ui.label(Formats.money(amount), Fonts.bodyBold(), Colors.TEXT);
    }
}
