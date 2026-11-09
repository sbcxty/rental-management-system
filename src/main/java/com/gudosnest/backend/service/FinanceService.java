package com.gudosnest.backend.service;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.BookingStatus;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.Transaction;
import com.gudosnest.backend.repository.MoneyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Money figures for the Dashboard and Finances pages, all computed from the ledger
 * (FinancialTransactions) so every screen agrees:
 *   income   = rental income + damage fees - refunds paid out
 *   expenses = maintenance + supplies + utilities
 *   net      = income - expenses
 */
public class FinanceService {

    /** Expense categories in the order they are listed. Keys are transaction types. */
    public static final Map<String, String> EXPENSE_TYPES = new LinkedHashMap<>();

    static {
        EXPENSE_TYPES.put("maintenance_expense", "Maintenance");
        EXPENSE_TYPES.put("supply_expense", "Supplies");
        EXPENSE_TYPES.put("utility_expense", "Utilities");
    }

    /** Income and expenses of one month. */
    public record Month(YearMonth month, BigDecimal income, BigDecimal expenses) {
        public BigDecimal net() {
            return income.subtract(expenses);
        }
    }

    /** Totals for a period. */
    public record Summary(BigDecimal income, BigDecimal expenses, int bookings, Map<String, BigDecimal> expenseByType) {
        public BigDecimal net() {
            return income.subtract(expenses);
        }
    }

    private final MoneyRepository money = new MoneyRepository();

    public List<Transaction> ledger(int ownerId, Integer propertyId) {
        return money.transactions(ownerId, propertyId);
    }

    /** Signed effect of one ledger line on income (expenses are counted separately). */
    private static BigDecimal incomeEffect(Transaction t) {
        if (t.isIncome()) {
            return t.amount();
        }
        return t.type().equals("refund") ? t.amount().negate() : BigDecimal.ZERO;
    }

    public Summary summary(List<Transaction> ledger, LocalDate from, LocalDate toExclusive) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        int bookingCount = 0;
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        EXPENSE_TYPES.keySet().forEach(k -> byType.put(k, BigDecimal.ZERO));
        for (Transaction t : ledger) {
            LocalDate day = t.date().toLocalDate();
            if (day.isBefore(from) || !day.isBefore(toExclusive)) {
                continue;
            }
            income = income.add(incomeEffect(t));
            if (t.isExpense()) {
                expenses = expenses.add(t.amount());
                byType.merge(t.type(), t.amount(), BigDecimal::add);
            }
            if (t.type().equals("rental_income")) {
                bookingCount++;
            }
        }
        return new Summary(income, expenses, bookingCount, byType);
    }

    /**
     * Income and expenses per month for the {@code count} months ending with {@code last}.
     * DSA: TreeMap: months are keys, so the result is in calendar order whatever order the
     * ledger lines come in, and months with no activity still appear (as zero).
     */
    public List<Month> monthly(List<Transaction> ledger, YearMonth last, int count) {
        TreeMap<YearMonth, BigDecimal[]> months = new TreeMap<>();
        YearMonth first = last.minusMonths(count - 1L);
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            months.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Transaction t : ledger) {
            BigDecimal[] slot = months.get(YearMonth.from(t.date()));
            if (slot == null) {
                continue;
            }
            slot[0] = slot[0].add(incomeEffect(t));
            if (t.isExpense()) {
                slot[1] = slot[1].add(t.amount());
            }
        }
        return months.entrySet().stream().map(e -> new Month(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    /** Share (0..1) of the acquisition cost already earned back as all-time net profit. */
    public double investmentRecovery(List<Transaction> ledger, List<Property> properties) {
        BigDecimal cost = properties.stream().map(Property::getAcquisitionCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (cost.signum() == 0) {
            return 0;
        }
        Summary all = summary(ledger, LocalDate.MIN, LocalDate.MAX);
        return Math.max(0, all.net().divide(cost, 4, RoundingMode.HALF_UP).doubleValue());
    }

    public void addExpense(int ownerId, Property property, String type, BigDecimal amount, LocalDate date, String note) {
        if (property == null) {
            throw new RuleException("Choose the property this expense is for.");
        }
        if (property.getOwnerId() != ownerId) {
            throw new RuleException("That property belongs to another owner.");
        }
        if (!EXPENSE_TYPES.containsKey(type)) {
            throw new RuleException("Choose a category.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new RuleException("Enter an amount above ₱0.00.");
        }
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new RuleException("The date can't be in the future.");
        }
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > 255) {
            throw new RuleException("The note can be up to 255 characters.");
        }
        LocalDateTime at = date.equals(LocalDate.now()) ? LocalDateTime.now().withNano(0) : date.atTime(12, 0);
        money.insertTransaction(property.getId(), null, type, amount, at, cleanNote);
    }

    /**
     * Share (0..1) of unit-nights that were booked over the last {@code days} days:
     * nights covered by approved or completed stays, divided by units × days.
     */
    public double occupancyRate(List<Booking> bookings, int unitCount, int days) {
        if (unitCount == 0) {
            return 0;
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        long booked = 0;
        for (Booking b : bookings) {
            if (b.getStatus() != BookingStatus.APPROVED && b.getStatus() != BookingStatus.COMPLETED) {
                continue;
            }
            LocalDate in = b.getCheckIn().toLocalDate().isBefore(start) ? start : b.getCheckIn().toLocalDate();
            LocalDate out = b.getCheckOut().toLocalDate().isAfter(end) ? end : b.getCheckOut().toLocalDate();
            if (out.isAfter(in)) {
                booked += ChronoUnit.DAYS.between(in, out);
            }
        }
        return Math.min(1.0, booked / (double) (unitCount * (long) days));
    }
}
