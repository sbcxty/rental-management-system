package com.gudosnest.frontend.renter;

import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.DamageUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.Component;
import java.time.LocalDateTime;

/**
 * The renter opens a damage report: pay it (a confirm page, then "Payment received" with the
 * receipt), message the host, or see the receipt of one already paid.
 */
final class PayDamageDialog {

    private PayDamageDialog() {
    }

    /** Returns true if the report was paid. */
    static boolean show(Component parent, Portal portal, DamageReport report) {
        boolean[] paid = {false};
        Booking b = report.booking();
        AppDialog dialog = new AppDialog(parent, "Damage report");
        JPanel body = dialog.getBody();
        DamageUi.details(body, report, false);
        if (report.isOpen()) {
            String note = report.isOverdue(LocalDateTime.now())
                    ? "This is overdue, so you can't book new stays. Paying lifts that straight away."
                    : "Pay by " + Formats.dateTime(report.dueAt()) + ". If it is still unpaid then, you can't book"
                    + " new stays until it is paid. Questions? Message the host.";
            Card box = Widgets.tintBox(new MigLayout("insets 12 16 12 16, fillx", "[grow,fill]"));
            if (report.isOverdue(LocalDateTime.now())) {
                box.setBackground(Colors.WARNING_TINT);
            }
            box.add(Widgets.wrap(note, Fonts.caption(), Colors.TEXT, DamageUi.TEXT_WIDTH - 32), "wmin 0");
            body.add(box, "gaptop 8");
        }

        AppButton message = new AppButton("Message host", AppButton.Style.OUTLINED).withIcon("message-square");
        message.addActionListener(e -> {
            dialog.dispose();
            portal.openConversation(b.getOwnerId(), b.getOwnerName(), b);
        });
        dialog.addLeftButton(message);

        if (report.isOpen()) {
            dialog.addCloseButton("Close");
            AppButton pay = new AppButton("Pay " + Formats.money(report.requestedFee()), AppButton.Style.PRIMARY)
                    .withIcon("credit-card");
            pay.addActionListener(e -> dialog.confirmPage("Pay the damage fee?",
                    "You pay " + Formats.money(report.requestedFee()) + " to " + b.getOwnerName() + " for damage during your stay at "
                            + b.getPropertyName() + " (" + Formats.ref(b.getId(), b.getCreatedAt()) + "). You get a receipt"
                            + " right away.",
                    "Pay " + Formats.money(report.requestedFee()), AppButton.Style.PRIMARY, () -> {
                        DamageReport done;
                        try {
                            done = portal.damage.pay(portal.user().getId(), report.id());
                        } catch (RuleException | DataException ex) {
                            AppDialog.error(dialog, ex.getMessage());
                            dialog.previousPage();
                            return;
                        }
                        paid[0] = true;
                        DamageUi.saveReceipt(done);
                        paidPage(dialog, done);
                    }));
            dialog.addButton(pay);
            dialog.getRootPane().setDefaultButton(pay);
        } else if (report.isPaid()) {
            AppButton receipt = new AppButton("View receipt", AppButton.Style.OUTLINED).withIcon("receipt");
            receipt.addActionListener(e -> DamageUi.showReceipt(dialog, report));
            dialog.addButton(receipt);
            dialog.addCloseButton("Close");
        } else {
            dialog.addCloseButton("Close");
        }
        dialog.showDialog(DamageUi.DIALOG_WIDTH);
        if (paid[0]) {
            portal.refreshBadges();
        }
        return paid[0];
    }

    /** "Payment received": no way back to the unpaid report; the receipt is one click away. */
    private static void paidPage(AppDialog dialog, DamageReport done) {
        JPanel body = dialog.replacePage("Payment received");
        DamageUi.details(body, done, false);
        AppButton receipt = new AppButton("View receipt", AppButton.Style.TEXT).withIcon("receipt");
        receipt.addActionListener(e -> DamageUi.showReceipt(dialog, done));
        dialog.addLeftButton(receipt);
        AppButton ok = new AppButton("Done", AppButton.Style.PRIMARY);
        ok.addActionListener(e -> dialog.dispose());
        dialog.addButton(ok);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.refit();
    }
}
