package com.gudosnest.backend.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// PATTERN: Decorator
// A stay's price is built in layers: the nights, then a cleaning fee on top, then (for long
// stays) a discount. Each layer wraps the previous price, adds its own line to the breakdown
// and adjusts the total. New charges or discounts are new decorators; nothing else changes.
//
//   Price price = Price.forStay(rate, nights, cleaningFee);   // builds the chain below
//   = new LongStayDiscount(new CleaningFee(new NightlyRate(rate, nights), fee))
public interface Price {

    /** One row of the breakdown, for example "3 nights × ₱5,400.00" and ₱16,200.00. */
    record Line(String label, BigDecimal amount, boolean discount) {
    }

    BigDecimal total();

    /** Every row from the innermost layer out, ready to print. */
    List<Line> lines();

    /** The amount of the long-stay discount (0 when there is none), stored on the booking. */
    default BigDecimal discount() {
        return lines().stream().filter(Line::discount).map(Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** The standard price of a stay. */
    static Price forStay(BigDecimal nightlyRate, long nights, BigDecimal cleaningFee) {
        Price price = new NightlyRate(nightlyRate, nights);
        if (cleaningFee.signum() > 0) {
            price = new CleaningFee(price, cleaningFee);
        }
        return new LongStayDiscount(price, nightlyRate, nights);
    }

    // ---------------------------------------------------------------------------------------

    /** The base: rate × nights. */
    final class NightlyRate implements Price {
        private final BigDecimal rate;
        private final long nights;

        public NightlyRate(BigDecimal rate, long nights) {
            this.rate = rate;
            this.nights = nights;
        }

        @Override
        public BigDecimal total() {
            return rate.multiply(BigDecimal.valueOf(nights));
        }

        @Override
        public List<Line> lines() {
            List<Line> lines = new ArrayList<>();
            String label = nights + (nights == 1 ? " night" : " nights") + " × " + Money.format(rate);
            lines.add(new Line(label, total(), false));
            return lines;
        }
    }

    /** Shared code for every layer that wraps another price. */
    abstract class PriceDecorator implements Price {
        protected final Price inner;

        protected PriceDecorator(Price inner) {
            this.inner = inner;
        }
    }

    /** Adds the unit's one-time cleaning fee. */
    final class CleaningFee extends PriceDecorator {
        private final BigDecimal fee;

        public CleaningFee(Price inner, BigDecimal fee) {
            super(inner);
            this.fee = fee;
        }

        @Override
        public BigDecimal total() {
            return inner.total().add(fee);
        }

        @Override
        public List<Line> lines() {
            List<Line> lines = inner.lines();
            lines.add(new Line("Cleaning fee", fee, false));
            return lines;
        }
    }

    /** 10% off the nights (not the cleaning fee) for stays of 7 nights or more. */
    final class LongStayDiscount extends PriceDecorator {
        public static final int MIN_NIGHTS = 7;
        public static final BigDecimal RATE = new BigDecimal("0.10");

        private final BigDecimal amount;

        public LongStayDiscount(Price inner, BigDecimal nightlyRate, long nights) {
            super(inner);
            this.amount = nights >= MIN_NIGHTS
                    ? nightlyRate.multiply(BigDecimal.valueOf(nights)).multiply(RATE).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        @Override
        public BigDecimal total() {
            return inner.total().subtract(amount);
        }

        @Override
        public List<Line> lines() {
            List<Line> lines = inner.lines();
            if (amount.signum() > 0) {
                lines.add(new Line("Weekly discount (10%)", amount, true));
            }
            return lines;
        }
    }
}
