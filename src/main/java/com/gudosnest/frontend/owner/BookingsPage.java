package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.structures.SortedSearch;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.RowTableModel;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Every booking of the owner's properties (Stitch "Bookings"): status tabs with counts, a
 * property filter, a search by renter, property or booking reference, and a table. A row opens
 * the booking review dialog (approve, reject, cancel, mark as completed).
 */
public class BookingsPage extends BasePage {

    private static final String[] TABS = {"All", "Pending", "Upcoming", "Staying", "Completed", "Cancelled"};

    private int tab;
    private Integer propertyId;
    private String query = "";
    private List<Booking> bookings = List.of();
    private final RowTableModel<Booking> model = new RowTableModel<>(BookingsPage::toRow,
            "Renter", "Property / unit", "Dates", "Total", "Status");
    private JTable table;
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public BookingsPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        bookings = portal.bookings.forOwner(ownerId).stream()
                .filter(b -> propertyId == null || b.getPropertyId() == propertyId)
                .sorted(Comparator.comparingInt(Booking::getId))   // sorted by id for the reference search
                .toList();

        PageHeader header = new PageHeader("Bookings");
        header.setSubtitle("Click a booking to approve, reject, cancel or complete it.");
        PropertyFilter filter = new PropertyFilter(portal.owner.properties(ownerId), propertyId);
        filter.onChange(() -> {
            propertyId = filter.selectedId();
            reload();
        });
        header.addAction(filter);
        add(header, "gapbottom 24");

        JPanel controls = Ui.transparent("insets 0, fillx", "[][grow][]");
        TabBar tabs = new TabBar(TABS);
        for (int i = 0; i < TABS.length; i++) {
            tabs.setCount(i, (int) bookings.stream().filter(inTab(i)).count());
        }
        tabs.setSelectedIndex(tab);
        tabs.onChange(i -> {
            tab = i;
            refreshTable();
        });
        controls.add(tabs);
        JTextField search = Ui.searchField("Renter, property or Ref #GN-…");
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
        controls.add(search, "cell 2 0, w 280!, h 40!");
        add(controls, "gapbottom 16");

        table = AppTable.create(model);
        AppTable.setColumnWidth(table, 0, 200);
        AppTable.setColumnWidth(table, 1, 240);
        AppTable.setColumnWidth(table, 2, 200);
        AppTable.setColumnWidth(table, 3, 120);
        AppTable.setColumnWidth(table, 4, 140);
        RowTableModel.onRowClick(table, model, b -> {
            if (BookingReviewDialog.show(this, portal, b)) {
                reload();
            }
        });
        add(tableHolder);
        refreshTable();
    }

    private static Predicate<Booking> inTab(int tab) {
        return b -> switch (tab) {
            case 1 -> b.getStage() == Booking.Stage.PENDING;
            case 2 -> b.getStage() == Booking.Stage.UPCOMING;
            case 3 -> b.getStage() == Booking.Stage.STAYING;
            case 4 -> b.getStage() == Booking.Stage.COMPLETED;
            case 5 -> b.getStage() == Booking.Stage.CANCELLED || b.getStage() == Booking.Stage.REJECTED
                    || b.getStage() == Booking.Stage.EXPIRED;
            default -> true;
        };
    }

    private void refreshTable() {
        List<Booking> shown;
        int refId = query.isEmpty() ? -1 : SortedSearch.bookingIdFromReference(query);
        if (refId >= 0) {
            // A reference was typed: binary search the id-sorted list instead of scanning it
            int index = SortedSearch.binarySearch(bookings, refId, Booking::getId);
            shown = index < 0 ? List.of() : List.of(bookings.get(index));
        } else {
            String q = query.toLowerCase();
            shown = bookings.stream().filter(inTab(tab))
                    .filter(b -> q.isEmpty() || b.getRenterName().toLowerCase().contains(q)
                            || b.getPropertyName().toLowerCase().contains(q) || b.getUnitLabel().toLowerCase().contains(q))
                    .sorted(Comparator.comparing(Booking::getCheckIn).reversed())
                    .toList();
        }
        model.setRows(shown);
        tableHolder.removeAll();
        if (shown.isEmpty()) {
            tableHolder.add(Ui.emptyState(refId >= 0 ? "No booking with that reference."
                    : bookings.isEmpty() ? "No bookings yet." : "No bookings match."), "growx");
        } else {
            RowTableModel.fitToRows(table);
            tableHolder.add(AppTable.inCard(table));
        }
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    private static Object[] toRow(Booking b) {
        return new Object[]{
                new AppTable.TwoLine(b.getRenterName(), Formats.ref(b.getId(), b.getCreatedAt())),
                new AppTable.TwoLine(b.getPropertyName(), b.getUnitLabel()),
                new AppTable.TwoLine(BookingUi.dates(b), Formats.nights(b.getNights())),
                Formats.money(b.getTotal()),
                BookingUi.pill(b)
        };
    }
}
