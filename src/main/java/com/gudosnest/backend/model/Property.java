package com.gudosnest.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** A listing owned by one owner. A house has exactly one unit; a multi-unit property has several. */
public class Property {

    public enum Type {
        HOUSE("Whole house"), MULTI_UNIT("Multi-unit");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Type fromDb(String value) {
            return valueOf(value.toUpperCase());
        }
    }

    private final int id;
    private final int ownerId;
    private final String ownerName;
    private final String name;
    private final String location;
    private final Type type;
    private final BigDecimal acquisitionCost;
    private final LocalDate acquisitionDate;
    private final List<String> images;
    private final List<Unit> units;

    private Property(Builder b) {
        this.id = b.id;
        this.ownerId = b.ownerId;
        this.ownerName = b.ownerName;
        this.name = b.name;
        this.location = b.location;
        this.type = b.type;
        this.acquisitionCost = b.acquisitionCost;
        this.acquisitionDate = b.acquisitionDate;
        this.images = List.copyOf(b.images);
        this.units = List.copyOf(b.units);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Type getType() {
        return type;
    }

    public boolean isHouse() {
        return type == Type.HOUSE;
    }

    public BigDecimal getAcquisitionCost() {
        return acquisitionCost;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public List<String> getImages() {
        return images;
    }

    /** The first photo, or null if the property has none yet. */
    public String getCoverImage() {
        return images.isEmpty() ? null : images.get(0);
    }

    public List<Unit> getUnits() {
        return units;
    }

    public long getOccupiedCount() {
        return units.stream().filter(Unit::isOccupied).count();
    }

    public int getMaxCapacity() {
        return units.stream().mapToInt(Unit::getCapacity).max().orElse(0);
    }

    /** Lowest nightly rate among the units ("From ₱2,800.00 per night"). */
    public BigDecimal getLowestRate() {
        return units.stream().map(Unit::getDailyRate).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    /** "Whole house" or "3 units". */
    public String getUnitSummary() {
        return isHouse() ? Type.HOUSE.label() : units.size() + (units.size() == 1 ? " unit" : " units");
    }

    // PATTERN: Builder
    // A property has many optional parts (acquisition date, photos, units, owner name for display).
    // The builder names each value as it is set, instead of a constructor with ten positional
    // arguments that are easy to swap by mistake, and build() checks the required ones.
    public static final class Builder {
        private int id;
        private int ownerId;
        private String ownerName;
        private String name;
        private String location;
        private Type type = Type.HOUSE;
        private BigDecimal acquisitionCost = BigDecimal.ZERO;
        private LocalDate acquisitionDate;
        private final List<String> images = new ArrayList<>();
        private final List<Unit> units = new ArrayList<>();

        private Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder owner(int ownerId, String ownerName) {
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder acquisition(BigDecimal cost, LocalDate date) {
            this.acquisitionCost = cost;
            this.acquisitionDate = date;
            return this;
        }

        public Builder images(List<String> paths) {
            this.images.addAll(paths);
            return this;
        }

        public Builder units(List<Unit> units) {
            this.units.addAll(units);
            return this;
        }

        public Property build() {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(location, "location");
            return new Property(this);
        }
    }
}
