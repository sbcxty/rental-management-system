package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.Transaction;
import com.gudosnest.backend.service.FinanceService;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Badge;
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

import javax.swing.JPanel;
import javax.swing.JTable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * The owner's ledger: every recorded money movement, newest first. Booking income, damage fees
 * and refunds are recorded by the app; expenses (maintenance, supplies, utilities) are added by
 * the owner with "Add expense". Each line has a coloured pill for its category, and the totals at
 * the top tally the lines of the chosen property.
 */
public class FinancesPage extends BasePage {

    private static final String[] TABS = {"All", "Income", "Refunds", "Expenses"};

    private int tab;
    private Integer propertyId;
    private List<Transaction> ledger = List.of();
    private List<Transaction> shown = List.of();
    private final RowTableModel<Transaction> model = new RowTableModel<>(FinancesPage::toRow,
            "Date", "Property", "Category", "Details", "Amount");
    private JTable table;
    private final Pager pager = new Pager();
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public FinancesPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        List<Property> properties = portal.owner.properties(ownerId);
        ledger = portal.finance.ledger(ownerId, propertyId);

        PageHeader header = new PageHeader("Finances");
        header.setSubtitle("Every recorded transaction. Bookings, damage fees and refunds are added for you; add expenses yourself.");
        PropertyFilter filter = new PropertyFilter(properties, propertyId);
        filter.onChange(() -> {
            propertyId = filter.selectedId();
            pager.reset();
            reload();
        });
        header.addAction(filter);
        AppButton add = new AppButton("Add expense", AppButton.Style.PRIMARY).withIcon("plus");
        add.addActionListener(e -> {
            if (ExpenseDialog.show(this, portal, properties, propertyId)) {
                tab = 3;                        // show the new expense under Expenses
                pager.reset();
                reload();
            }
        });
        header.addAction(add);
        add(header, "gapbottom 24");

        // All-time totals of the lines listed (the same formula as the Dashboard: FinanceService)
        FinanceService.Summary all = portal.finance.summary(ledger, LocalDate.MIN, LocalDate.MAX);
        BigDecimal refunds = sum(ledger.stream().filter(inTab(2)).toList());
        StatRow stats = new StatRow();
        stats.addCard(stat("Income", "wallet", Formats.money(all.income().add(refunds)), "Bookings and damage fees"));
        stats.addCard(stat("Refunds", "refresh-ccw", Formats.money(refunds), "Paid back to renters"));
        stats.addCard(stat("Expenses", "receipt", Formats.money(all.expenses()), "Added by you"));
        stats.addCard(stat("Net profit", "credit-card", Formats.money(all.net()), "Income − refunds − expenses"));
        add(stats, "gapbottom 24");

        TabBar tabs = new TabBar(TABS);
        for (int i = 0; i < TABS.length; i++) {
            tabs.setCount(i, (int) ledger.stream().filter(inTab(i)).count());
        }
        tabs.setSelectedIndex(tab);
        tabs.onChange(i -> {
            tab = i;
            pager.reset();
            refreshTable();
        });
        add(tabs, "gapbottom 16");

        table = AppTable.create(model);
        AppTable.setColumnWidth(table, 0, 130);
        AppTable.setColumnWidth(table, 1, 210);
        AppTable.setColumnWidth(table, 2, 150);
        AppTable.setColumnWidth(table, 3, 260);
        AppTable.setColumnWidth(table, 4, 140);
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

    private static BigDecimal sum(List<Transaction> list) {
        return list.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Predicate<Transaction> inTab(int tab) {
        return t -> switch (tab) {
            case 1 -> t.isIncome();
            case 2 -> t.type().equals("refund");
            case 3 -> t.isExpense();
            default -> true;
        };
    }

    private void refreshTable() {
        shown = ledger.stream().filter(inTab(tab)).toList();
        pager.setTotal(shown.size());
        showPage();
    }

    private void showPage() {
        model.setRows(pager.slice(shown));
        tableHolder.removeAll();
        if (shown.isEmpty()) {
            tableHolder.add(AppTable.emptyCard(tab == 3 ? "No expenses yet. Use Add expense to record one."
                    : "No transactions here yet."));
        } else {
            RowTableModel.fitToRows(table);
            tableHolder.add(AppTable.inCard(table));
        }
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    /**
     * One colour per category, so the ledger can be scanned at a glance: rental income green,
     * damage fee blue, refund amber, maintenance Primary tint, supplies grey, utilities Primary.
     * (Never red: red is only for destructive buttons and errors.)
     */
    static Badge.Tone tone(Transaction t) {
        return switch (t.type()) {
            case "rental_income" -> Badge.Tone.SUCCESS;
            case "damage_fee_income" -> Badge.Tone.INFO;
            case "refund" -> Badge.Tone.WARNING;
            case "maintenance_expense" -> Badge.Tone.NEUTRAL;
            case "supply_expense" -> Badge.Tone.MUTED;
            default -> Badge.Tone.STRONG;
        };
    }

    private static Object[] toRow(Transaction t) {
        String sign = t.isIncome() ? "+" : "−";
        return new Object[]{
                new AppTable.TwoLine(Formats.date(t.date()), Formats.time(t.date())),
                t.propertyName(),
                new AppTable.Pill(t.label(), tone(t)),
                t.note() == null || t.note().isBlank() ? "—" : t.note(),
                sign + Formats.money(t.amount())
        };
    }
}
