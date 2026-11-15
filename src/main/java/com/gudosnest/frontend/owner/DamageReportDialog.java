package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.service.OwnerService;
import com.gudosnest.backend.service.ReceiptPaths;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.ImageUtil;
import com.gudosnest.frontend.components.MoneyInput;
import com.gudosnest.frontend.components.Receipt;
import com.gudosnest.frontend.components.ReceiptDialog;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.WheelScroll;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Damage reports: file a new one against a stay, and open one to charge or dismiss it. Follow-up
 * steps (the confirmation, "Damage fee charged", the receipt) are next pages of the same dialog.
 */
final class DamageReportDialog {

    /** Dialog width, and the width wrapped text keeps inside it (the width less the 24px insets and room for a scrollbar). */
    private static final int WIDTH = 560;
    private static final int TEXT_WIDTH = WIDTH - 48 - 16;

    private DamageReportDialog() {
    }

    // ---------------------------------------------------------------------------------- file

    /**
     * New report. Only stays that have started or finished can be picked; with a property chosen on
     * the page, its newest stay is selected first. Returns true if a report was filed.
     */
    static boolean file(Component parent, Portal portal, Integer propertyId) {
        int ownerId = portal.user().getId();
        List<Booking> stays = portal.owner.reportableBookings(portal.bookings.forOwner(ownerId));
        if (stays.isEmpty()) {
            AppDialog.info(parent, "There are no stays to report on yet. A report can be filed once a stay has started.");
            return false;
        }
        boolean[] filed = {false};
        AppDialog dialog = new AppDialog(parent, "File damage report");
        JPanel body = dialog.getBody();
        body.add(Widgets.wrap("Pick a stay that has started or finished. Renters don't see damage reports;"
                + " you charge or dismiss the report afterwards.", Fonts.body(), Colors.TEXT_MUTED, TEXT_WIDTH),
                "wmin 0, gapbottom 8");

        JComboBox<Booking> stay = new JComboBox<>(stays.toArray(new Booking[0]));
        stay.setRenderer(BookingUi.stayRenderer(stay, b -> b, true));
        stays.stream().filter(b -> propertyId != null && b.getPropertyId() == propertyId)
                .findFirst().ifPresent(stay::setSelectedItem);
        body.add(Ui.field("Stay", stay), "wmin 0, gapbottom 8");

        // Label and counter above the text, 4px apart like Ui.field
        JLabel counter = Ui.caption("0 / " + OwnerService.MAX_DAMAGE_DESCRIPTION);
        JPanel descriptionField = Ui.transparent("insets 0, gap 0, fillx, wrap 2", "[grow][]");
        descriptionField.add(Ui.label("What was damaged", Fonts.bodyMedium(), Colors.TEXT));
        descriptionField.add(counter, "aligny bottom");
        JTextArea description = Ui.textArea(4);
        limit(description, counter, OwnerService.MAX_DAMAGE_DESCRIPTION);
        JScrollPane descriptionScroll = WheelScroll.install(new JScrollPane(description));
        descriptionField.add(descriptionScroll, "span 2, growx, h 88!, wmin 0, gaptop 4");
        body.add(descriptionField, "wmin 0, gapbottom 8");

        JTextField fee = Ui.textField("For example 1,200");
        body.add(Ui.field("Fee to charge (₱)", fee), "gapbottom 8");

        PhotoPicker photo = new PhotoPicker();
        body.add(Ui.fieldFree("Photo (optional)", photo.panel), "wmin 0");

        dialog.addCloseButton("Cancel");
        AppButton save = new AppButton("File report", AppButton.Style.PRIMARY);
        save.addActionListener(e -> {
            boolean ok = BookingReviewDialog.run(dialog, () -> {
                BigDecimal amount = MoneyInput.parse(fee.getText(), "Fee to charge");
                portal.owner.fileDamageReport(ownerId, (Booking) stay.getSelectedItem(), description.getText(),
                        photo.save(), amount);
            });
            if (ok) {
                filed[0] = true;
                dialog.dispose();
            }
        });
        dialog.addButton(save);
        dialog.getRootPane().setDefaultButton(save);
        dialog.showDialog(WIDTH);
        return filed[0];
    }

