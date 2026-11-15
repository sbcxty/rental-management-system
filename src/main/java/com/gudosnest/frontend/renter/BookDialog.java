package com.gudosnest.frontend.renter;

import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.service.BookingService;
import com.gudosnest.backend.service.ReceiptPaths;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.backend.service.pricing.Price;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Carousel;
import com.gudosnest.frontend.components.DateField;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Receipt;
import com.gudosnest.frontend.components.Stepper;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.time.LocalDate;
import java.util.List;

/**
 * The property page a renter opens from Browse (Stitch "Property Detail"): photos and details on
 * the left, the price card on the right. The price updates as dates, unit and guests change.
 * Every rule and the payment itself are in BookingService (quote / book); this dialog only shows
 * them. Pay first: the booking is saved as pending and the owner has 48 hours to approve.
 */
final class BookDialog {

    private BookDialog() {
    }

    /**
     * Opens the property with the Browse search filled in. {@code fitting} are the units that fit
     * that search; the first one is preselected. Returns the new booking, or null if none was made.
     */
    static Booking show(Component parent, Portal portal, Property p, LocalDate checkIn, LocalDate checkOut,
                        int guests, List<Unit> fitting) {
        return new BookDialog().open(parent, portal, p, checkIn, checkOut, guests, fitting);
    }

    private Portal portal;
    private Property property;
    private AppDialog dialog;
    private Booking made;

    private JComboBox<Unit> unitPicker;
    private final DateField checkIn = new DateField("Check-in");
    private final DateField checkOut = new DateField("Check-out");
    private Stepper guests;
    private final JTextArea requests = Ui.textArea(2);
    private final JLabel rate = Ui.label("", Fonts.sectionTitle(), Colors.TEXT);
    private final JLabel about = Widgets.wrap("", Fonts.body(), Colors.TEXT);
    private final JPanel breakdown = Ui.transparent("insets 0, gap 0 6, fillx, wrap 1", "[grow,fill]");
    private final AppButton reserve = new AppButton("Reserve", AppButton.Style.PRIMARY).withIcon("credit-card");

    private Booking open(Component parent, Portal portal, Property p, LocalDate in, LocalDate out,
                         int guestCount, List<Unit> fitting) {
        this.portal = portal;
        this.property = p;
        dialog = new AppDialog(parent, p.getName());

        unitPicker = new JComboBox<>(p.getUnits().toArray(new Unit[0]));
        unitPicker.setSelectedItem(fitting.isEmpty() ? p.getUnits().get(0) : fitting.get(0));
        guests = new Stepper(guestCount, 1, selectedUnit().getCapacity());
        setUpDates(in, out);

        // The price card sets the height; the details scroll inside it, so the dialog fits a laptop screen.
        JPanel columns = new JPanel(new MigLayout("insets 0, gap 24, fill", "[440!][340!]", "[grow,fill]"));
        columns.setOpaque(false);
        JScrollPane left = Ui.scroll(details());
        left.getViewport().setBackground(Colors.CARD);
        left.getViewport().getView().setBackground(Colors.CARD);
        columns.add(left, "h 120:120, growy");
        columns.add(priceCard(), "aligny top");
        dialog.getBody().add(columns);

        AppButton message = new AppButton("Message host", AppButton.Style.OUTLINED).withIcon("message-square");
        message.addActionListener(e -> {
            dialog.dispose();
            portal.openConversation(p.getOwnerId(), p.getOwnerName(), null);
        });
        dialog.addLeftButton(message);
        dialog.addCloseButton("Close");
        reserve.addActionListener(e -> reserve());
        dialog.addButton(reserve);
        dialog.getRootPane().setDefaultButton(reserve);

        unitPicker.addActionListener(e -> refresh());
        guests.onChange(v -> refresh());
        checkOut.onChange(v -> refresh());
        refresh();
        dialog.showDialog(860);

        if (made != null) {
            showBooked(parent, made);
        }
        return made;
    }

    // ---------------------------------------------------------------------------- layout

