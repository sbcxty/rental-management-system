package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.model.Records.PaymentRow;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Receipt;
import com.gudosnest.frontend.components.ReceiptDialog;
import com.gudosnest.frontend.components.RowTableModel;
import com.gudosnest.frontend.components.StatCard;
import com.gudosnest.frontend.components.StatRow;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;

import javax.swing.JPanel;
import javax.swing.JTable;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

/**
 * Money that moved on the owner's bookings (Stitch "Payments"): totals, type tabs and a table of
 * booking payments, damage fees and refunds. A row opens its receipt, or the review for a
 * refund that is still waiting.
 */
public class PaymentsPage extends BasePage {

    private static final String[] TABS = {"All", "Booking payments", "Damage fees", "Refunds"};

    private int tab;
    private Integer propertyId;
    private List<PaymentRow> rows = List.of();
    private final RowTableModel<PaymentRow> model = new RowTableModel<>(PaymentsPage::toRow,
            "Date", "Renter", "Property / unit", "Type", "Amount", "Status");
    private JTable table;
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public PaymentsPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        List<Property> properties = portal.owner.properties(ownerId);
        String propertyName = properties.stream().filter(p -> propertyId != null && p.getId() == propertyId)
                .map(Property::getName).findFirst().orElse(null);
        rows = portal.owner.payments(ownerId).stream()
                .filter(r -> propertyName == null || r.propertyName().equals(propertyName)).toList();

        PageHeader header = new PageHeader("Payments");
        header.setSubtitle("Booking payments, damage fees and refunds. Click a row for its receipt.");
        PropertyFilter filter = new PropertyFilter(properties, propertyId);
        filter.onChange(() -> {
            propertyId = filter.selectedId();
            reload();
        });
        header.addAction(filter);
        add(header, "gapbottom 24");

        BigDecimal received = sum(rows.stream().filter(r -> !r.isRefund()).toList());
        BigDecimal refunded = sum(rows.stream().filter(r -> r.isRefund() && "Refunded".equals(r.status())).toList());
        long waiting = rows.stream().filter(r -> r.isRefund() && "Requested".equals(r.status())).count();
        StatRow stats = new StatRow();
        stats.addCard(stat("Received", "wallet", Formats.money(received), "Booking payments and damage fees"));
        stats.addCard(stat("Refunded", "refresh-ccw", Formats.money(refunded), "Paid back to renters"));
        stats.addCard(stat("Net received", "credit-card", Formats.money(received.subtract(refunded)), "Received minus refunded"));
        stats.addCard(stat("Refunds to review", "clock", String.valueOf(waiting),
                waiting == 0 ? "Nothing waiting" : "Click a Requested row to review"));
        add(stats, "gapbottom 24");

        TabBar tabs = new TabBar(TABS);
        for (int i = 0; i < TABS.length; i++) {
            tabs.setCount(i, (int) rows.stream().filter(inTab(i)).count());
        }
        tabs.setSelectedIndex(tab);
        tabs.onChange(i -> {
            tab = i;
            refreshTable();
        });
        add(tabs, "gapbottom 16");

        table = AppTable.create(model);
        AppTable.setColumnWidth(table, 0, 140);
        AppTable.setColumnWidth(table, 1, 200);
        AppTable.setColumnWidth(table, 2, 220);
        AppTable.setColumnWidth(table, 3, 140);
        AppTable.setColumnWidth(table, 4, 130);
        AppTable.setColumnWidth(table, 5, 120);
        RowTableModel.onRowClick(table, model, this::open);
        add(tableHolder);
        refreshTable();
    }

    private static StatCard stat(String title, String icon, String value, String note) {
        StatCard card = new StatCard(title, icon);
        card.setValue(value);
        card.setNote(note);
        return card;
    }

    private static BigDecimal sum(List<PaymentRow> list) {
        return list.stream().map(PaymentRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Predicate<PaymentRow> inTab(int tab) {
        return r -> switch (tab) {
            case 1 -> "Booking payment".equals(r.kind());
            case 2 -> "Damage fee".equals(r.kind());
            case 3 -> r.isRefund();
            default -> true;
        };
    }

    private void refreshTable() {
        List<PaymentRow> shown = rows.stream().filter(inTab(tab)).toList();
        model.setRows(shown);
        tableHolder.removeAll();
        if (shown.isEmpty()) {
            tableHolder.add(Ui.emptyState("No payments here yet."), "growx");
        } else {
            RowTableModel.fitToRows(table);
            tableHolder.add(AppTable.inCard(table));
        }
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    private static Object[] toRow(PaymentRow r) {
        String amount = (r.isRefund() ? "−" : "") + Formats.money(r.amount());
        Badge.Tone tone = switch (r.status()) {
            case "Requested" -> Badge.Tone.NEUTRAL;
            case "Rejected" -> Badge.Tone.MUTED;
            case "Refunded" -> Badge.Tone.STRONG;
            default -> Badge.Tone.INFO;
        };
        return new Object[]{
                new AppTable.TwoLine(r.date() == null ? "" : Formats.date(r.date()),
                        r.date() == null ? "" : Formats.time(r.date())),
                new AppTable.TwoLine(r.renterName(), Formats.ref(r.bookingId(), r.bookingCreatedAt())),
                new AppTable.TwoLine(r.propertyName(), r.unitLabel()),
                r.kind(),
                amount,
                new AppTable.Pill(r.status(), tone)
        };
    }

    /** Receipt for paid rows and processed refunds; the review dialog for a refund still waiting. */
    private void open(PaymentRow r) {
        Booking b = portal.bookings.byId(r.bookingId());
        if (r.isRefund()) {
            switch (r.status()) {
                case "Requested" -> {
                    if (RefundReviewDialog.show(this, portal, portal.owner.refund(r.refundId()))) {
                        reload();
                    }
                }
                case "Refunded" -> BookingUi.showRefundReceipt(this, b, r.date());
                default -> BookingReviewDialog.show(this, portal, b);
            }
        } else if ("Damage fee".equals(r.kind())) {
            DamageReport report = portal.owner.damageReports(portal.user().getId()).stream()
                    .filter(d -> d.booking().getId() == r.bookingId() && "charged".equals(d.status()))
                    .filter(d -> d.resolvedAt() == null || r.date() == null || d.resolvedAt().equals(r.date()))
                    .findFirst().orElse(null);
            if (report == null) {
                BookingReviewDialog.show(this, portal, b);
            } else {
                ReceiptDialog.show(this, Receipt.forDamageFee(report, r.date()), "damage-fee-" + report.id() + ".png");
            }
        } else {
            BookingUi.showPaymentReceipt(this, b);
        }
    }
}
