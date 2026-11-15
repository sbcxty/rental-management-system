package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.RefundRow;
import com.gudosnest.backend.service.refund.RefundPolicy;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Receipt;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** The owner reviews a renter's refund request: approve (paid out now) or reject. */
public final class RefundReviewDialog {

    private RefundReviewDialog() {
    }

    public static boolean show(Component parent, Portal portal, RefundRow refund) {
        boolean[] changed = {false};
        AppDialog dialog = new AppDialog(parent, "Refund request");
        fill(dialog, dialog.getBody(), parent, portal, refund, changed, false);
        dialog.showDialog(560);
        if (changed[0]) {
            portal.refreshBadges();
        }
        return changed[0];
    }

    /**
     * Shows the refund review as the next page of an open dialog (the booking dialog's "Refund"
     * button), with "Go back" to return. Sets changed[0] when the refund is approved or rejected.
     */
    static void showAsPage(AppDialog dialog, Component parent, Portal portal, RefundRow refund, boolean[] changed) {
        fill(dialog, dialog.nextPage("Refund request"), parent, portal, refund, changed, true);
        dialog.refit();
    }

    /** The summary (with the cancellation fee taken off) and the approve / reject buttons. */
    private static void fill(AppDialog dialog, JPanel body, Component parent, Portal portal, RefundRow refund,
                             boolean[] changed, boolean asPage) {
        Booking b = refund.booking();

        body.add(Ui.label(b.getPropertyName() + " · " + b.getUnitLabel(), Fonts.cardTitle(), Colors.TEXT));
        body.add(Ui.muted(b.getRenterName() + " · " + Formats.ref(b.getId(), b.getCreatedAt())
                + " · cancelled " + Formats.date(b.getCancelledAt())), "gapbottom 8");

        long daysBefore = ChronoUnit.DAYS.between(b.getCancelledAt().toLocalDate(), b.getCheckIn().toLocalDate());
        Card summary = new Card(new MigLayout("insets 16, gap 0 10, fillx, wrap 1", "[grow,fill]"));
        summary.add(Ui.row("Original booking total", Formats.money(b.getTotal())));
        summary.add(Ui.row("Stay dates", Formats.dateRange(b.getCheckIn().toLocalDate(), b.getCheckOut().toLocalDate())));
        summary.add(Ui.row("Cancelled before check-in", daysBefore + (daysBefore == 1 ? " day" : " days")));
        summary.add(Ui.row("Policy tier", RefundPolicy.FULL.equals(refund.tier()) ? "Full refund" : "Partial refund"));
        if (refund.cancellationFee().signum() > 0) {
            summary.add(Ui.row("Cancellation fee (20%)", "-" + Formats.money(refund.cancellationFee())));
        }
        summary.add(Ui.divider());
        JPanel total = Ui.transparent("insets 0, fillx", "[grow][right]");
        total.add(Ui.label("Refund to pay", Fonts.bodyBold(), Colors.TEXT));
        total.add(Ui.label(Formats.money(refund.amount()), Fonts.sectionTitle(), Colors.PRIMARY));
        summary.add(total);
        body.add(summary);
        if (b.getCancellationReason() != null) {
            body.add(Ui.label("Renter's reason", Fonts.captionMedium(), Colors.TEXT_MUTED), "gaptop 4");
            body.add(Widgets.wrap(b.getCancellationReason(), Fonts.body(), Colors.TEXT), "wmin 0");
        }
        body.add(Widgets.policyBox(), "gaptop 8");

        int ownerId = portal.user().getId();
        AppButton reject = new AppButton("Reject refund", AppButton.Style.DANGER);
        reject.addActionListener(e -> dialog.confirmPage("Reject this refund?",
                b.getRenterName() + " will not get " + Formats.money(refund.amount()) + " back.",
                "Reject refund", AppButton.Style.DANGER, () -> {
                    if (BookingReviewDialog.run(dialog, () -> portal.owner.rejectRefund(refund.refundId(), ownerId))) {
                        changed[0] = true;
                        dialog.dispose();
                    } else {
                        dialog.previousPage();
                    }
                }));
        AppButton approve = new AppButton("Approve refund", AppButton.Style.PRIMARY);
        approve.addActionListener(e -> {
            if (BookingReviewDialog.run(dialog, () -> portal.owner.approveRefund(refund.refundId(), ownerId))) {
                changed[0] = true;
                dialog.dispose();
                RefundRow done = portal.owner.refund(refund.refundId());
                LocalDateTime at = done.processedAt() == null ? LocalDateTime.now() : done.processedAt();
                Receipt.forRefund(done.booking(), at).saveQuietly(
                        com.gudosnest.backend.service.ReceiptPaths.refund(refund.paymentId()));
                AppDialog.info(parent, "Refund paid", "Refund of " + Formats.money(refund.amount()) + " paid to " + b.getRenterName() + ".");
            }
        });
        if (asPage) {
            dialog.addBackButton("Go back");
        } else {
            AppButton message = new AppButton("Message renter", AppButton.Style.OUTLINED).withIcon("message-square");
            message.addActionListener(e -> {
                dialog.dispose();
                portal.openConversation(b.getRenterId(), b.getRenterName(), b);
            });
            dialog.addLeftButton(message);
        }
        dialog.addButton(reject);
        dialog.addButton(approve);
    }
}
