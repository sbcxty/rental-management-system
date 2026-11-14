package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.BookingStatus;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.Transaction;
import com.gudosnest.backend.service.FinanceService;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.BarChart;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Segmented;
import com.gudosnest.frontend.components.StatCard;
import com.gudosnest.frontend.components.StatRow;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Owner dashboard (Stitch "Owner Dashboard"): greeting, four stats, income per month,
 * today's check-ins and check-outs, and what needs attention.
 */
public class DashboardPage extends BasePage {

    private Integer propertyId;
    private int chartRange;   // 0 = 6 months, 1 = this year

    public DashboardPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        List<Property> properties = portal.owner.properties(ownerId);
        List<Property> shown = properties.stream().filter(p -> propertyId == null || p.getId() == propertyId).toList();
        List<Booking> bookings = portal.bookings.forOwner(ownerId).stream()
                .filter(b -> propertyId == null || b.getPropertyId() == propertyId).toList();
        List<Transaction> ledger = portal.finance.ledger(ownerId, propertyId);

        PageHeader header = new PageHeader(greeting() + ", " + portal.user().getFullName().split(" ")[0]);
        header.setSubtitle(Formats.longDate(LocalDate.now()));
        PropertyFilter filter = new PropertyFilter(properties, propertyId);
        filter.onChange(() -> {
            propertyId = filter.selectedId();
            reload();
        });
        header.addAction(filter);
        add(header, "gapbottom 24");