    /** Left column: carousel, name, about the unit, check-in and check-out times, cancellation policy. */
    private JComponent details() {
        JPanel left = Ui.transparent("insets 0 0 0 12, gap 0, fillx, wrap 1", "[grow,fill]");
        Carousel carousel = new Carousel(190);
        carousel.setPhotos(property.getImages());
        left.add(carousel, "gapbottom 14");

        left.add(Ui.iconLabel(property.getLocation(), "map-pin", Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        left.add(Ui.caption("Hosted by " + property.getOwnerName() + " · " + property.getUnitSummary()
                + " · up to " + Formats.guests(property.getMaxCapacity())), "wmin 0, gapbottom 12");

        left.add(Ui.label("About this place", Fonts.bodyBold(), Colors.TEXT), "gapbottom 4");
        left.add(about, "wmin 0, gapbottom 12");

        left.add(Ui.label("Check-in and check-out", Fonts.bodyBold(), Colors.TEXT), "gapbottom 4");
        LocalDate any = LocalDate.now();
        left.add(Ui.iconLabel("Check-in after " + Formats.time(any.atTime(BookingService.CHECK_IN_TIME)),
                "clock", Fonts.caption(), Colors.TEXT));
        left.add(Ui.iconLabel("Check-out before " + Formats.time(any.atTime(BookingService.CHECK_OUT_TIME)),
                "log-out", Fonts.caption(), Colors.TEXT), "gapbottom 12");

        left.add(Widgets.policyBox());
        return left;
    }

    /** Right column: rate, unit, dates, guests, requests, live price and the 48-hour note. */
    private JComponent priceCard() {
        Card card = new Card(new MigLayout("insets 16, gap 0, fillx, wrap 1", "[grow,fill]"));
        JPanel head = Ui.transparent("insets 0, gap 4", "[][]");
        head.add(rate);
        head.add(Ui.caption("/ night"), "aligny bottom");
        card.add(head, "gapbottom 8");

        if (property.getUnits().size() > 1) {
            card.add(Ui.field("Unit", unitPicker), "gapbottom 8");
        }
        JPanel dates = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill,sg d][grow,fill,sg d]");
        dates.add(Ui.field("Check-in", checkIn), "wmin 0");
        dates.add(Ui.field("Check-out", checkOut), "wmin 0");
        card.add(dates, "gapbottom 8");
        JPanel guestRow = Ui.transparent("insets 0, fillx", "[grow][]");
        guestRow.add(Ui.label("Guests", Fonts.bodyMedium(), Colors.TEXT));
        guestRow.add(guests);
        card.add(guestRow, "gapbottom 8");
        card.add(Ui.field("Special requests (optional)", new JScrollPane(requests)), "gapbottom 12");

        card.add(breakdown, "gapbottom 10");
        card.add(Widgets.wrap("Owner approves within " + BookingService.APPROVAL_HOURS
                + " hours, or you're refunded in full.", Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        return card;
    }

    /** Same date limits as Browse: check-in from tomorrow, check-out 1 to 30 nights later. */
    private void setUpDates(LocalDate in, LocalDate out) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        checkIn.setMinimum(tomorrow);
        checkIn.setValue(in);
        limitCheckOut(in == null ? tomorrow : in);
        checkOut.setValue(out);
        checkIn.onChange(day -> {
            if (day != null) {
                limitCheckOut(day);
                LocalDate current = checkOut.getValue();
                if (current != null && (!current.isAfter(day) || current.isAfter(day.plusDays(BookingService.MAX_NIGHTS)))) {
                    checkOut.setValue(null);
                }
            }
            refresh();
        });
    }

    private void limitCheckOut(LocalDate in) {
        checkOut.setMinimum(in.plusDays(1));
        checkOut.setMaximum(in.plusDays(BookingService.MAX_NIGHTS));
    }

    // ---------------------------------------------------------------------------- behaviour

    private Unit selectedUnit() {
        return (Unit) unitPicker.getSelectedItem();
    }

    /** Re-reads the inputs and redraws the price; Reserve is enabled only for a stay that can be booked. */
    private void refresh() {
        Unit unit = selectedUnit();
        guests.setRange(1, unit.getCapacity());
        rate.setText(Formats.money(unit.getDailyRate()));
        String description = unit.getDescription() == null ? "No description yet." : unit.getDescription();
        about.setText("<html>" + Widgets.escape(description).replace("\n", "<br>") + "</html>");

        breakdown.removeAll();
        try {
            Price price = portal.bookings.quote(unit, checkIn.getValue(), checkOut.getValue(), guests.getValue());
            if (!portal.bookings.isAvailable(unit.getId(), checkIn.getValue(), checkOut.getValue())) {
                throw new RuleException(unit.getName() + " is already booked on those dates. Try other dates"
                        + (property.getUnits().size() > 1 ? " or another unit." : "."));
            }
            for (Price.Line line : price.lines()) {
                breakdown.add(Ui.row(line.label(), (line.discount() ? "-" : "") + Formats.money(line.amount())));
            }
            breakdown.add(Ui.divider(), "gaptop 2");
            JPanel total = Ui.transparent("insets 0, fillx", "[grow][right]");
            total.add(Ui.label("Total", Fonts.bodyBold(), Colors.TEXT));
            total.add(Ui.label(Formats.money(price.total()), Fonts.sectionTitle(), Colors.TEXT));
            breakdown.add(total);
            reserve.setText("Reserve · Pay " + Formats.money(price.total()));
            reserve.setEnabled(true);
        } catch (RuleException | DataException e) {
            breakdown.add(Widgets.wrap(e.getMessage(), Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
            reserve.setText("Reserve");
            reserve.setEnabled(false);
        }
        breakdown.revalidate();
        breakdown.repaint();
        growToFit();
    }

    /** The price can gain a line (discount, cleaning fee): make the open dialog taller instead of cutting it. */
    private void growToFit() {
        if (dialog.isShowing() && dialog.getPreferredSize().height > dialog.getHeight()) {
            dialog.setSize(dialog.getWidth(), dialog.getPreferredSize().height);
        }
    }

    /** Asks once more with the full amount, then pays and saves the booking (pending). */
    private void reserve() {
        Unit unit = selectedUnit();
        LocalDate in = checkIn.getValue();
        LocalDate out = checkOut.getValue();
        Price price;
        try {
            price = portal.bookings.quote(unit, in, out, guests.getValue());
        } catch (RuleException e) {
            AppDialog.error(dialog, e.getMessage());
            return;
        }
        String summary = "Pay " + Formats.money(price.total()) + " now for " + property.getName() + " · " + unit.getName()
                + ",\n" + Formats.dateRange(in, out) + " (" + Formats.nights(out.toEpochDay() - in.toEpochDay())
                + "), " + Formats.guests(guests.getValue()) + "?\n\nThe owner has " + BookingService.APPROVAL_HOURS
                + " hours to approve. If they reject it or don't answer in time, you're refunded in full.";
        if (!AppDialog.confirm(dialog, summary, "Confirm & pay")) {
            return;
        }
        try {
            Booking b = portal.bookings.book(portal.user().getId(), unit, in, out, guests.getValue(), requests.getText());
            Receipt.forPayment(b).saveQuietly(ReceiptPaths.payment(b.getId(), b.getCreatedAt()));
            made = b;
            dialog.dispose();
        } catch (RuleException | DataException e) {
            AppDialog.error(dialog, e.getMessage());
            refresh();
        }
    }

    /** "Awaiting owner approval": the reference, the stay, what was paid, and the receipt. */
    private void showBooked(Component parent, Booking b) {
        AppDialog done = new AppDialog(parent, "Payment received");
        JPanel body = done.getBody();
        body.add(Ui.label(b.getPropertyName(), Fonts.cardTitle(), Colors.TEXT));
        body.add(Ui.muted(b.getUnitLabel() + " · Reference " + Formats.refCode(b.getId(), b.getCreatedAt())),
                "gapbottom 8");
        body.add(BookingUi.banner(b, false), "gapbottom 4");
        body.add(BookingUi.stayStrip(b), "gapbottom 4");
        body.add(BookingUi.paymentSummary(b));

        AppButton receipt = new AppButton("View receipt", AppButton.Style.TEXT).withIcon("receipt");
        receipt.addActionListener(e -> BookingUi.showPaymentReceipt(done, b));
        done.addLeftButton(receipt);
        AppButton message = new AppButton("Message host", AppButton.Style.OUTLINED).withIcon("message-square");
        message.addActionListener(e -> {
            done.dispose();
            portal.openConversation(b.getOwnerId(), b.getOwnerName(), b);
        });
        done.addButton(message);
        AppButton ok = new AppButton("Done", AppButton.Style.PRIMARY);
        ok.addActionListener(e -> done.dispose());
        done.addButton(ok);
        done.getRootPane().setDefaultButton(ok);
        done.showDialog(560);
    }
}
