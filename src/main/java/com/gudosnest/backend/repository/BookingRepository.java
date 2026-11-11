package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.BookingStatus;
import com.gudosnest.backend.model.Property;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Bookings joined with renter, unit, property, owner, rental payment and refund. */
public class BookingRepository {

    static final String SELECT = """
            SELECT b.*, r.full_name AS renter_name, r.email AS renter_email, u.unit_name,
                   p.property_id, p.property_name, p.location, p.property_type,
                   o.user_id AS owner_id, o.full_name AS owner_name,
                   (SELECT i.image_path FROM PropertyImages i WHERE i.property_id = p.property_id
                    ORDER BY i.sort_order, i.image_id LIMIT 1) AS cover_image,
                   pay.payment_id, pay.payment_date,
                   rf.refund_id, rf.refund_status, rf.policy_tier, rf.refund_amount, rf.cancellation_fee AS refund_fee,
                   rf.requested_at AS refund_requested_at, rf.processed_at AS refund_processed_at
            FROM Bookings b
            JOIN Users r ON r.user_id = b.renter_id
            JOIN Units u ON u.unit_id = b.unit_id
            JOIN Properties p ON p.property_id = u.property_id
            JOIN Users o ON o.user_id = p.owner_id
            LEFT JOIN Payments pay ON pay.booking_id = b.booking_id AND pay.payment_type = 'rental_payment'
            LEFT JOIN Refunds rf ON rf.payment_id = pay.payment_id
            """;

    public List<Booking> forOwner(int ownerId) {
        return Db.list(SELECT + " WHERE p.owner_id = ? ORDER BY b.booking_id", BookingRepository::map, ownerId);
    }

    public List<Booking> forRenter(int renterId) {
        return Db.list(SELECT + " WHERE b.renter_id = ? ORDER BY b.booking_id", BookingRepository::map, renterId);
    }

    /** The most recently made booking by this renter at any of this owner's properties, if any. */
    public Optional<Booking> latestBetween(int renterId, int ownerId) {
        return Db.one(SELECT + " WHERE b.renter_id = ? AND p.owner_id = ? ORDER BY b.created_at DESC, b.booking_id DESC LIMIT 1",
                BookingRepository::map, renterId, ownerId);
    }

    public Optional<Booking> byId(int bookingId) {
        return Db.one(SELECT + " WHERE b.booking_id = ?", BookingRepository::map, bookingId);
    }

    /** Approved bookings of a unit that haven't been completed: these block the dates. */
    public List<Booking> approvedForUnit(int unitId) {
        return Db.list(SELECT + " WHERE b.unit_id = ? AND b.booking_status = 'approved' ORDER BY b.check_in",
                BookingRepository::map, unitId);
    }

    /** Other pending bookings of the same unit that overlap [checkIn, checkOut). */
    public List<Booking> pendingOverlapping(int unitId, LocalDateTime checkIn, LocalDateTime checkOut, int excludeId) {
        return Db.list(SELECT + " WHERE b.unit_id = ? AND b.booking_status = 'pending' AND b.booking_id <> ?"
                        + " AND b.check_in < ? AND b.check_out > ?",
                BookingRepository::map, unitId, excludeId, checkOut, checkIn);
    }

    public List<Integer> expiredPendingIds(LocalDateTime now) {
        return Db.list("SELECT booking_id FROM Bookings WHERE booking_status = 'pending'"
                + " AND (expires_at <= ? OR check_in <= ?)", rs -> rs.getInt(1), now, now);
    }

    public int insert(int renterId, int unitId, LocalDateTime createdAt, LocalDateTime expiresAt,
                      LocalDateTime checkIn, LocalDateTime checkOut, int guests, String requests,
                      BigDecimal rate, BigDecimal cleaning, BigDecimal discount, BigDecimal total) {
        return Db.insert("""
                        INSERT INTO Bookings (renter_id, unit_id, created_at, expires_at, check_in, check_out,
                            number_of_renters, special_requests, booking_status, nightly_rate, cleaning_fee,
                            discount_amount, total_amount)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?, ?)""",
                renterId, unitId, createdAt, expiresAt, checkIn, checkOut, guests, requests,
                rate, cleaning, discount, total);
    }

    public void setStatus(int bookingId, BookingStatus status) {
        Db.update("UPDATE Bookings SET booking_status = ? WHERE booking_id = ?", status, bookingId);
    }

    public void cancel(int bookingId, String by, String reason, LocalDateTime at) {
        Db.update("UPDATE Bookings SET booking_status = 'cancelled', cancelled_at = ?, cancelled_by = ?,"
                + " cancellation_reason = ? WHERE booking_id = ?", at, by, reason, bookingId);
    }

    public void complete(int bookingId, LocalDateTime at) {
        Db.update("UPDATE Bookings SET booking_status = 'completed', checkout_confirmed_at = ? WHERE booking_id = ?",
                at, bookingId);
    }

    /**
     * A unit is occupied while it has an approved booking whose check-in time has passed
     * (until that booking is completed). Recomputed for every unit in one statement.
     */
    public void refreshOccupancy(LocalDateTime now) {
        Db.update("""
                UPDATE Units u SET unit_status = CASE WHEN EXISTS (
                    SELECT 1 FROM Bookings b WHERE b.unit_id = u.unit_id
                    AND b.booking_status = 'approved' AND b.check_in <= ?)
                THEN 'occupied' ELSE 'unoccupied' END""", now);
    }

    static Booking map(ResultSet rs) throws SQLException {
        Integer refundId = Db.integer(rs, "refund_id");
        return Booking.builder()
                .id(rs.getInt("booking_id"))
                .renter(rs.getInt("renter_id"), rs.getString("renter_name"), rs.getString("renter_email"))
                .unit(rs.getInt("unit_id"), rs.getString("unit_name"))
                .property(rs.getInt("property_id"), rs.getString("property_name"), rs.getString("location"),
                        Property.Type.fromDb(rs.getString("property_type")), rs.getString("cover_image"))
                .owner(rs.getInt("owner_id"), rs.getString("owner_name"))
                .created(Db.dateTime(rs, "created_at"), Db.dateTime(rs, "expires_at"))
                .stay(Db.dateTime(rs, "check_in"), Db.dateTime(rs, "check_out"), rs.getInt("number_of_renters"))
                .specialRequests(rs.getString("special_requests"))
                .status(BookingStatus.fromDb(rs.getString("booking_status")))
                .price(rs.getBigDecimal("nightly_rate"), rs.getBigDecimal("cleaning_fee"),
                        rs.getBigDecimal("discount_amount"), rs.getBigDecimal("total_amount"))
                .cancellation(Db.dateTime(rs, "cancelled_at"), rs.getString("cancelled_by"),
                        rs.getString("cancellation_reason"))
                .checkoutConfirmed(Db.dateTime(rs, "checkout_confirmed_at"))
                .payment(rs.getInt("payment_id"), Db.dateTime(rs, "payment_date"))
                .refund(refundId, rs.getString("refund_status"), rs.getString("policy_tier"),
                        rs.getBigDecimal("refund_amount"), rs.getBigDecimal("refund_fee"))
                .build();
    }
}