        // Stats
        LocalDate today = LocalDate.now();
        FinanceService.Summary sixMonths = portal.finance.summary(ledger,
                YearMonth.now().minusMonths(5).atDay(1), today.plusDays(1));
        long active = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING
                || b.getStatus() == BookingStatus.APPROVED).count();
        int units = shown.stream().mapToInt(p -> p.getUnits().size()).sum();
        double occupancy = portal.finance.occupancyRate(bookings, units, 30);

        StatRow stats = new StatRow();
        stats.addCard(stat("Total earned", "wallet", Formats.money(sixMonths.net()), "Last 6 months, after refunds and expenses"));
        stats.addCard(stat("Active bookings", "calendar-days", String.valueOf(active), "Pending and approved"));
        stats.addCard(stat("Occupancy rate", "building-2", Formats.percent(occupancy), "Share of nights booked, last 30 days"));
        stats.addCard(stat("Properties managed", "house", String.valueOf(shown.size()),
                units + (units == 1 ? " bookable unit" : " bookable units")));
        add(stats, "gapbottom 24");

        JPanel row = Ui.transparent("insets 0, gap 24, fillx", "[grow 62,fill][grow 38,fill]");
        row.add(incomeCard(ledger), "aligny top");
        JPanel right = Ui.transparent("insets 0, gap 24, wrap 1, fillx", "[grow,fill]");
        right.add(todayCard(bookings));
        right.add(attentionCard(bookings));
        row.add(right, "aligny top");
        add(row);
    }

    private static String greeting() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.NOON)) {
            return "Good morning";
        }
        return now.isBefore(LocalTime.of(18, 0)) ? "Good afternoon" : "Good evening";
    }

    private static StatCard stat(String title, String icon, String value, String note) {
        StatCard card = new StatCard(title, icon);
        card.setValue(value);
        card.setNote(note);
        return card;
    }

    // ------------------------------------------------------------------------- income chart

    private JComponent incomeCard(List<Transaction> ledger) {
        Card card = new Card(new MigLayout("insets 24, gap 0, fillx, wrap 1", "[grow,fill]"));
        JPanel top = Ui.transparent("insets 0, fillx", "[grow][]");
        JPanel titles = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        titles.add(Ui.sectionTitle("Income per month"));
        titles.add(Ui.muted("Money received from bookings, after refunds"));
        top.add(titles);
        Segmented range = new Segmented("6 months", "This year");
        if (chartRange == 1) {
            range.setSelectedIndex(1);
        }
        range.onChange(i -> {
            chartRange = i;
            reload();
        });
        top.add(range, "aligny top");
        card.add(top, "gapbottom 16");

        YearMonth now = YearMonth.now();
        int count = chartRange == 0 ? 6 : now.getMonthValue();
        List<FinanceService.Month> months = portal.finance.monthly(ledger, now, count);
        BigDecimal total = months.stream().map(FinanceService.Month::income).reduce(BigDecimal.ZERO, BigDecimal::add);

        JPanel big = Ui.transparent("insets 0, gap 10", "[][]");
        big.add(Ui.label(Formats.money(total), Fonts.statValue(), Colors.TEXT));
        big.add(Ui.muted(chartRange == 0 ? "earned in the last 6 months" : "earned in " + now.getYear()),
                "aligny bottom, gapbottom 6");
        card.add(big);

        if (months.size() >= 2) {
            FinanceService.Month current = months.get(months.size() - 1);
            FinanceService.Month previous = months.get(months.size() - 2);
            BigDecimal diff = current.income().subtract(previous.income());
            String monthName = current.month().getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            String prevName = previous.month().getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            String change;
            if (diff.signum() == 0) {
                change = "the same as " + prevName + ".";
            } else {
                String pct = previous.income().signum() > 0
                        ? " (" + Math.round(Math.abs(diff.doubleValue() / previous.income().doubleValue() * 100)) + "%)" : "";
                change = (diff.signum() > 0 ? "up " : "down ") + Formats.money(diff.abs()) + pct + " from " + prevName + ".";
            }
            JLabel line = Ui.label(monthName + " so far: " + Formats.money(current.income()) + ", " + change,
                    Fonts.body(), Colors.TEXT_MUTED);
            line.setIcon(Icons.get(diff.signum() < 0 ? "arrow-down" : "arrow-up", 14, Colors.TEXT_MUTED));
            card.add(line, "gaptop 4, gapbottom 16");
        }
        card.add(Ui.divider(), "gapbottom 16");

        BarChart chart = new BarChart();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (FinanceService.Month m : months) {
            labels.add(Formats.shortMonth(m.month().atDay(1)));
            values.add(m.income());
        }
        chart.setData(labels, values);
        card.add(chart, "h 300!");
        return card;
    }

    // -------------------------------------------------------------------------------- today

    /** Check-ins and check-outs today. */
    private JComponent todayCard(List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        record Event(Booking booking, String kind, LocalDateTime at) {
        }
        List<Event> events = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getStatus() != BookingStatus.APPROVED) {
                continue;
            }
            if (b.getCheckIn().toLocalDate().equals(today)) {
                events.add(new Event(b, "Check-in", b.getCheckIn()));
            }
            if (b.getCheckOut().toLocalDate().equals(today)) {
                events.add(new Event(b, "Check-out", b.getCheckOut()));
            }
        }
        events.sort(Comparator.comparing(Event::at));

        Card card = new Card(new MigLayout("insets 20 24 20 24, gap 0, fillx, wrap 1", "[grow,fill]"));
        JPanel head = Ui.transparent("insets 0, fillx", "[grow][]");
        head.add(Ui.iconLabel("Today", "calendar-days", Fonts.sectionTitle(), Colors.TEXT));
        head.add(new Badge(events.size() + (events.size() == 1 ? " event" : " events"), Badge.Tone.NEUTRAL));
        card.add(head, "gapbottom 12");
        card.add(Ui.divider(), "gapbottom 4");
        if (events.isEmpty()) {
            card.add(Ui.muted("No check-ins or check-outs today."), "gaptop 12");
        }
        for (Event ev : events) {
            Card row = Widgets.clickableCard(new MigLayout("insets 10 8 10 8, gap 12, fillx", "[][grow,fill][]"),
                    () -> {
                        if (BookingReviewDialog.show(this, portal, ev.booking())) {
                            reload();
                        }
                    });
            row.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            row.add(new Badge(ev.kind(), Badge.Tone.NEUTRAL));
            JPanel text = Ui.transparent("insets 0, gap 0, wrap 1", "[grow,fill]");
            text.add(Ui.label(ev.booking().getPropertyName(), Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
            text.add(Ui.caption(ev.booking().getRenterName() + " · " + ev.booking().getUnitLabel()), "wmin 0");
            row.add(text, "wmin 0");
            row.add(Ui.label(Formats.time(ev.at()), Fonts.bodyBold(), Colors.TEXT));
            card.add(row);
        }
        return card;
    }

    // ----------------------------------------------------------------------- needs attention

    private JComponent attentionCard(List<Booking> bookings) {
        Attention attention = Attention.load(portal, bookings, propertyId);
        Card card = new Card(new MigLayout("insets 20 24 20 24, gap 0, fillx, wrap 1", "[grow,fill]"));
        JPanel head = Ui.transparent("insets 0, fillx", "[grow][]");
        head.add(Ui.sectionTitle("Needs attention"));
        head.add(new Badge(attention.total() + (attention.total() == 1 ? " item" : " items"), Badge.Tone.NEUTRAL));
        card.add(head, "gapbottom 12");
        card.add(Ui.divider(), "gapbottom 4");
        if (attention.total() == 0) {
            card.add(Ui.muted("Nothing to review. New bookings and refund requests appear here."), "gaptop 12");
            return card;
        }

        if (attention.refundCount > 0) {
            Attention.Item first = attention.items.stream().filter(Attention.Item::isRefund).findFirst().orElseThrow();
            Booking b = first.subject();
            card.add(attentionRow("Refund requests", attention.refundCount + " to review",
                    Formats.ref(b.getId(), b.getCreatedAt()) + " · " + b.getPropertyName() + " · "
                            + Formats.money(first.refund().amount()), 1));
        }
        if (attention.pendingCount > 0) {
            List<String> names = attention.items.stream().filter(i -> !i.isRefund())
                    .map(i -> i.subject().getPropertyName()).distinct().toList();
            card.add(attentionRow("Pending bookings", attention.pendingCount + " awaiting",
                    "Awaiting approval (" + String.join(", ", names) + ")", 2));
        }
        card.add(Ui.divider(), "gaptop 4, gapbottom 12");
        card.add(Widgets.link("View all (" + attention.total() + (attention.total() == 1 ? " item)" : " items)"), true,
                () -> portal.open(new NeedsAttentionPage(portal, 0, propertyId))));
        return card;
    }

    private JComponent attentionRow(String title, String count, String detail, int tab) {
        Card row = Widgets.clickableCard(new MigLayout("insets 12 8 12 8, gap 4 2, fillx", "[grow,fill][]"),
                () -> portal.open(new NeedsAttentionPage(portal, tab, propertyId)));
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        JPanel titleRow = Ui.transparent("insets 0, gap 8", "[][]");
        titleRow.add(Ui.label("●", Fonts.caption(), tab == 1 ? Colors.DANGER : Colors.PRIMARY));
        titleRow.add(Ui.label(title, Fonts.bodyBold(), Colors.TEXT));
        titleRow.add(new Badge(count, Badge.Tone.NEUTRAL));
        row.add(titleRow);
        JLabel chevron = new JLabel(Icons.get("chevron-right", 16, Colors.TEXT_MUTED), SwingConstants.CENTER);
        row.add(chevron, "spany 2, wrap");
        row.add(Ui.caption(detail), "wmin 0, gapleft 20");
        return row;
    }
}
