package com.gudosnest.backend.model;

/** The booking_status column. See {@link Booking#getStage()} for what the user sees. */
public enum BookingStatus {
    PENDING, APPROVED, REJECTED, EXPIRED, CANCELLED, COMPLETED;

    public static BookingStatus fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
