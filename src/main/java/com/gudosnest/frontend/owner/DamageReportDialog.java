package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.service.DamageService;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.ImageUtil;
import com.gudosnest.frontend.components.MoneyInput;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.WheelScroll;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.shared.DamageUi;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner side of damage reports: file a new one against a stay, and open one to dismiss it or see
 * its receipt. Follow-up steps (the confirmation, the receipt) are next pages of the same dialog.
 */
final class DamageReportDialog {

    private static final int WIDTH = DamageUi.DIALOG_WIDTH;
    private static final int TEXT_WIDTH = DamageUi.TEXT_WIDTH;
    /** File report is wider: Property and Renter sit side by side and the form fits without scrolling. */
    private static final int FILE_WIDTH = 640;

    private DamageReportDialog() {
    }

    // ---------------------------------------------------------------------------------- file

    /**
     * New report: pick the property, then the renter. Only renters staying now or who checked out in
     * the last 7 days are listed, so the lists stay short. With a property chosen on the page, it
     * is selected first. Returns true if a report was filed.
     */
    static boolean file(Component parent, Portal portal, Integer propertyId) {
        int ownerId = portal.user().getId();
        List<Booking> stays = portal.damage.reportableBookings(portal.bookings.forOwner(ownerId));
        if (stays.isEmpty()) {
            AppDialog.info(parent, "No stays to report on. Damage can be reported during a stay or up to "
                    + DamageService.REPORT_WITHIN_DAYS + " days after check-out.");
            return false;
        }
        boolean[] filed = {false};
        AppDialog dialog = new AppDialog(parent, "File damage report");
        JPanel body = dialog.getBody();
        // Group the stays by property (insertion order = latest check-out first), so the property
        // list holds only places with someone to report on, and each renter list is short.
        Map<Integer, PlaceStays> byProperty = new LinkedHashMap<>();
        for (Booking b : stays) {
            byProperty.computeIfAbsent(b.getPropertyId(), id -> new PlaceStays(b.getPropertyName(), new ArrayList<>()))
                    .stays().add(b);
        }
        JComboBox<PlaceStays> place = new JComboBox<>(byProperty.values().toArray(new PlaceStays[0]));
        if (propertyId != null && byProperty.containsKey(propertyId)) {
            place.setSelectedItem(byProperty.get(propertyId));
        }
        JComboBox<Booking> stay = new JComboBox<>();
        stay.setRenderer(BookingUi.renterRenderer(stay));
        Runnable fillRenters = () -> {
            stay.removeAllItems();
            PlaceStays chosen = (PlaceStays) place.getSelectedItem();
            if (chosen != null) {
                chosen.stays().forEach(stay::addItem);
            }
        };
        fillRenters.run();
        place.addActionListener(e -> fillRenters.run());
        // Both columns start at 0 wide and share the whole row 45/55, so their widths never depend
        // on the chosen property or renter name (with content-sized columns they jumped on change).
        JPanel pickers = Ui.transparent("insets 0, gap 12 0, fillx", "[0:0,grow 45,fill][0:0,grow 55,fill]");
        pickers.add(Ui.field("Property", place), "wmin 0");
        pickers.add(Ui.field("Renter", stay), "wmin 0");
        body.add(pickers, "wmin 0");
        body.add(Ui.caption("Renters staying now or who checked out in the last " + DamageService.REPORT_WITHIN_DAYS
                + " days."), "wmin 0");

        // Label and counter above the text, 4px apart like Ui.field
        JLabel counter = Ui.caption("0 / " + DamageService.MAX_DESCRIPTION);
        JPanel descriptionField = Ui.transparent("insets 0, gap 0, fillx, wrap 2", "[grow][]");
        descriptionField.add(Ui.label("What was damaged", Fonts.bodyMedium(), Colors.TEXT));
        descriptionField.add(counter, "aligny bottom");
        JTextArea description = Ui.textArea(3);   // 3 rows = the 72px box, so no scrollbar until it is full
        limit(description, counter, DamageService.MAX_DESCRIPTION);
        JScrollPane descriptionScroll = WheelScroll.install(new JScrollPane(description));
        descriptionField.add(descriptionScroll, "span 2, growx, h 72!, wmin 0, gaptop 4");
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
                portal.damage.file(ownerId, (Booking) stay.getSelectedItem(), description.getText(),
                        photo.save(), amount);
            });
            if (ok) {
                filed[0] = true;
                dialog.dispose();
            }
        });
        dialog.addButton(save);
        dialog.getRootPane().setDefaultButton(save);
        dialog.showDialog(FILE_WIDTH);
        return filed[0];
    }

    /** One property in the picker with its reportable stays; shows as the property name. */
    private record PlaceStays(String name, List<Booking> stays) {
        @Override
        public String toString() {
            return name;
        }
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

    /**
     * Opens a report. Unpaid: the owner can dismiss it (the renter pays it from their portal).
     * Paid: its receipt. Returns true if it changed.
     */
    static boolean review(Component parent, Portal portal, DamageReport report) {
        boolean[] changed = {false};
        AppDialog dialog = new AppDialog(parent, "Damage report");
        JPanel body = dialog.getBody();
        DamageUi.details(body, report, true);
        if (report.isOpen()) {
            String note = report.isOverdue(LocalDateTime.now())
                    ? "Overdue: " + report.booking().getRenterName() + " can't book until this is paid or you dismiss it."
                    : "The renter pays this from their portal. If it is still unpaid after "
                    + DamageReport.PAY_WITHIN_HOURS + " hours, they can't book until it is paid.";
            Card box = Widgets.tintBox(new MigLayout("insets 12 16 12 16, fillx", "[grow,fill]"));
            box.add(Widgets.wrap(note, Fonts.caption(), Colors.TEXT, TEXT_WIDTH - 32), "wmin 0");
            body.add(box, "gaptop 8");
        }

        int ownerId = portal.user().getId();
        Booking b = report.booking();
        AppButton message = new AppButton("Message renter", AppButton.Style.OUTLINED).withIcon("message-square");
        message.addActionListener(e -> {
            dialog.dispose();
            portal.openConversation(b.getRenterId(), b.getRenterName(), b);
        });
        dialog.addLeftButton(message);

        if (report.isOpen()) {
            dialog.addCloseButton("Close");
            AppButton dismiss = new AppButton("Dismiss report", AppButton.Style.DANGER);
            dismiss.addActionListener(e -> dialog.confirmPage("Dismiss this report?",
                    "The report closes and " + b.getRenterName() + " won't be asked to pay "
                            + Formats.money(report.requestedFee()) + ". This can't be undone.",
                    "Dismiss report", AppButton.Style.DANGER, () -> {
                        if (BookingReviewDialog.run(dialog, () -> portal.damage.dismiss(ownerId, report.id()))) {
                            changed[0] = true;
                            dialog.dispose();
                        } else {
                            dialog.previousPage();
                        }
                    }));
            dialog.addButton(dismiss);
        } else if (report.isPaid()) {
            AppButton receipt = new AppButton("View receipt", AppButton.Style.OUTLINED).withIcon("receipt");
            receipt.addActionListener(e -> DamageUi.showReceipt(dialog, report));
            dialog.addButton(receipt);
            dialog.addCloseButton("Close");
        } else {
            dialog.addCloseButton("Close");
        }
        dialog.showDialog(WIDTH);
        if (changed[0]) {
            portal.refreshBadges();
        }
        return changed[0];
    }
}
