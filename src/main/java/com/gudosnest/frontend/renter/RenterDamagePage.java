package com.gudosnest.frontend.renter;

import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Pager;
import com.gudosnest.frontend.components.RowTableModel;
import com.gudosnest.frontend.components.StatCard;
import com.gudosnest.frontend.components.StatRow;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.DamageUi;

import javax.swing.JPanel;
import javax.swing.JTable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Damage a host reported after the renter's stay. The renter pays each report within 48 hours;
 * while one is overdue they can't book (the banner says so). A row opens the report to pay it or
 * to see its receipt.
 */
public class RenterDamagePage extends BasePage {

    private static final String[] TABS = {"All", "To pay", "Paid", "Dismissed"};

    private int tab;
    private List<DamageReport> reports = List.of();
    private final RowTableModel<DamageReport> model = new RowTableModel<>(RenterDamagePage::toRow,
            "Reported", "Property / unit", "Damage", "Fee", "Pay by", "Status");
    private JTable table;
    private final Pager pager = new Pager();
    private List<DamageReport> shown = List.of();
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public RenterDamagePage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        int renterId = portal.user().getId();
        LocalDateTime now = LocalDateTime.now();
        // Unpaid first (soonest due on top), then the rest newest first
        reports = portal.damage.forRenter(renterId).stream()
                .sorted(Comparator.comparing((DamageReport r) -> !r.isOpen())
                        .thenComparing(r -> r.isOpen() ? r.dueAt() : LocalDateTime.MAX)
                        .thenComparing(DamageReport::createdAt, Comparator.reverseOrder()))
                .toList();

        PageHeader header = new PageHeader("Damage reports");
        header.setSubtitle("Damage a host reported after your stay. Pay within "
                + DamageReport.PAY_WITHIN_HOURS + " hours to keep booking.");
        add(header, "gapbottom 24");

        List<DamageReport> open = reports.stream().filter(inTab(1)).toList();
        long overdue = open.stream().filter(r -> r.isOverdue(now)).count();
        if (overdue > 0) {
            add(DamageUi.suspendedBanner((int) overdue, null), "gapbottom 24");
        }

        StatRow stats = new StatRow();
        stats.addCard(stat("To pay", "wallet", Formats.money(sum(open)),
                open.isEmpty() ? "Nothing to pay" : open.size() + (open.size() == 1 ? " report" : " reports")));
        stats.addCard(stat("Overdue", "clock", String.valueOf(overdue),
                overdue == 0 ? "You can book" : "Booking is paused"));
        stats.addCard(stat("Paid", "receipt", Formats.money(sum(reports.stream().filter(inTab(2)).toList())),
                "Damage fees paid"));
        add(stats, "gapbottom 24");

        TabBar tabs = new TabBar(TABS);
        for (int i = 0; i < TABS.length; i++) {
            tabs.setCount(i, (int) reports.stream().filter(inTab(i)).count());
        }
        tabs.setSelectedIndex(tab);
        tabs.onChange(i -> {
            tab = i;
            pager.reset();
            refreshTable();
        });
        add(tabs, "gapbottom 16");

        table = AppTable.create(model);
        AppTable.setColumnWidth(table, 0, 120);
        AppTable.setColumnWidth(table, 1, 220);
        AppTable.setColumnWidth(table, 2, 240);
        AppTable.setColumnWidth(table, 3, 120);
        AppTable.setColumnWidth(table, 4, 150);
        AppTable.setColumnWidth(table, 5, 150);
        RowTableModel.onRowClick(table, model, r -> {
            if (PayDamageDialog.show(this, portal, r)) {
                reload();
            }
        });
        pager.onChange(this::showPage);
        add(tableHolder, "gapbottom 16");
        add(pager);
        refreshTable();
    }

    private static StatCard stat(String title, String icon, String value, String note) {
        StatCard card = new StatCard(title, icon);
        card.setValue(value);
        card.setNote(note);
        return card;
    }

    private static BigDecimal sum(List<DamageReport> list) {
        return list.stream().map(DamageReport::requestedFee).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Predicate<DamageReport> inTab(int tab) {
        return r -> switch (tab) {
            case 1 -> r.isOpen();
            case 2 -> r.isPaid();
            case 3 -> "dismissed".equals(r.status());
            default -> true;
        };
    }

    private void refreshTable() {
        shown = reports.stream().filter(inTab(tab)).toList();
        pager.setTotal(shown.size());
        showPage();
    }

    /** The current page of reports, or the empty card. */
    private void showPage() {
        model.setRows(pager.slice(shown));
        tableHolder.removeAll();
        if (shown.isEmpty()) {
            tableHolder.add(AppTable.emptyCard(reports.isEmpty()
                    ? "No damage reports. Thanks for taking care of your stays." : "No reports here."));
        } else {
            RowTableModel.fitToRows(table);
            tableHolder.add(AppTable.inCard(table));
        }
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    private static Object[] toRow(DamageReport r) {
        String payBy = r.isOpen() ? Formats.date(r.dueAt()) : "";
        String payByTime = r.isOpen() ? Formats.time(r.dueAt()) : "";
        return new Object[]{
                new AppTable.TwoLine(Formats.date(r.createdAt()), Formats.time(r.createdAt())),
                new AppTable.TwoLine(r.booking().getPropertyName(), r.booking().getUnitLabel() + " · "
                        + Formats.ref(r.booking().getId(), r.booking().getCreatedAt())),
                DamageUi.oneLine(r.description()),
                Formats.money(r.requestedFee()),
                new AppTable.TwoLine(payBy.isEmpty() ? "—" : payBy, payByTime),
                DamageUi.pill(r)
        };
    }
}
