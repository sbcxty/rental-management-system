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

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Function;

/** Booking pieces shown the same way in both portals: status pill, details, payment summary. */
public final class BookingUi {

    private BookingUi() {
    }

    public static Badge.Tone tone(Booking.Stage stage) {
        return switch (stage) {
            case PENDING -> Badge.Tone.PENDING;
            case UPCOMING -> Badge.Tone.INFO;
            case STAYING -> Badge.Tone.STRONG;
            case COMPLETED -> Badge.Tone.SUCCESS;
            case CANCELLED, REJECTED -> Badge.Tone.WARNING;
            case EXPIRED -> Badge.Tone.MUTED;
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

    /** Reference text in one form for comparing: upper case, without "Ref", "#" or spaces. */
    private static String refKey(String text) {
        return text.toUpperCase().replace("REF", "").replace("#", "").replaceAll("\\s+", "");
    }

    /**
     * True when a search looks like all or part of a booking reference: it starts with "GN",
     * "Ref" or "#", or is only digits and dashes ("8815", "GN-2026-88"). Such a search covers every
     * tab, since the renter or owner is looking for one booking.
     */
    public static boolean looksLikeReference(String query) {
        String trimmed = query.trim().toUpperCase();
        String key = refKey(query);
        return !key.isEmpty() && (trimmed.startsWith("GN") || trimmed.startsWith("REF") || trimmed.startsWith("#")
                || key.chars().allMatch(c -> Character.isDigit(c) || c == '-'));
    }

    /** True when the booking's reference contains the typed text, so partial references match as you type. */
    public static boolean referenceContains(Booking b, String query) {
        return refKey(Formats.refCode(b.getId(), b.getCreatedAt())).contains(refKey(query));
    }

    /** Check-in / Check-out / Duration / Guests in a tinted strip. */
    public static JComponent stayStrip(Booking b) {
        Card strip = Widgets.tintBox(new MigLayout("insets 16, gap 16, fillx",
                "[grow,fill][grow,fill][][]"));   // the two dates share the spare width
        strip.add(stat("Check-in", Formats.time(b.getCheckIn()) + ", " + Formats.date(b.getCheckIn())));
        strip.add(stat("Check-out", Formats.time(b.getCheckOut()) + ", " + Formats.date(b.getCheckOut())));
        strip.add(stat("Duration", Formats.nights(b.getNights())));
        strip.add(stat("Guests", Formats.guests(b.getGuests())));
        return strip;
    }

    private static JComponent stat(String label, String value) {
        JPanel p = Ui.transparent("insets 0, gap 0, wrap 1, fillx", "[grow,fill]");
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

    /**
     * Draws each stay in a booking dropdown on two lines (property and unit; then dates, reference
     * and the total, or the renter's name for the owner), never wider than the field: long text is
     * cut with "…", so the open list lines up with the field instead of spilling over the page. The
     * closed field shows one line, centred vertically in the field.
     */
    public static <T> ListCellRenderer<T> stayRenderer(JComboBox<T> combo, Function<T, Booking> booking,
                                                       boolean forOwner) {
        return twoLineRenderer(combo, value -> {
            Booking b = booking.apply(value);
            String dates = dates(b);
            String ref = Formats.ref(b.getId(), b.getCreatedAt());
            return new String[]{place(b), place(b) + " · " + dates, forOwner
                    ? b.getRenterName() + " · " + dates + " · " + ref
                    : dates + " · " + ref + " · " + Formats.money(b.getTotal())};
        });
    }

    /**
     * The same two-line dropdown with the renter first (the property is already chosen):
     * "Miguel Alvarez", then "Room 1 · Nov 15 – Nov 19, 2026". Closed, it shows
     * "Miguel Alvarez · Room 1".
     */
    public static ListCellRenderer<Booking> renterRenderer(JComboBox<Booking> combo) {
        return twoLineRenderer(combo, b -> new String[]{b.getRenterName(),
                b.getRenterName() + " · " + b.getUnitLabel(),
                b.getUnitLabel() + " · " + dates(b)});
    }

    /** {@code texts} gives {main line in the list, the one line in the closed field, second line}. */
    private static <T> ListCellRenderer<T> twoLineRenderer(JComboBox<T> combo, Function<T, String[]> texts) {
        MigLayout layout = new MigLayout("insets 6 12 6 12, gap 0, wrap 1", "[grow,fill]");
        JPanel cell = new JPanel(layout);
        JLabel name = new JLabel();
        JLabel details = new JLabel();
        name.setFont(Fonts.bodyMedium());
        details.setFont(Fonts.caption());
        cell.add(name, "wmin 0");
        cell.add(details, "wmin 0, hidemode 3");   // hidden in the closed field, so it takes no space
        return (list, value, index, selected, focused) -> {
            boolean closed = index < 0;
            if (value == null) {
                name.setText(" ");
                details.setText(" ");
            } else {
                String[] t = texts.apply(value);
                name.setText(closed ? t[1] : t[0]);
                details.setText(t[2]);
            }
            details.setVisible(!closed);
            layout.setLayoutConstraints(closed ? "insets 0 4 0 4, gap 0, wrap 1, aligny center"
                    : "insets 6 12 6 12, gap 0, wrap 1");
            boolean highlight = selected && !closed;
            cell.setOpaque(!closed);
            cell.setBackground(highlight ? list.getSelectionBackground() : list.getBackground());
            name.setForeground(highlight ? list.getSelectionForeground() : Colors.TEXT);
            details.setForeground(highlight ? list.getSelectionForeground() : Colors.TEXT_MUTED);
            cell.setPreferredSize(null);
            Dimension size = cell.getPreferredSize();
            if (combo.getWidth() > 0) {
                size.width = Math.min(size.width, combo.getWidth());
            }
            cell.setPreferredSize(size);
            return cell;
        };
    }
}
