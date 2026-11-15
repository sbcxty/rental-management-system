package com.gudosnest.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A booking with the names and amounts the screens need (renter, unit, property, payment and
 * refund), loaded in one query. Money values are a snapshot taken when the booking was made.
 */
public class Booking {

    /** What the booking means to a person right now, which is more than the status column. */
    public enum Stage {
        PENDING("Pending"),
        UPCOMING("Upcoming"),
        STAYING("Staying now"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled"),
        REJECTED("Rejected"),
        EXPIRED("Expired");

        private final String label;

        Stage(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** Bookings that are finished one way or another. */
        public boolean isClosed() {
            return this == COMPLETED || this == CANCELLED || this == REJECTED || this == EXPIRED;
        }
    }

    private final int id;
    private final int renterId;
    private final String renterName;
    private final String renterEmail;
    private final int unitId;
    private final String unitName;
    private final int propertyId;
    private final String propertyName;
    private final String location;
    private final Property.Type propertyType;
    private final int ownerId;
    private final String ownerName;
    private final String coverImage;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime checkIn;
    private final LocalDateTime checkOut;
    private final int guests;
    private final String specialRequests;
    private final BookingStatus status;
    private final BigDecimal nightlyRate;
    private final BigDecimal cleaningFee;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final LocalDateTime cancelledAt;
    private final String cancelledBy;
    private final String cancellationReason;
    private final LocalDateTime checkoutConfirmedAt;
    private final int paymentId;
    private final LocalDateTime paidAt;
    private final Integer refundId;
    private final String refundStatus;
    private final String refundTier;
    private final BigDecimal refundAmount;
    private final BigDecimal cancellationFee;

    private Booking(Builder b) {
        id = b.id;
        renterId = b.renterId;
        renterName = b.renterName;
        renterEmail = b.renterEmail;
        unitId = b.unitId;
        unitName = b.unitName;
        propertyId = b.propertyId;
        propertyName = b.propertyName;
        location = b.location;
        propertyType = b.propertyType;
        ownerId = b.ownerId;
        ownerName = b.ownerName;
        coverImage = b.coverImage;
        createdAt = b.createdAt;
        expiresAt = b.expiresAt;
        checkIn = b.checkIn;
        checkOut = b.checkOut;
        guests = b.guests;
        specialRequests = b.specialRequests;
        status = b.status;
        nightlyRate = b.nightlyRate;
        cleaningFee = b.cleaningFee;
        discount = b.discount;
        total = b.total;
        cancelledAt = b.cancelledAt;
        cancelledBy = b.cancelledBy;
        cancellationReason = b.cancellationReason;
        checkoutConfirmedAt = b.checkoutConfirmedAt;
        paymentId = b.paymentId;
        paidAt = b.paidAt;
        refundId = b.refundId;
        refundStatus = b.refundStatus;
        refundTier = b.refundTier;
        refundAmount = b.refundAmount;
        cancellationFee = b.cancellationFee;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Stage getStage() {
        return getStage(LocalDateTime.now());
    }

    public Stage getStage(LocalDateTime now) {
        return switch (status) {
            case PENDING -> Stage.PENDING;
            case COMPLETED -> Stage.COMPLETED;
            case CANCELLED -> Stage.CANCELLED;
            case REJECTED -> Stage.REJECTED;
            case EXPIRED -> Stage.EXPIRED;
            // Once check-out time passes, BookingService.refreshStatuses() completes the stay
            // (within a few minutes at most), so until then it still counts as staying.
            case APPROVED -> now.isBefore(checkIn) ? Stage.UPCOMING : Stage.STAYING;
        };
    }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
    }

    public BigDecimal getNightsSubtotal() {
        return nightlyRate.multiply(BigDecimal.valueOf(getNights()));
    }

    public boolean hasRefund() {
        return refundId != null;
    }

    public int getId() {
        return id;
    }

    public int getRenterId() {
        return renterId;
    }

    public String getRenterName() {
        return renterName;
    }

    public String getRenterEmail() {
        return renterEmail;
    }

    public int getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getLocation() {
        return location;
    }

    public Property.Type getPropertyType() {
        return propertyType;
    }

    /** "Whole house" for a house, otherwise the unit name. */
    public String getUnitLabel() {
        return propertyType == Property.Type.HOUSE ? Property.Type.HOUSE.label() : unitName;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    public BigDecimal getCleaningFee() {
        return cleaningFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCheckoutConfirmedAt() {
        return checkoutConfirmedAt;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Integer getRefundId() {
        return refundId;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public String getRefundTier() {
        return refundTier;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    // PATTERN: Builder
    // A booking row has more than thirty values, many optional (cancellation, checkout, refund).
    // The builder sets them by name and build() checks the ones every booking must have.
    public static final class Builder {
        private int id;
        private int renterId;
        private String renterName;
        private String renterEmail;
        private int unitId;
        private String unitName;
        private int propertyId;
        private String propertyName;
        private String location;
        private Property.Type propertyType = Property.Type.HOUSE;
        private int ownerId;
        private String ownerName;
        private String coverImage;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private int guests = 1;
        private String specialRequests;
        private BookingStatus status = BookingStatus.PENDING;
        private BigDecimal nightlyRate = BigDecimal.ZERO;
        private BigDecimal cleaningFee = BigDecimal.ZERO;
        private BigDecimal discount = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;
        private LocalDateTime cancelledAt;
        private String cancelledBy;
        private String cancellationReason;
        private LocalDateTime checkoutConfirmedAt;
        private int paymentId;
        private LocalDateTime paidAt;
        private Integer refundId;
        private String refundStatus;
        private String refundTier;
        private BigDecimal refundAmount;
        private BigDecimal cancellationFee;

        private Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder renter(int id, String name, String email) {
            this.renterId = id;
            this.renterName = name;
            this.renterEmail = email;
            return this;
        }

        public Builder unit(int id, String name) {
            this.unitId = id;
            this.unitName = name;
            return this;
        }

        public Builder property(int id, String name, String location, Property.Type type, String coverImage) {
            this.propertyId = id;
            this.propertyName = name;
            this.location = location;
            this.propertyType = type;
            this.coverImage = coverImage;
            return this;
        }

        public Builder owner(int id, String name) {
            this.ownerId = id;
            this.ownerName = name;
            return this;
        }

        public Builder created(LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder stay(LocalDateTime checkIn, LocalDateTime checkOut, int guests) {
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.guests = guests;
            return this;
        }

        public Builder specialRequests(String text) {
            this.specialRequests = text;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder price(BigDecimal nightlyRate, BigDecimal cleaningFee, BigDecimal discount, BigDecimal total) {
            this.nightlyRate = nightlyRate;
            this.cleaningFee = cleaningFee;
            this.discount = discount;
            this.total = total;
            return this;
        }

        public Builder cancellation(LocalDateTime at, String by, String reason) {
            this.cancelledAt = at;
            this.cancelledBy = by;
            this.cancellationReason = reason;
            return this;
        }

        public Builder checkoutConfirmed(LocalDateTime at) {
            this.checkoutConfirmedAt = at;
            return this;
        }

        public Builder payment(int paymentId, LocalDateTime paidAt) {
            this.paymentId = paymentId;
            this.paidAt = paidAt;
            return this;
        }

        public Builder refund(Integer refundId, String status, String tier, BigDecimal amount, BigDecimal fee) {
            this.refundId = refundId;
            this.refundStatus = status;
            this.refundTier = tier;
            this.refundAmount = amount;
            this.cancellationFee = fee;
            return this;
        }

        public Booking build() {
            Objects.requireNonNull(checkIn, "checkIn");
            Objects.requireNonNull(checkOut, "checkOut");
            if (!checkOut.isAfter(checkIn)) {
                throw new IllegalStateException("Check-out must be after check-in");
            }
            return new Booking(this);
        }
    }
}
