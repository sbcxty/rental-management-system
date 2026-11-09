package com.gudosnest.backend.service;

import java.time.LocalDateTime;

/**
 * Where each receipt image is saved (relative to the project folder; receipts/ is git-ignored).
 * The database stores these paths; the image itself is drawn from the booking data by the UI,
 * so a missing file can always be redrawn.
 */
public final class ReceiptPaths {

    private ReceiptPaths() {
    }

    public static String payment(int bookingId, LocalDateTime createdAt) {
        return "receipts/GN-" + createdAt.getYear() + "-" + bookingId + ".png";
    }

    public static String refund(int paymentId) {
        return "receipts/refund-" + paymentId + ".png";
    }

    public static String damageFee(int reportId) {
        return "receipts/damage-fee-" + reportId + ".png";
    }
}
