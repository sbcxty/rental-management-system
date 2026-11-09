package com.gudosnest.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One inventory line (for example "Towels, 12, consumable") in a unit. */
public class Item {

    public enum Type {
        CONSUMABLE("Consumable"), EQUIPMENT("Equipment");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Condition {
        GOOD("Good"), DAMAGED("Damaged"), MAINTENANCE("Maintenance"), MISSING("Missing");

        private final String label;

        Condition(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public boolean needsAttention() {
            return this != GOOD;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final int id;
    private final int propertyId;
    private final Integer unitId;
    private final String name;
    private final Type type;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final Condition condition;
    private final LocalDateTime updatedAt;

    public Item(int id, int propertyId, Integer unitId, String name, Type type, int quantity,
                BigDecimal unitPrice, Condition condition, LocalDateTime updatedAt) {
        this.id = id;
        this.propertyId = propertyId;
        this.unitId = unitId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.condition = condition;
        this.updatedAt = updatedAt;
    }

    // PATTERN: Prototype
    // "Copy inventory from another unit" clones every item of the source unit into the target
    // unit: same name, type, quantity and price, condition reset to Good, no id (new rows).
    public Item copyTo(int targetUnitId) {
        return new Item(0, propertyId, targetUnitId, name, type, quantity, unitPrice, Condition.GOOD, null);
    }

    public BigDecimal getTotalValue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getId() {
        return id;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Condition getCondition() {
        return condition;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
