package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.structures.SortedSearch;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.RowTableModel;
import com.gudosnest.frontend.components.StatCard;
import com.gudosnest.frontend.components.StatRow;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Damage the owner found after a stay: totals, status tabs, a search and a table of reports.
 * "File report" opens a new report; a row opens the report to charge or dismiss it (open) or to
 * see its receipt (charged). Renters never see this page or the reports.
 */
public class DamageReportsPage extends BasePage {

    private static final String[] TABS = {"All", "Open", "Charged", "Dismissed"};

    private int tab;
    private Integer propertyId;
    private String query = "";
    /** The owner's reports, newest first (what the table shows). */
    private List<DamageReport> reports = List.of();
    /** The same reports sorted by booking id, so a typed reference is found by binary search. */
    private List<DamageReport> byBooking = List.of();
    private final RowTableModel<DamageReport> model = new RowTableModel<>(DamageReportsPage::toRow,
            "Filed", "Renter", "Property / unit", "Damage", "Fee", "Status");
    private JTable table;
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public DamageReportsPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        List<Property> properties = portal.owner.properties(ownerId);
        reports = portal.owner.damageReports(ownerId).stream()
                .filter(r -> propertyId == null || r.booking().getPropertyId() == propertyId)
                .sorted(Comparator.comparing(DamageReport::createdAt).reversed())
                .toList();
        byBooking = reports.stream().sorted(Comparator.comparingInt(r -> r.booking().getId())).toList();

        PageHeader header = new PageHeader("Damage reports");
        header.setSubtitle("Damage found after a stay. Click a report to review it.");
        PropertyFilter filter = new PropertyFilter(properties, propertyId);
        filter.onChange(() -> {
            propertyId = filter.selectedId();
            reload();
        });
        header.addAction(filter);
        AppButton file = new AppButton("File report", AppButton.Style.PRIMARY).withIcon("plus");
        file.addActionListener(e -> {
            if (DamageReportDialog.file(this, portal, propertyId)) {
                tab = 1;                        // show the new report under Open
                reload();
            }
        });
        header.addAction(file);
        add(header, "gapbottom 24");

        List<DamageReport> open = reports.stream().filter(inTab(1)).toList();
        List<DamageReport> charged = reports.stream().filter(inTab(2)).toList();
        long dismissed = reports.stream().filter(inTab(3)).count();
        StatRow stats = new StatRow();
        stats.addCard(stat("Open reports", "triangle-alert", String.valueOf(open.size()),
                open.isEmpty() ? "Nothing waiting" : "Charge or dismiss each one"));
        stats.addCard(stat("Fees to decide", "clock", Formats.money(sum(open)), "Requested on open reports"));
        stats.addCard(stat("Charged", "wallet", Formats.money(sum(charged)), "Paid as damage fees"));
        stats.addCard(stat("Dismissed", "x", String.valueOf(dismissed), "Closed without a fee"));
        add(stats, "gapbottom 24");

        // Tabs on the left, search on the right of the same row (both are short enough to fit).
        JPanel controls = Ui.transparent("insets 0, fillx, gap 16", "[][grow][]");
        TabBar tabs = new TabBar(TABS);
        for (int i = 0; i < TABS.length; i++) {
            tabs.setCount(i, (int) reports.stream().filter(inTab(i)).count());
        }
        tabs.setSelectedIndex(tab);
        tabs.onChange(i -> {
            tab = i;
            refreshTable();
        });
        controls.add(tabs, "cell 0 0");
        JTextField search = Ui.searchField("Renter, property or Ref #GN-…");
        search.setPreferredSize(new Dimension(UIScale.scale(240), search.getPreferredSize().height));
        search.setText(query);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                query = search.getText().trim();
                refreshTable();
            }
        });
        controls.add(search, "cell 2 0, h 40!, aligny center");
        add(controls, "gapbottom 16");

        table = AppTable.create(model);
        AppTable.setColumnWidth(table, 0, 120);
        AppTable.setColumnWidth(table, 1, 180);
        AppTable.setColumnWidth(table, 2, 200);
        AppTable.setColumnWidth(table, 3, 240);
        AppTable.setColumnWidth(table, 4, 120);
        AppTable.setColumnWidth(table, 5, 120);
        RowTableModel.onRowClick(table, model, r -> {
            if (DamageReportDialog.review(this, portal, r)) {
                reload();
            }
        });
        add(tableHolder);
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
            case 1 -> "open".equals(r.status());
            case 2 -> "charged".equals(r.status());
            case 3 -> "dismissed".equals(r.status());
            default -> true;
        };
    }

    private void refreshTable() {
        List<DamageReport> shown;
        int refId = query.isEmpty() ? -1 : SortedSearch.bookingIdFromReference(query);
        if (refId >= 0) {
            // A reference was typed: a booking can have several reports, so binary search finds
            // the whole run of reports with that booking id in the id-sorted list.
            shown = SortedSearch.equalRange(byBooking, refId, r -> r.booking().getId()).stream()
                    .sorted(Comparator.comparing(DamageReport::createdAt).reversed())
                    .toList();
        } else {
            String q = query.toLowerCase();
            shown = reports.stream().filter(inTab(tab))
                    .filter(r -> q.isEmpty() || r.booking().getRenterName().toLowerCase().contains(q)
                            || r.booking().getPropertyName().toLowerCase().contains(q)
                            || r.booking().getUnitLabel().toLowerCase().contains(q)
                            || r.description().toLowerCase().contains(q))
                    .toList();
        }
        model.setRows(shown);
        tableHolder.removeAll();
        if (shown.isEmpty()) {
            tableHolder.add(Ui.emptyState(refId >= 0 ? "No damage report on that booking."
                    : reports.isEmpty() ? "No damage reports yet. Use File report after a stay."
                    : query.isEmpty() ? "No reports here." : "No reports match."), "growx");
        } else {
            RowTableModel.fitToRows(table);
            tableHolder.add(AppTable.inCard(table));
        }
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    /** Open = waiting (Primary tint), Charged = green, Dismissed = grey. */
    static Badge.Tone tone(DamageReport r) {
        return switch (r.status()) {
            case "open" -> Badge.Tone.NEUTRAL;
            case "charged" -> Badge.Tone.SUCCESS;
            default -> Badge.Tone.MUTED;
        };
    }

    private static Object[] toRow(DamageReport r) {
        return new Object[]{
                new AppTable.TwoLine(Formats.date(r.createdAt()), Formats.time(r.createdAt())),
                new AppTable.TwoLine(r.booking().getRenterName(), Formats.ref(r.booking().getId(), r.booking().getCreatedAt())),
                new AppTable.TwoLine(r.booking().getPropertyName(), r.booking().getUnitLabel()),
                firstLine(r.description()),
                Formats.money(r.requestedFee()),
                new AppTable.Pill(r.statusLabel(), tone(r))
        };
    }

    /** The description on one line (the table cell cuts it with "…" when it is too long). */
    private static String firstLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
