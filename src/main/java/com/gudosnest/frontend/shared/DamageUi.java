package com.gudosnest.frontend.shared;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.ImageUtil;
import com.gudosnest.frontend.components.Receipt;
import com.gudosnest.frontend.components.ReceiptDialog;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.Component;
import java.time.LocalDateTime;

/** Damage report pieces shown the same way in both portals: status pill, details, receipt. */
public final class DamageUi {

    /** Width of the damage dialogs, and the width wrapped text keeps inside them (less insets and a scrollbar). */
    public static final int DIALOG_WIDTH = 560;
    public static final int TEXT_WIDTH = DIALOG_WIDTH - 48 - 16;

    private DamageUi() {
    }

    /** Awaiting payment = Primary tint, Overdue = amber, Paid = green, Dismissed = grey. */
    public static Badge.Tone tone(DamageReport r) {
        if (r.isOverdue(LocalDateTime.now())) {
            return Badge.Tone.WARNING;
        }
        return switch (r.status()) {
            case "open" -> Badge.Tone.NEUTRAL;
            case "charged" -> Badge.Tone.SUCCESS;
            default -> Badge.Tone.MUTED;
        };
    }

    public static AppTable.Pill pill(DamageReport r) {
        return new AppTable.Pill(r.statusLabel(), tone(r));
    }

    /** "Pay by Nov 17 · 9:57 PM" for an unpaid report, otherwise empty. */
    public static String payBy(DamageReport r) {
        return r.isOpen() ? Formats.dateTime(r.dueAt()) : "";
    }

    /** The description on one line (a table cell cuts it with "…" when it is too long). */
    public static String oneLine(String text) {
        return text.replaceAll("\s+", " ").trim();
    }

    /**
     * Place and status, who it concerns (the renter for the owner, the host for the renter), the
     * photo (if any), stay / dates / fee, then the description.
     */
    public static void details(JPanel body, DamageReport r, boolean forOwner) {
        Booking b = r.booking();
        JPanel titleRow = Ui.transparent("insets 0, fillx, gap 12", "[grow][]");
        titleRow.add(Ui.label(BookingUi.place(b), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        titleRow.add(new Badge(r.statusLabel(), tone(r)), "aligny center");
        body.add(titleRow, "wmin 0");
        String who = forOwner ? b.getRenterName() : "Host " + b.getOwnerName();
        body.add(Ui.muted(who + " · " + Formats.ref(b.getId(), b.getCreatedAt())), "wmin 0, gapbottom 8");

        if (r.photoPath() != null && ImageUtil.load(r.photoPath()) != null) {
            body.add(new Widgets.Photo(r.photoPath(), 12, true), "h 200!, wmin 0, gapbottom 8");
        }

        Card summary = new Card(new MigLayout("insets 16, gap 0 8, fillx, wrap 1", "[grow,fill]"));
        summary.add(Ui.row("Stay", BookingUi.dates(b) + " · " + Formats.nights(b.getNights())));
        summary.add(Ui.row("Reported", Formats.dateTime(r.createdAt())));
        if (r.isOpen()) {
            summary.add(Ui.row(r.isOverdue(LocalDateTime.now()) ? "Was due" : "Pay by", Formats.dateTime(r.dueAt())));
        } else if (r.resolvedAt() != null) {
            summary.add(Ui.row(r.isPaid() ? "Paid" : "Dismissed", Formats.dateTime(r.resolvedAt())));
        }
        summary.add(Ui.divider(), "gaptop 4");
        JPanel total = Ui.transparent("insets 0, fillx", "[grow][right]");
        total.add(Ui.label(switch (r.status()) {
            case "charged" -> "Fee paid";
            case "dismissed" -> "Fee (not charged)";
            default -> "Fee to pay";
        }, Fonts.bodyBold(), Colors.TEXT));
        total.add(Ui.label(Formats.money(r.requestedFee()), Fonts.sectionTitle(), Colors.PRIMARY));
        summary.add(total);
        body.add(summary, "gapbottom 8");

        body.add(Ui.label("What was damaged", Fonts.captionMedium(), Colors.TEXT_MUTED));
        body.add(Widgets.wrap(r.description(), Fonts.body(), Colors.TEXT, TEXT_WIDTH), "wmin 0");
    }

    /**
     * Amber banner for a suspended renter: booking is paused until the overdue reports are paid.
     * {@code onView} (may be null) adds a "View damage reports" link.
     */
    public static Card suspendedBanner(int overdueCount, Runnable onView) {
        Card box = new Card(new MigLayout("insets 16 20 16 20, gap 12, fillx", "[][grow,fill][]"));
        box.setBackground(Colors.WARNING_TINT);
        box.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        box.add(new javax.swing.JLabel(com.gudosnest.frontend.theme.Icons.get("triangle-alert", 20, Colors.WARNING)), "aligny top");
        JPanel text = Ui.transparent("insets 0, gap 0, wrap 1", "[grow,fill]");
        text.add(Ui.label("Booking is paused", Fonts.bodyBold(), Colors.WARNING));
        text.add(Ui.label((overdueCount == 1 ? "A damage report is" : overdueCount + " damage reports are")
                + " unpaid after " + DamageReport.PAY_WITHIN_HOURS + " hours. Pay to book again.",
                Fonts.body(), Colors.TEXT), "wmin 0");
        box.add(text, "wmin 0");
        if (onView != null) {
            box.add(Widgets.link("View damage reports", true, onView), "aligny center");
        }
        return box;
    }

    /** The damage fee receipt (as the next page when opened from a dialog). */
    public static void showReceipt(Component parent, DamageReport r) {
        LocalDateTime paidAt = r.resolvedAt() == null ? LocalDateTime.now() : r.resolvedAt();
        ReceiptDialog.show(parent, Receipt.forDamageFee(r, paidAt), "damage-fee-" + r.id() + ".png");
    }

    /** Saves the receipt image next to the others once a fee is paid. */
    public static void saveReceipt(DamageReport r) {
        LocalDateTime paidAt = r.resolvedAt() == null ? LocalDateTime.now() : r.resolvedAt();
        Receipt.forDamageFee(r, paidAt).saveQuietly(com.gudosnest.backend.service.ReceiptPaths.damageFee(r.id()));
    }
}
