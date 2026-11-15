package com.gudosnest.backend.model;

import java.math.BigDecimal;

/** One bookable space: the whole house, or one room/suite of a multi-unit property. */
public class Unit {

    private final int id;
    private final int propertyId;
    private String name;
    private final String description;
    private final int capacity;
    private final BigDecimal dailyRate;
    private final BigDecimal cleaningFee;
    private final boolean occupied;

    public Unit(int id, int propertyId, String name, String description, int capacity,
                BigDecimal dailyRate, BigDecimal cleaningFee, boolean occupied) {
        this.id = id;
        this.propertyId = propertyId;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.dailyRate = dailyRate;
        this.cleaningFee = cleaningFee;
        this.occupied = occupied;
    }

    // PATTERN: Prototype
    // "Duplicate unit" starts a new unit from an existing one: same rates, capacity and
    // description, a new name, no id (it is not saved yet) and not occupied. Copying the
    // prototype is simpler and safer than re-typing every field by hand.
    public Unit copy(String newName) {
        return new Unit(0, propertyId, newName, description, capacity, dailyRate, cleaningFee, false);
    }

    public int getId() {
        return id;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public BigDecimal getCleaningFee() {
        return cleaningFee;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
