package com.gudosnest.frontend.renter;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.BookingStatus;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.backend.service.refund.RefundPolicy;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Segmented;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * The renter's Cancellations page (two tabs: Request, History).
 *
 * Request: pick a pending or upcoming stay, give a reason, and see the refund that applies today
 * (from RefundPolicy, the same rules BookingService uses when it cancels). A renter's cancel takes
 * effect at once; only the refund is reviewed by the owner.
 * History: the stays this renter has cancelled, with what happened to the money.
 */
public class CancellationsPage extends BasePage {

    private static final String[] REASONS = {
            "Change of travel plans",
            "Schedule conflict",
            "Weather or transit disruption",
            "Booked different dates",
            "Other"
    };
    private static final int MAX_CONTEXT = 300;

    private boolean showHistory;
    private Integer selectedId;
    private JPanel summary;   // right column, rebuilt when another reservation is picked

    /** @param bookingId the stay to select first (from "Cancel stay" in Messages), or null */
    public CancellationsPage(Portal portal, Integer bookingId) {
        super(portal);
        this.selectedId = bookingId;
    }

    @Override
    protected void build() {
        List<Booking> mine = portal.bookings.forRenter(portal.user().getId());
        List<Booking> cancellable = mine.stream().filter(CancellationsPage::canCancel).toList();
        List<Booking> history = mine.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .sorted(Comparator.comparing(Booking::getCancelledAt).reversed()).toList();

        PageHeader header = new PageHeader("Cancellations")
                .setSubtitle("Cancel an upcoming stay and see what you get back.");
        Segmented tabs = new Segmented("Request", "History (" + history.size() + ")");
        if (showHistory) {
            tabs.setSelectedIndex(1);
        }
        tabs.onChange(i -> {      // added after setSelectedIndex so building the page doesn't reload it
            showHistory = i == 1;
            reload();
        });
        header.addAction(tabs);
        add(header, "gapbottom 24");

        add(showHistory ? historyView(history) : requestView(cancellable), "growx");
    }

    /** Pending bookings and approved stays that haven't started can be cancelled. */
    private static boolean canCancel(Booking b) {
        return b.getStage() == Booking.Stage.PENDING || b.getStage() == Booking.Stage.UPCOMING;
    }

    // ---------------------------------------------------------------------- request tab

    private JComponent requestView(List<Booking> cancellable) {
        if (cancellable.isEmpty()) {
            return Ui.emptyState("You have no pending or upcoming stays to cancel.");
        }
        Booking first = cancellable.stream().filter(b -> selectedId != null && b.getId() == selectedId)
                .findFirst().orElse(cancellable.get(0));
        selectedId = first.getId();

        JPanel columns = Ui.transparent("insets 0, gap 24, fillx", "[grow 58,fill][grow 42,fill]");
        columns.add(formCard(cancellable, first), "aligny top, wmin 0");
        summary = Ui.transparent("insets 0, gap 16, fillx, wrap 1", "[grow,fill]");
        renderSummary(first);
        columns.add(summary, "aligny top, wmin 0");
        return columns;
    }

