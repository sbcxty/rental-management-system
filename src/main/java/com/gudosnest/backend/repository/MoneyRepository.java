package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.PaymentRow;
import com.gudosnest.backend.model.Records.RefundRow;
import com.gudosnest.backend.model.Records.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Payments, refunds and the finance ledger (FinancialTransactions). */
public class MoneyRepository {

    // ------------------------------------------------------------------------------ payments

    public int insertPaidPayment(int bookingId, BigDecimal amount, String type, String receiptPath, LocalDateTime at) {
        return Db.insert("INSERT INTO Payments (booking_id, amount, receipt_url, payment_type, payment_status, payment_date)"
                + " VALUES (?, ?, ?, ?, 'paid', ?)", bookingId, amount, receiptPath, type, at);
    }

    public void setPaymentReceipt(int paymentId, String receiptPath) {
        Db.update("UPDATE Payments SET receipt_url = ? WHERE payment_id = ?", receiptPath, paymentId);
    }

    /** Everything paid in or out on the owner's properties, newest first. */
    public List<PaymentRow> paymentRows(int ownerId) {
        String joins = """
                FROM Payments pay
                JOIN Bookings b ON b.booking_id = pay.booking_id
                JOIN Users r ON r.user_id = b.renter_id
                JOIN Units u ON u.unit_id = b.unit_id
                JOIN Properties p ON p.property_id = u.property_id
                """;
        String sql = "SELECT pay.payment_id, b.booking_id, b.created_at, r.full_name, p.property_name, p.property_type,"
                + " u.unit_name, CASE pay.payment_type WHEN 'rental_payment' THEN 'Booking payment' ELSE 'Damage fee' END AS kind,"
                + " pay.amount, pay.payment_date AS dt, 'Paid' AS status, NULL AS refund_id "
                + joins + " WHERE p.owner_id = ? AND pay.payment_status = 'paid'"
                + " UNION ALL "
                + "SELECT pay.payment_id, b.booking_id, b.created_at, r.full_name, p.property_name, p.property_type,"
                + " u.unit_name, 'Refund', rf.refund_amount, COALESCE(rf.processed_at, rf.reviewed_at, rf.requested_at),"
                + " CASE rf.refund_status WHEN 'processed' THEN 'Refunded' WHEN 'requested' THEN 'Requested' ELSE 'Rejected' END,"
                + " rf.refund_id "
                + joins + " JOIN Refunds rf ON rf.payment_id = pay.payment_id WHERE p.owner_id = ?"
                + " ORDER BY dt DESC";
        return Db.list(sql, rs -> new PaymentRow(rs.getInt(1), rs.getInt(2), Db.dateTime(rs, "created_at"),
                rs.getString(4), rs.getString(5),
                rs.getString(6).equals("house") ? Property.Type.HOUSE.label() : rs.getString(7),
                rs.getString(8), rs.getBigDecimal(9), Db.dateTime(rs, "dt"), rs.getString(11),
                Db.integer(rs, "refund_id")), ownerId, ownerId);
    }

    /** A damage-fee payment (for its receipt). */
    public Optional<PaymentRow> damagePayment(int paymentId) {
        return Db.one("SELECT pay.payment_id, b.booking_id, b.created_at, r.full_name, p.property_name, pay.amount,"
                + " pay.payment_date FROM Payments pay JOIN Bookings b ON b.booking_id = pay.booking_id"
                + " JOIN Users r ON r.user_id = b.renter_id JOIN Units u ON u.unit_id = b.unit_id"
                + " JOIN Properties p ON p.property_id = u.property_id WHERE pay.payment_id = ?",
                rs -> new PaymentRow(rs.getInt(1), rs.getInt(2), Db.dateTime(rs, "created_at"), rs.getString(4),
                        rs.getString(5), "", "Damage fee", rs.getBigDecimal(6), Db.dateTime(rs, "payment_date"),
                        "Paid", null), paymentId);
    }

    // ------------------------------------------------------------------------------- refunds

    public int insertRefund(int paymentId, String tier, BigDecimal fee, BigDecimal amount, String status,
                            LocalDateTime requestedAt, LocalDateTime processedAt, String receiptPath) {
        return Db.insert("INSERT INTO Refunds (payment_id, policy_tier, cancellation_fee, refund_amount, refund_status,"
                        + " requested_at, reviewed_at, processed_at, refund_receipt_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                paymentId, tier, fee, amount, status, requestedAt, processedAt, processedAt, receiptPath);
    }

    public void markRefundProcessed(int refundId, LocalDateTime at, String receiptPath) {
        Db.update("UPDATE Refunds SET refund_status = 'processed', reviewed_at = ?, processed_at = ?,"
                + " refund_receipt_url = ? WHERE refund_id = ?", at, at, receiptPath, refundId);
    }

    public void markRefundRejected(int refundId, LocalDateTime at) {
        Db.update("UPDATE Refunds SET refund_status = 'rejected', reviewed_at = ? WHERE refund_id = ?", at, refundId);
    }

    /** Refund requests waiting for this owner's review, oldest first. */
    public List<RefundRow> requestedRefunds(int ownerId) {
        return Db.list(BookingRepository.SELECT + " WHERE p.owner_id = ? AND rf.refund_status = 'requested'"
                + " ORDER BY rf.requested_at", MoneyRepository::mapRefund, ownerId);
    }

    public Optional<RefundRow> refundById(int refundId) {
        return Db.one(BookingRepository.SELECT + " WHERE rf.refund_id = ?", MoneyRepository::mapRefund, refundId);
    }

    private static RefundRow mapRefund(java.sql.ResultSet rs) throws java.sql.SQLException {
        Booking booking = BookingRepository.map(rs);
        LocalDateTime requested = Db.dateTime(rs, "refund_requested_at");
        LocalDateTime processed = Db.dateTime(rs, "refund_processed_at");
        return new RefundRow(booking.getRefundId(), booking.getPaymentId(), booking, booking.getRefundTier(),
                booking.getCancellationFee(), booking.getRefundAmount(), booking.getRefundStatus(), requested, processed);
    }

    // ------------------------------------------------------------------------------ ledger

    public void insertTransaction(int propertyId, Integer paymentId, String type, BigDecimal amount,
                                  LocalDateTime at, String note) {
        Db.update("INSERT INTO FinancialTransactions (property_id, payment_id, transaction_type, amount,"
                + " transaction_date, note) VALUES (?, ?, ?, ?, ?, ?)", propertyId, paymentId, type, amount, at, note);
    }

    /** Ledger lines of the owner's properties (one property if propertyId is not null), newest first. */
    public List<Transaction> transactions(int ownerId, Integer propertyId) {
        String sql = "SELECT t.*, p.property_name FROM FinancialTransactions t"
                + " JOIN Properties p ON p.property_id = t.property_id WHERE p.owner_id = ?"
                + (propertyId == null ? "" : " AND p.property_id = " + propertyId.intValue())
                + " ORDER BY t.transaction_date DESC, t.transaction_id DESC";
        return Db.list(sql, rs -> new Transaction(rs.getInt("transaction_id"), rs.getInt("property_id"),
                rs.getString("property_name"), rs.getString("transaction_type"), rs.getBigDecimal("amount"),
                Db.dateTime(rs, "transaction_date"), rs.getString("note")), ownerId);
    }
}
