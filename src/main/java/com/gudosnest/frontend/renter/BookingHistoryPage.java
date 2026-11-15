package com.gudosnest.frontend.renter;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.structures.SortedSearch;
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
import com.gudosnest.frontend.shared.BookingUi;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Every booking the renter has made: totals, status tabs with counts, a search by property or
 * booking reference, and a table 10 rows a page (newest check-in first). A row opens
 * {@link RenterBookingDialog}: receipts, message the host, cancel before check-in, or confirm
 * check-out once the stay has started.
 */
public class BookingHistoryPage extends BasePage {

    // PATTERN: Strategy
    // Each tab is a constant that knows which booking stages it shows. The page calls
    // tab.matches(booking) for the table and the tab counts without a switch on the tab index,
    // so adding a tab means adding one constant here.
    private enum Tab {
        ALL("All", EnumSet.allOf(Booking.Stage.class)),
        PENDING("Pending", EnumSet.of(Booking.Stage.PENDING)),
        UPCOMING("Upcoming", EnumSet.of(Booking.Stage.UPCOMING)),
        STAYING("Staying", EnumSet.of(Booking.Stage.STAYING)),
        COMPLETED("Completed", EnumSet.of(Booking.Stage.COMPLETED)),
        // Rejected and expired bookings sit with cancelled ones, as on the owner's Bookings page
        CANCELLED("Cancelled", EnumSet.of(Booking.Stage.CANCELLED, Booking.Stage.REJECTED, Booking.Stage.EXPIRED));

        private final String label;
        private final Set<Booking.Stage> stages;

        Tab(String label, Set<Booking.Stage> stages) {
            this.label = label;
            this.stages = stages;
        }

        boolean matches(Booking b) {
            return stages.contains(b.getStage());
        }
    }

    private static final Tab[] TABS = Tab.values();

    private Tab tab = Tab.ALL;
    private String query = "";
    private List<Booking> bookings = List.of();     // sorted by id, for the reference search
    private final RowTableModel<Booking> model = new RowTableModel<>(BookingHistoryPage::toRow,
            "Booked", "Property / unit", "Dates", "Total", "Status");
    private JTable table;
    private final Pager pager = new Pager();
    private List<Booking> shown = List.of();
    private String emptyText = "";
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public BookingHistoryPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        bookings = portal.bookings.forRenter(portal.user().getId()).stream()
                .sorted(Comparator.comparingInt(Booking::getId))
                .toList();

        PageHeader header = new PageHeader("Booking history");
        header.setSubtitle("Your stays and requests. Click a booking for its receipt, to cancel or to check out.");
        JTextField search = Ui.searchField("Property or Ref #GN-…");
        search.setPreferredSize(new Dimension(UIScale.scale(220), search.getPreferredSize().height));
        header.addAction(search);
        add(header, "gapbottom 24");

        add(stats(), "gapbottom 24");

        TabBar tabs = new TabBar(labels());
        for (Tab t : TABS) {
            tabs.setCount(t.ordinal(), (int) bookings.stream().filter(t::matches).count());
        }
        tabs.setSelectedIndex(tab.ordinal());
        tabs.onChange(i -> {
            tab = TABS[i];
            pager.reset();
            refreshTable();
        });
        add(tabs, "gapbottom 16");

        search.setText(query);
        Ui.onTextChange(search, () -> {
            query = search.getText().trim();
            pager.reset();
            refreshTable();
        });