    /** Stops typing at {@code max} characters and shows "n / max" in the counter. */
    private static void limit(JTextArea area, JLabel counter, int max) {
        ((AbstractDocument) area.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String add = text == null ? "" : text;
                int room = max - (fb.getDocument().getLength() - length);
                super.replace(fb, offset, length, add.length() > room ? add.substring(0, Math.max(0, room)) : add, attrs);
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }
        });
        area.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                counter.setText(area.getDocument().getLength() + " / " + max);
            }
        });
    }

    /** "Add photo" button; once a picture is picked, a preview with its name and Remove. */
    private static final class PhotoPicker {
        final JPanel panel = Ui.transparent("insets 0, gap 12, fillx", "[][grow]");
        private String picked;      // the file the owner chose
        private String stored;      // its copy in uploads/damage, once saved

        PhotoPicker() {
            refresh();
        }

        private void refresh() {
            panel.removeAll();
            if (picked == null) {
                AppButton add = new AppButton("Add photo", AppButton.Style.OUTLINED).withIcon("image-plus");
                add.addActionListener(e -> pick());
                panel.add(add, "h 40!");
                panel.add(Ui.caption("JPG or PNG of the damage"), "aligny center");
            } else {
                panel.add(new Widgets.Photo(picked, 8, true), "w 96!, h 72!");
                JPanel side = Ui.transparent("insets 0, gap 0, wrap 1", "[grow,fill]");
                side.add(Ui.body(new File(picked).getName()), "wmin 0");
                AppButton remove = new AppButton("Remove", AppButton.Style.TEXT);
                remove.addActionListener(e -> {
                    picked = null;
                    stored = null;
                    refresh();
                });
                side.add(remove, "growx 0, gaptop 4");
                panel.add(side, "aligny center, wmin 0");
            }
            panel.revalidate();
            panel.repaint();
        }

        private void pick() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose a photo of the damage");
            chooser.setFileFilter(new FileNameExtensionFilter("Pictures (JPG, PNG)", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (ImageUtil.load(f.getPath()) == null) {
                    AppDialog.error(panel, f.getName() + " is not a picture this app can open (use JPG or PNG).");
                    return;
                }
                picked = f.getAbsolutePath();
                stored = null;
                refresh();
            }
        }

        /** Copies the photo into uploads/damage (once, even if Save is pressed again) and returns the path to store. */
        String save() {
            if (picked == null) {
                return null;
            }
            if (stored == null) {
                try {
                    stored = ImageUtil.copyToUploads(new File(picked), ImageUtil.DAMAGE_UPLOADS_DIR);
                } catch (IOException ex) {
                    throw new RuleException("Couldn't add the photo: " + ex.getMessage());
                }
            }
            return stored;
        }
    }

    // ---------------------------------------------------------------------------------- review

    /** Opens a report: charge or dismiss it while open, or see its receipt once charged. Returns true if it changed. */
    static boolean review(Component parent, Portal portal, DamageReport report) {
        boolean[] changed = {false};
        AppDialog dialog = new AppDialog(parent, "Damage report");
        details(dialog.getBody(), report);

        int ownerId = portal.user().getId();
        Booking b = report.booking();
        AppButton message = new AppButton("Message renter", AppButton.Style.OUTLINED).withIcon("message-square");
        message.addActionListener(e -> {
            dialog.dispose();
            portal.openConversation(b.getRenterId(), b.getRenterName(), b);
        });
        dialog.addLeftButton(message);

        switch (report.status()) {
            case "open" -> {
                AppButton dismiss = new AppButton("Dismiss", AppButton.Style.OUTLINED);
                dismiss.addActionListener(e -> dialog.confirmPage("Dismiss this report?",
                        "The report closes without a fee and " + b.getRenterName()
                                + " is not charged. This can't be undone.",
                        "Dismiss report", AppButton.Style.DANGER, () -> {
                            if (BookingReviewDialog.run(dialog, () -> portal.owner.dismissDamage(ownerId, report))) {
                                changed[0] = true;
                                dialog.dispose();
                            } else {
                                dialog.previousPage();
                            }
                        }));
                AppButton charge = new AppButton("Charge " + Formats.money(report.requestedFee()), AppButton.Style.PRIMARY);
                charge.addActionListener(e -> dialog.confirmPage("Charge the damage fee?",
                        b.getRenterName() + " is charged " + Formats.money(report.requestedFee())
                                + ". It is recorded as a paid damage fee and added to this property's income."
                                + " This can't be undone.",
                        "Charge fee", AppButton.Style.PRIMARY, () -> {
                            if (BookingReviewDialog.run(dialog, () -> portal.owner.chargeDamage(ownerId, report))) {
                                changed[0] = true;
                                chargedPage(dialog, portal, report.id());
                            } else {
                                dialog.previousPage();
                            }
                        }));
                dialog.addButton(dismiss);
                dialog.addButton(charge);
                dialog.getRootPane().setDefaultButton(charge);
            }
            case "charged" -> {
                AppButton receipt = new AppButton("View receipt", AppButton.Style.OUTLINED).withIcon("receipt");
                receipt.addActionListener(e -> showReceipt(dialog, report));
                dialog.addButton(receipt);
                dialog.addCloseButton("Close");
            }
            default -> dialog.addCloseButton("Close");
        }
        dialog.showDialog(WIDTH);
        if (changed[0]) {
            portal.refreshBadges();
        }
        return changed[0];
    }

    /** "Damage fee charged": no way back to the open report, the receipt is one click away. */
    private static void chargedPage(AppDialog dialog, Portal portal, int reportId) {
        DamageReport done = portal.owner.damageReports(portal.user().getId()).stream()
                .filter(r -> r.id() == reportId).findFirst().orElse(null);
        if (done == null) {
            dialog.dispose();
            return;
        }
        Receipt.forDamageFee(done, paidAt(done)).saveQuietly(ReceiptPaths.damageFee(done.id()));

        JPanel body = dialog.replacePage("Damage fee charged");
        details(body, done);
        AppButton receipt = new AppButton("View receipt", AppButton.Style.TEXT).withIcon("receipt");
        receipt.addActionListener(e -> showReceipt(dialog, done));
        dialog.addLeftButton(receipt);
        AppButton ok = new AppButton("Done", AppButton.Style.PRIMARY);
        ok.addActionListener(e -> dialog.dispose());
        dialog.addButton(ok);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.refit();
    }

    private static void showReceipt(AppDialog dialog, DamageReport report) {
        ReceiptDialog.show(dialog, Receipt.forDamageFee(report, paidAt(report)), "damage-fee-" + report.id() + ".png");
    }

    private static LocalDateTime paidAt(DamageReport r) {
        return r.resolvedAt() == null ? LocalDateTime.now() : r.resolvedAt();
    }

    /** Place, renter and status, the photo (if any), the stay and fee, then the description. */
    private static void details(JPanel body, DamageReport r) {
        Booking b = r.booking();
        JPanel titleRow = Ui.transparent("insets 0, fillx, gap 12", "[grow][]");
        titleRow.add(Ui.label(BookingUi.place(b), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        titleRow.add(new Badge(r.statusLabel(), DamageReportsPage.tone(r)), "aligny center");
        body.add(titleRow, "wmin 0");
        body.add(Ui.muted(b.getRenterName() + " · " + Formats.ref(b.getId(), b.getCreatedAt())), "wmin 0, gapbottom 8");

        if (r.photoPath() != null && ImageUtil.load(r.photoPath()) != null) {
            body.add(new Widgets.Photo(r.photoPath(), 12, true), "h 200!, wmin 0, gapbottom 8");
        }

        Card summary = new Card(new MigLayout("insets 16, gap 0 8, fillx, wrap 1", "[grow,fill]"));
        summary.add(Ui.row("Stay", BookingUi.dates(b) + " · " + Formats.nights(b.getNights())));
        summary.add(Ui.row("Filed", Formats.dateTime(r.createdAt())));
        if (r.resolvedAt() != null) {
            summary.add(Ui.row("charged".equals(r.status()) ? "Charged" : "Dismissed", Formats.dateTime(r.resolvedAt())));
        }
        summary.add(Ui.divider(), "gaptop 4");
        JPanel total = Ui.transparent("insets 0, fillx", "[grow][right]");
        total.add(Ui.label(switch (r.status()) {
            case "charged" -> "Fee charged";
            case "dismissed" -> "Fee requested (not charged)";
            default -> "Fee to charge";
        }, Fonts.bodyBold(), Colors.TEXT));
        total.add(Ui.label(Formats.money(r.requestedFee()), Fonts.sectionTitle(), Colors.PRIMARY));
        summary.add(total);
        body.add(summary, "gapbottom 8");

        body.add(Ui.label("What was damaged", Fonts.captionMedium(), Colors.TEXT_MUTED));
        body.add(Widgets.wrap(r.description(), Fonts.body(), Colors.TEXT, TEXT_WIDTH), "wmin 0");
    }
}
