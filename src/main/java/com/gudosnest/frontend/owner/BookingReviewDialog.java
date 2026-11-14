package com.gudosnest.frontend.owner;

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
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

/**
 * Everything an owner does with one booking happens here: approve or reject a pending booking,
 * cancel an upcoming one (full refund), mark a stay as completed, refund a cancelled one whose refund
 * is waiting for review (same as on Payments: the cancellation fee stays off), see the receipt,
 * message the renter.
 * Returns true from {@link #show} if something changed, so the page can reload.
 */
public final class BookingReviewDialog {

    private BookingReviewDialog() {
    }

    public static boolean show(Component parent, Portal portal, Booking b) {
        boolean[] changed = {false};
        AppDialog dialog = new AppDialog(parent, "Booking " + Formats.refCode(b.getId(), b.getCreatedAt()));
        JPanel body = dialog.getBody();

        JPanel head = Ui.transparent("insets 0, gap 8, fillx", "[grow][]");
        JPanel names = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        names.add(Ui.label(b.getPropertyName(), Fonts.cardTitle(), Colors.TEXT));
        names.add(Ui.muted(b.getUnitLabel() + " · " + b.getLocation()));
        head.add(names);
        head.add(BookingUi.badge(b), "aligny top");
        body.add(head, "gapbottom 8");

        JPanel renter = Ui.transparent("insets 0, gap 8", "[][]");
        renter.add(new com.gudosnest.frontend.components.Avatar(initials(b.getRenterName()), 36));
        JPanel renterText = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        renterText.add(Ui.label(b.getRenterName(), Fonts.bodyMedium(), Colors.TEXT));
        renterText.add(Ui.caption(b.getRenterEmail() + " · booked " + Formats.dateTime(b.getCreatedAt())));
        renter.add(renterText);
        body.add(renter, "gapbottom 8");

        body.add(BookingUi.banner(b, true), "gapbottom 4");
        body.add(BookingUi.stayStrip(b), "gapbottom 4");
        if (b.getSpecialRequests() != null) {
            body.add(Ui.label("Special requests", Fonts.captionMedium(), Colors.TEXT_MUTED));
            body.add(Widgets.wrap(b.getSpecialRequests(), Fonts.body(), Colors.TEXT), "wmin 0, gapbottom 4");
        }
        body.add(Ui.label("Payment", Fonts.captionMedium(), Colors.TEXT_MUTED));
        body.add(BookingUi.paymentSummary(b));

        AppButton message = new AppButton("Message renter", AppButton.Style.OUTLINED).withIcon("message-square");
        message.addActionListener(e -> {
            dialog.dispose();
            portal.openConversation(b.getRenterId(), b.getRenterName(), b);
        });
        dialog.addLeftButton(message);
        AppButton receipt = new AppButton("View receipt", AppButton.Style.TEXT).withIcon("receipt");
        receipt.addActionListener(e -> BookingUi.showPaymentReceipt(dialog, b));
        dialog.addLeftButton(receipt);

        int ownerId = portal.user().getId();
        switch (b.getStage()) {
            case PENDING -> {
                AppButton reject = new AppButton("Reject", AppButton.Style.DANGER);
                reject.addActionListener(e -> dialog.confirmPage("Reject this booking?",
                        b.getRenterName() + " is refunded " + Formats.money(b.getTotal()) + " in full, automatically.",
                        "Reject booking", AppButton.Style.DANGER, () -> {
                            if (run(dialog, () -> portal.bookings.reject(b.getId(), ownerId))) {
                                changed[0] = true;
                                dialog.dispose();
                            } else {
                                dialog.previousPage();
                            }
                        }));
                AppButton approve = new AppButton("Approve", AppButton.Style.PRIMARY);
                approve.addActionListener(e -> {
                    List<Booking>[] rejected = new List[1];
                    if (run(dialog, () -> rejected[0] = portal.bookings.approve(b.getId(), ownerId))) {
                        changed[0] = true;
                        dialog.dispose();
                        if (!rejected[0].isEmpty()) {
                            AppDialog.info(parent, "Booking approved", rejected[0].size()
                                    + (rejected[0].size() == 1 ? " other pending booking overlapped these dates and was"
                                    : " other pending bookings overlapped these dates and were")
                                    + " rejected with a full refund.");
                        }
                    }
                });
                dialog.addButton(reject);
                dialog.addButton(approve);
            }
            case UPCOMING -> {
                AppButton cancel = new AppButton("Cancel booking", AppButton.Style.DANGER);
                cancel.addActionListener(e -> askReason(dialog, reason -> {
                    if (run(dialog, () -> portal.bookings.cancelByOwner(b.getId(), ownerId, reason))) {
                        changed[0] = true;
                        dialog.dispose();
                        AppDialog.info(parent, "Booking cancelled", b.getRenterName() + " was refunded "
                                + Formats.money(b.getTotal()) + " in full.");
                    }
                }));
                dialog.addButton(cancel);
            }
            case STAYING -> {
                AppButton complete = new AppButton("Mark as completed", AppButton.Style.PRIMARY);
                complete.addActionListener(e -> dialog.confirmPage("Mark this stay as completed?",
                        "The unit becomes available again.", "Mark as completed", AppButton.Style.PRIMARY, () -> {
                            if (run(dialog, () -> portal.bookings.markCompleted(b.getId(), ownerId))) {
                                changed[0] = true;
                                dialog.dispose();
                            } else {
                                dialog.previousPage();
                            }
                        }));
                dialog.addButton(complete);
            }
            case CANCELLED -> {
                // Renter cancelled an approved booking and the refund waits for the owner's review
                if (b.getRefundId() != null && "requested".equals(b.getRefundStatus())) {
                    AppButton refund = new AppButton("Refund", AppButton.Style.PRIMARY).withIcon("refresh-ccw");
                    refund.addActionListener(e -> RefundReviewDialog.showAsPage(dialog, parent, portal,
                            portal.owner.refund(b.getRefundId()), changed));
                    dialog.addButton(refund);
                }
            }
            default -> {
                // Nothing to do: the window's X or Esc closes it
            }
        }
        dialog.showDialog(640);
        if (changed[0]) {
            portal.refreshBadges();
        }
        return changed[0];
    }

    /** Next page of the booking dialog: asks the owner why they are cancelling, then runs onReason. */
    private static void askReason(AppDialog dialog, Consumer<String> onReason) {
        JPanel page = dialog.nextPage("Cancel this booking?");
        page.add(Widgets.wrap("The renter is refunded in full, automatically. Tell them why "
                + "you're cancelling; they'll see this reason.", Fonts.body(), Colors.TEXT), "wmin 0");
        JTextArea reason = Ui.textArea(4);
        page.add(Ui.fieldFree("Reason", com.gudosnest.frontend.components.WheelScroll.install(new JScrollPane(reason))));
        JLabel error = Ui.error("Enter a reason for the renter.");
        error.setVisible(false);
        page.add(error, "hidemode 3");
        dialog.addBackButton("Go back");
        AppButton confirm = new AppButton("Cancel booking", AppButton.Style.DANGER);
        confirm.addActionListener(e -> {
            if (reason.getText().isBlank()) {
                error.setVisible(true);
                page.revalidate();
                reason.requestFocusInWindow();
                return;
            }
            onReason.accept(reason.getText().trim());
        });
        dialog.addButton(confirm);
        dialog.refit();
        reason.requestFocusInWindow();
    }

    /** Runs an action and shows its error, if any. Returns true when it worked. */
    static boolean run(Component parent, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuleException | DataException e) {
            AppDialog.error(parent, e.getMessage());
            return false;
        }
    }

    static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
