package com.gudosnest.frontend.renter;

import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JPanel;
import java.awt.Component;
import java.time.LocalDateTime;

/**
 * The renter's look at one booking (from Booking history and Messages): status and refund,
 * dates, payment, receipts, and what the renter may do at this stage. Before check-in,
 * "Cancel booking" opens the Cancellations page, which shows the refund that applies and asks
 * for a reason, so the refund rules live in one place. After check-in time the renter confirms
 * check-out here. Returns true from {@link #show} if the booking changed, so the page can reload.
 */
public final class RenterBookingDialog {

    private RenterBookingDialog() {
    }

    /** @param withMessage false when opened from the conversation itself (no "Message host") */
    public static boolean show(Component parent, Portal portal, Booking b, boolean withMessage) {
        boolean[] changed = {false};
        int renterId = portal.user().getId();
        AppDialog dialog = new AppDialog(parent, "Booking " + Formats.refCode(b.getId(), b.getCreatedAt()));
        JPanel body = dialog.getBody();

        JPanel head = Ui.transparent("insets 0, gap 8, fillx", "[grow][]");
        JPanel names = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        names.add(Ui.label(b.getPropertyName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        names.add(Ui.muted(b.getUnitLabel() + " · " + b.getLocation()), "wmin 0");
        names.add(Ui.caption("Hosted by " + b.getOwnerName() + " · booked " + Formats.dateTime(b.getCreatedAt())),
                "wmin 0");
        head.add(names, "wmin 0");
        head.add(BookingUi.badge(b), "aligny top");
        body.add(head, "gapbottom 8");

        body.add(BookingUi.banner(b, false), "gapbottom 4");
        body.add(BookingUi.stayStrip(b), "gapbottom 4");
        if (b.getSpecialRequests() != null) {
            body.add(Ui.label("Special requests", Fonts.captionMedium(), Colors.TEXT_MUTED));
            body.add(Widgets.wrap(b.getSpecialRequests(), Fonts.body(), Colors.TEXT), "wmin 0, gapbottom 4");
        }
        body.add(Ui.label("Payment", Fonts.captionMedium(), Colors.TEXT_MUTED));
        body.add(BookingUi.paymentSummary(b));

        if (withMessage) {
            AppButton message = new AppButton("Message host", AppButton.Style.OUTLINED).withIcon("message-square");
            message.addActionListener(e -> {
                dialog.dispose();
                portal.openConversation(b.getOwnerId(), b.getOwnerName(), b);
            });
            dialog.addLeftButton(message);
        }
        if (b.getPaidAt() != null) {
            AppButton receipt = new AppButton("View receipt", AppButton.Style.TEXT).withIcon("receipt");
            receipt.addActionListener(e -> BookingUi.showPaymentReceipt(dialog, b));
            dialog.addLeftButton(receipt);
        }
        if ("processed".equals(b.getRefundStatus())) {
            AppButton refund = new AppButton("Refund receipt", AppButton.Style.TEXT).withIcon("refresh-ccw");
            refund.addActionListener(e -> {
                LocalDateTime at = portal.bookings.refundProcessedAt(b, renterId);
                BookingUi.showRefundReceipt(dialog, b, at == null ? b.getCancelledAt() : at);
            });
            dialog.addLeftButton(refund);
        }

        switch (b.getStage()) {
            case PENDING, UPCOMING -> {
                // Cancelling is done on the Cancellations page: it quotes the refund tier for today
                // and asks for a reason before anything happens.
                if (portal instanceof RenterPortal renter) {
                    AppButton cancel = new AppButton("Cancel booking", AppButton.Style.OUTLINED);
                    cancel.addActionListener(e -> {
                        dialog.dispose();
                        renter.openCancellation(b.getId());
                    });
                    dialog.addButton(cancel);
                }
            }
            case STAYING -> {
                // The stay completes by itself at check-out time; this is for leaving early
                AppButton checkout = new AppButton("Confirm check-out", AppButton.Style.PRIMARY);
                String note = "Your stay ends automatically at check-out, " + Formats.dateTime(b.getCheckOut())
                        + ". Confirm now only if you have already left; the stay is then completed and the "
                        + "unit is free for the next guest. This can't be undone.";
                checkout.addActionListener(e -> dialog.confirmPage("Check out early?", note,
                        "Confirm check-out", AppButton.Style.PRIMARY, () -> {
                            if (run(dialog, () -> portal.bookings.confirmCheckout(b.getId(), renterId))) {
                                changed[0] = true;
                                dialog.dispose();
                            } else {
                                dialog.previousPage();
                            }
                        }));
                dialog.addButton(checkout);
            }
            default -> {
                // Closed bookings are read-only: the window's X or Esc closes the dialog
            }
        }
        dialog.showDialog(640);
        if (changed[0]) {
            portal.refreshBadges();
        }
        return changed[0];
    }

    /** Runs an action and shows its error, if any. Returns true when it worked. */
    private static boolean run(Component parent, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuleException | DataException e) {
            AppDialog.error(parent, e.getMessage());
            return false;
        }
    }
}