    private JComponent formCard(List<Booking> cancellable, Booking first) {
        Card card = new Card(new MigLayout("insets 24, gap 0, fillx, wrap 1", "[grow,fill]"));
        card.add(Ui.label("New cancellation request", Fonts.cardTitle(), Colors.TEXT));
        card.add(Ui.muted("Select a reservation to see the refund you are eligible for."), "gapbottom 20");

        JComboBox<Option> stays = new JComboBox<>();
        for (Booking b : cancellable) {
            Option option = new Option(b);
            stays.addItem(option);
            if (b.getId() == first.getId()) {
                stays.setSelectedItem(option);
            }
        }
        stays.addActionListener(e -> {
            Option picked = (Option) stays.getSelectedItem();
            if (picked != null) {
                selectedId = picked.booking.getId();
                renderSummary(picked.booking);
            }
        });
        card.add(labelRow("Select reservation", "Pending and upcoming stays"), "gapbottom 6");
        card.add(stays, "h 40!, wmin 0, gapbottom 16");

        JComboBox<String> reasons = new JComboBox<>(REASONS);
        card.add(labelRow("Reason for cancellation", null), "gapbottom 6");
        card.add(reasons, "h 40!, wmin 0, gapbottom 16");

        JLabel counter = Ui.caption("0 / " + MAX_CONTEXT);
        card.add(labelRow("Additional context (optional)", counter), "gapbottom 6");
        JTextArea notes = Ui.textArea(4);
        limit(notes, counter);
        JScrollPane notesScroll = new JScrollPane(notes);
        card.add(notesScroll, "h 105!, wmin 0, gapbottom 16");

        Card agree = Widgets.tintBox(new MigLayout("insets 10 14 10 14, fillx", "[grow,fill]"));
        JCheckBox understood = new JCheckBox("I understand this follows Gudo's Nest's cancellation policy.");
        understood.setFont(Fonts.captionMedium());
        understood.setOpaque(false);
        agree.add(understood, "wmin 0");
        card.add(agree, "gapbottom 20");

        JPanel footer = Ui.transparent("insets 0, fillx", "[grow][]");
        footer.add(Ui.iconLabel("Step 1 of 1 · The cancellation takes effect immediately", "refresh-ccw",
                Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        AppButton submit = new AppButton("Cancel this stay", AppButton.Style.PRIMARY).withIcon("calendar-x");
        submit.addActionListener(e -> submit(understood, (String) reasons.getSelectedItem(), notes.getText()));
        footer.add(submit);
        card.add(footer);
        return card;
    }

    /** The reason and optional note, as one text for the booking record. */
    private static String reasonText(String reason, String context) {
        String note = context == null ? "" : context.strip();
        return note.isEmpty() ? reason : reason + ": " + note;
    }

    private void submit(JCheckBox understood, String reason, String context) {
        if (!understood.isSelected()) {
            AppDialog.error(this, "Please confirm that you understand the cancellation policy before continuing.");
            return;
        }
        Booking b;
        RefundPolicy.Quote quote;
        try {
            b = portal.bookings.byId(selectedId);
            quote = portal.bookings.renterCancellationQuote(b);
        } catch (RuleException e) {
            AppDialog.error(this, e.getMessage());
            reload();
            return;
        }
        String what = BookingUi.place(b) + " (" + Formats.ref(b.getId(), b.getCreatedAt()) + ")";
        String outcome = quote.refundsAnything()
                ? "You get back " + Formats.money(quote.refund()) + "."
                : "No refund applies, because check-in is too close.";
        if (!AppDialog.confirm(this, "Cancel " + what + "?\n" + outcome + "\nThis can't be undone.",
                "Cancel stay")) {
            return;
        }
        showHistory = true;
        String done = "Your stay is cancelled. " + (b.getStage() == Booking.Stage.PENDING
                ? Formats.money(quote.refund()) + " is refunded to you automatically."
                : quote.refundsAnything()
                ? "A refund of " + Formats.money(quote.refund()) + " was requested; the owner will review it."
                : "No refund applies.");
        if (!act(() -> portal.bookings.cancelByRenter(b.getId(), portal.user().getId(), reasonText(reason, context)),
                done)) {
            showHistory = false;
        }
    }

    // ----------------------------------------------------------------- right-hand summary

    private void renderSummary(Booking b) {
        summary.removeAll();
        RefundPolicy.Quote quote;
        try {
            quote = portal.bookings.renterCancellationQuote(b);
        } catch (RuleException e) {
            summary.add(Ui.error(e.getMessage()));
            summary.revalidate();
            summary.repaint();
            return;
        }

        // The stay
        Card stay = new Card(new MigLayout("insets 14 14 14 16, gap 14, fillx", "[68!][grow,fill]"));
        stay.add(new Widgets.Photo(b.getCoverImage(), 12, true), "w 68!, h 68!");
        JPanel info = Ui.transparent("insets 0, gap 2, wrap 1, fillx", "[grow,fill]");
        info.add(BookingUi.badge(b), "growx 0, gapbottom 2");
        info.add(Ui.label(b.getPropertyName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        info.add(Ui.caption(BookingUi.dates(b) + " · " + Formats.nights(b.getNights())), "wmin 0");
        info.add(Ui.caption(Formats.ref(b.getId(), b.getCreatedAt())));
        stay.add(info, "wmin 0");
        summary.add(stay);

        // The refund
        long days = RefundPolicy.daysBefore(LocalDate.now(), b.getCheckIn().toLocalDate());
        Card refund = new Card(new MigLayout("insets 20, gap 0, fillx, wrap 1", "[grow,fill]"));
        refund.add(Ui.label("Expected refund", Fonts.cardTitle(), Colors.TEXT));
        refund.add(Ui.caption("Worked out from how many days are left before check-in."), "gapbottom 16");
        refund.add(Widgets.keyValue("Original booking total", Ui.bold(Formats.money(b.getTotal()))), "gapbottom 12");
        refund.add(Widgets.keyValue("Time until check-in", Ui.bold(days + (days == 1 ? " day" : " days"))),
                "gapbottom 12");
        refund.add(Widgets.keyValue("Policy tier applied", new Badge(quote.tierLabel(), toneOf(quote))),
                "gapbottom 12");
        String feeLabel = RefundPolicy.PARTIAL.equals(quote.tier()) ? "Cancellation fee (20%)" : "Cancellation fee";
        refund.add(Widgets.keyValue(feeLabel, Ui.fee(Formats.money(quote.fee()))), "gapbottom 16");

        Card estimate = Widgets.tintBox(new MigLayout("insets 12 14 12 14, gap 0, fillx, wrap 1", "[grow,fill]"));
        JPanel top = Ui.transparent("insets 0, fillx", "[grow][]");
        top.add(Ui.label("Estimated refund", Fonts.captionMedium(), Colors.TEXT));
        JPanel amount = Ui.transparent("insets 0, gap 6", "[][]");
        amount.add(Ui.label(Formats.money(quote.refund()), Fonts.sectionTitle(), Colors.PRIMARY));
        amount.add(Ui.caption("to your original payment"), "aligny bottom, gapbottom 2");
        top.add(amount);
        estimate.add(top, "gapbottom 6");
        estimate.add(Widgets.wrap(refundNote(b, quote), Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        refund.add(estimate);
        summary.add(refund);

        // The policy (same text as everywhere else)
        summary.add(Widgets.policyBox());

        summary.revalidate();
        summary.repaint();
    }

    private static Badge.Tone toneOf(RefundPolicy.Quote quote) {
        return switch (quote.tier()) {
            case RefundPolicy.PARTIAL -> Badge.Tone.NEUTRAL;
            case RefundPolicy.NONE -> Badge.Tone.MUTED;
            default -> Badge.Tone.INFO;
        };
    }

    private static String refundNote(Booking b, RefundPolicy.Quote quote) {
        if (b.getStage() == Booking.Stage.PENDING) {
            return "The owner hasn't approved this yet, so you're refunded in full, automatically.";
        }
        return quote.refundsAnything()
                ? "The owner reviews the refund after you cancel."
                : "Check-in is within 2 days, so this stay can't be refunded.";
    }

    // ------------------------------------------------------------------------ history tab

    private JComponent historyView(List<Booking> history) {
        Card card = new Card(new MigLayout("insets 20 24 20 24, gap 0, fillx, wrap 1", "[grow,fill]"));
        card.add(Ui.label("Past cancellations (" + history.size() + ")", Fonts.cardTitle(), Colors.TEXT),
                "gapbottom 14");
        if (history.isEmpty()) {
            card.add(Ui.emptyState("You haven't cancelled any stays."));
            return card;
        }
        for (Booking b : history) {
            card.add(historyRow(b), "gapbottom 10");
        }
        return card;
    }

    private JComponent historyRow(Booking b) {
        Card row = new Card(new MigLayout("insets 12 16 12 16, fillx", "[grow,fill][right]"));
        row.setBackground(Colors.PAGE);
        JPanel left = Ui.transparent("insets 0, gap 2, wrap 1", "[grow,fill]");
        left.add(Ui.label(b.getPropertyName() + "  (" + Formats.ref(b.getId(), b.getCreatedAt()) + ")",
                Fonts.bodyBold(), Colors.TEXT), "wmin 0");
        String by = "owner".equals(b.getCancelledBy()) ? " by the owner" : "";
        left.add(Ui.caption("Cancelled" + by + " on " + Formats.date(b.getCancelledAt()) + " · " + tierText(b)),
                "wmin 0");
        row.add(left, "wmin 0");
        row.add(amountText(b));
        return row;
    }

    private static String tierText(Booking b) {
        if (!b.hasRefund()) {
            return "No refund";
        }
        return RefundPolicy.PARTIAL.equals(b.getRefundTier()) ? "Partial refund" : "Full refund";
    }

    private static JLabel amountText(Booking b) {
        if (!b.hasRefund()) {
            return Ui.label("Refunded: " + Formats.money(java.math.BigDecimal.ZERO), Fonts.bodyBold(), Colors.PRIMARY);
        }
        return switch (b.getRefundStatus()) {
            case "processed" -> Ui.label("Refunded: " + Formats.money(b.getRefundAmount()), Fonts.bodyBold(), Colors.PRIMARY);
            case "requested" -> Ui.label("Pending review: " + Formats.money(b.getRefundAmount()), Fonts.bodyBold(),
                    Colors.PRIMARY);
            default -> Ui.label("Refund declined", Fonts.bodyBold(), Colors.TEXT_MUTED);
        };
    }

    // --------------------------------------------------------------------------- helpers

    private static JComponent labelRow(String left, Object right) {
        JPanel row = Ui.transparent("insets 0, fillx", "[grow][]");
        row.add(Ui.label(left, Fonts.bodyMedium(), Colors.TEXT));
        if (right instanceof JComponent c) {
            row.add(c);
        } else if (right instanceof String s) {
            row.add(Ui.caption(s));
        }
        return row;
    }

    /** Stops the text at 300 characters and keeps the "n / 300" counter up to date. */
    private static void limit(JTextArea area, JLabel counter) {
        ((AbstractDocument) area.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String add = text == null ? "" : text;
                int room = MAX_CONTEXT - (fb.getDocument().getLength() - length);
                if (room > 0) {
                    super.replace(fb, offset, length, add.length() > room ? add.substring(0, room) : add, attrs);
                } else if (add.isEmpty()) {
                    super.replace(fb, offset, length, add, attrs);
                }
            }
        });
        area.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                counter.setText(area.getText().length() + " / " + MAX_CONTEXT);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
    }

    /** One entry in the reservation dropdown: "Highland Glass Cabin · Nov 18 – Nov 22, 2026 · Ref #… (₱27,400.00)". */
    private static final class Option {
        final Booking booking;

        Option(Booking booking) {
            this.booking = booking;
        }

        @Override
        public String toString() {
            return booking.getPropertyName() + " · " + BookingUi.dates(booking) + " · "
                    + Formats.ref(booking.getId(), booking.getCreatedAt()) + " (" + Formats.money(booking.getTotal()) + ")";
        }
    }
}