        table = AppTable.create(model);
        AppTable.setColumnWidth(table, 0, 180);
        AppTable.setColumnWidth(table, 1, 240);
        AppTable.setColumnWidth(table, 2, 200);
        AppTable.setColumnWidth(table, 3, 160);
        AppTable.setColumnWidth(table, 4, 140);
        RowTableModel.onRowClick(table, model, b -> {
            if (RenterBookingDialog.show(this, portal, b, true)) {
                reload();
            }
        });
        pager.onChange(this::showPage);
        add(tableHolder, "gapbottom 16");
        add(pager);
        refreshTable();
    }

    private static String[] labels() {
        String[] labels = new String[TABS.length];
        for (Tab t : TABS) {
            labels[t.ordinal()] = t.label;
        }
        return labels;
    }

    /** Coming up, nights stayed, money spent (paid minus refunded), refunds still being reviewed. */
    private StatRow stats() {
        long pending = bookings.stream().filter(Tab.PENDING::matches).count();
        long comingUp = pending + bookings.stream().filter(Tab.UPCOMING::matches).count();
        List<Booking> completed = bookings.stream().filter(Tab.COMPLETED::matches).toList();
        long nights = completed.stream().mapToLong(Booking::getNights).sum();
        BigDecimal paid = bookings.stream().filter(b -> b.getPaidAt() != null)
                .map(Booking::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunded = bookings.stream().filter(b -> "processed".equals(b.getRefundStatus()))
                .map(Booking::getRefundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Booking> waiting = bookings.stream().filter(b -> "requested".equals(b.getRefundStatus())).toList();
        BigDecimal waitingAmount = waiting.stream().map(Booking::getRefundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        StatRow row = new StatRow();
        row.addCard(stat("Coming up", "calendar-days", String.valueOf(comingUp),
                comingUp == 0 ? "Nothing booked yet" : pending == 0 ? "All approved"
                        : pending + " waiting for approval"));
        row.addCard(stat("Nights stayed", "house", String.valueOf(nights),
                completed.size() + (completed.size() == 1 ? " completed stay" : " completed stays")));
        row.addCard(stat("Total spent", "wallet", Formats.money(paid.subtract(refunded)),
                refunded.signum() == 0 ? "Paid for your bookings" : "After " + Formats.money(refunded) + " refunded"));
        row.addCard(stat("Refunds pending", "clock", String.valueOf(waiting.size()),
                waiting.isEmpty() ? "Nothing waiting" : Formats.money(waitingAmount) + " in review"));
        return row;
    }

    private static StatCard stat(String title, String icon, String value, String note) {
        StatCard card = new StatCard(title, icon);
        card.setValue(value);
        card.setNote(note);
        return card;
    }

    private void refreshTable() {
        boolean reference = BookingUi.looksLikeReference(query);
        int refId = reference ? SortedSearch.bookingIdFromReference(query) : -1;
        // A complete reference: binary search the id-sorted list instead of scanning it
        int index = refId < 0 ? -1 : SortedSearch.binarySearch(bookings, refId, Booking::getId);
        if (index >= 0) {
            shown = List.of(bookings.get(index));
        } else if (reference) {
            // Part of a reference (still typing): every booking whose reference contains it, any tab
            shown = bookings.stream().filter(b -> BookingUi.referenceContains(b, query))
                    .sorted(Comparator.comparing(Booking::getCheckIn).reversed())
                    .toList();
        } else {
            String q = query.toLowerCase();
            shown = bookings.stream().filter(tab::matches)
                    .filter(b -> q.isEmpty() || b.getPropertyName().toLowerCase().contains(q)
                            || b.getUnitLabel().toLowerCase().contains(q)
                            || b.getLocation().toLowerCase().contains(q))
                    .sorted(Comparator.comparing(Booking::getCheckIn).reversed())
                    .toList();
        }
        emptyText = reference ? "No booking with that reference."
                : bookings.isEmpty() ? "No bookings yet. Find a place on Browse."
                : query.isEmpty() ? "No bookings here." : "No bookings match.";
        pager.setTotal(shown.size());
        showPage();
    }

    /** The current page of bookings (10 at a time), or the empty card. */
    private void showPage() {
        model.setRows(pager.slice(shown));
        tableHolder.removeAll();
        if (shown.isEmpty()) {
            tableHolder.add(AppTable.emptyCard(emptyText));
        } else {
            RowTableModel.fitToRows(table);
            tableHolder.add(AppTable.inCard(table));
        }
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    private static Object[] toRow(Booking b) {
        return new Object[]{
                new AppTable.TwoLine(Formats.date(b.getCreatedAt()), Formats.ref(b.getId(), b.getCreatedAt())),
                new AppTable.TwoLine(b.getPropertyName(), b.getUnitLabel()),
                new AppTable.TwoLine(BookingUi.dates(b), Formats.nights(b.getNights())),
                new AppTable.TwoLine(Formats.money(b.getTotal()), refundNote(b)),
                BookingUi.pill(b)
        };
    }

    /**
     * Second line under the total: what happened to the money. Never blank, so the amount lines
     * up with the two-line cells beside it.
     */
    private static String refundNote(Booking b) {
        if (!b.hasRefund()) {
            if (b.getStage() == Booking.Stage.CANCELLED) {
                return "No refund";
            }
            return b.getPaidAt() == null ? "Not paid" : "Paid " + Formats.date(b.getPaidAt());
        }
        return switch (b.getRefundStatus()) {
            case "processed" -> "Refunded " + Formats.money(b.getRefundAmount());
            case "requested" -> "Refund in review";
            default -> "Refund declined";
        };
    }
}
