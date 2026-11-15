package com.gudosnest.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Small read-only rows shown in lists. Each is a Java record: the fields are the data,
 * nothing is changed after loading.
 */
public final class Records {

    private Records() {
    }

    /** A money movement shown on the owner's Payments page (a rental payment, damage fee or refund). */
    public record PaymentRow(int paymentId, int bookingId, LocalDateTime bookingCreatedAt, String renterName,
                             String propertyName, String unitLabel, String kind, BigDecimal amount,
                             LocalDateTime date, String status, Integer refundId) {
        /** True for money going back to the renter. */
        public boolean isRefund() {
            return "Refund".equals(kind);
        }
    }

    /** A refund with the booking details the owner needs to review it. */
    public record RefundRow(int refundId, int paymentId, Booking booking, String tier,
                            BigDecimal cancellationFee, BigDecimal amount, String status,
                            LocalDateTime requestedAt, LocalDateTime processedAt) {
    }

    /** One line of the finances ledger. */
    public record Transaction(int id, int propertyId, String propertyName, String type, BigDecimal amount,
                              LocalDateTime date, String note) {
        public boolean isIncome() {
            return type.equals("rental_income") || type.equals("damage_fee_income");
        }

        public boolean isExpense() {
            return type.endsWith("_expense");
        }

        public String label() {
            return switch (type) {
                case "rental_income" -> "Rental income";
                case "damage_fee_income" -> "Damage fee";
                case "refund" -> "Refund";
                case "maintenance_expense" -> "Maintenance";
                case "supply_expense" -> "Supplies";
                case "utility_expense" -> "Utilities";
                default -> type;
            };
        }
    }

    /**
     * A damage report filed by an owner against a booking. The renter pays it; "charged" in the
     * database means paid. An unpaid report is overdue {@link #PAY_WITHIN_HOURS} hours after filing.
     */
    public record DamageReport(int id, Booking booking, String description, String photoPath,
                               BigDecimal requestedFee, String status, LocalDateTime createdAt,
                               LocalDateTime resolvedAt) {

        /** Hours the renter has to pay before their account can't book. */
        public static final int PAY_WITHIN_HOURS = 48;

        public boolean isOpen() {
            return "open".equals(status);
        }

        public boolean isPaid() {
            return "charged".equals(status);
        }

        /** When the renter must pay by. */
        public LocalDateTime dueAt() {
            return createdAt.plusHours(PAY_WITHIN_HOURS);
        }

        /** Still unpaid after the 48 hours. */
        public boolean isOverdue(LocalDateTime now) {
            return isOpen() && now.isAfter(dueAt());
        }

        public String statusLabel() {
            return switch (status) {
                case "open" -> isOverdue(LocalDateTime.now()) ? "Overdue" : "Awaiting payment";
                case "charged" -> "Paid";
                default -> "Dismissed";
            };
        }
    }

    /** One chat message. */
    public record Message(int id, int senderId, int receiverId, Integer bookingId, String text,
                          LocalDateTime sentAt, boolean read) {
    }

    /** One renter-owner conversation as listed on the left of the Messages page. */
    public record Conversation(int otherUserId, String otherName, String lastText, LocalDateTime lastAt,
                               int unread, Booking booking) {
    }

    /**
     * Someone found by the Messages search to start a new conversation with.
     * {@code properties} lists an owner's property names ("Highland Glass Cabin, Sanctuary Cove House"),
     * or is null for renters and owners without properties.
     */
    public record Contact(int userId, String name, String properties) {
    }
}
