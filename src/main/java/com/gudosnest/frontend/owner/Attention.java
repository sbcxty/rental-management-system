package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.BookingStatus;
import com.gudosnest.backend.model.Records.RefundRow;
import com.gudosnest.backend.structures.AttentionQueue;
import com.gudosnest.frontend.portal.Portal;

import java.util.List;

/**
 * What the owner needs to act on: pending bookings (approve or reject before the 48 hours run out)
 * and refund requests (approve or reject). Kept in an AttentionQueue so the most urgent is first.
 */
final class Attention {

    /** One item: exactly one of booking/refund is set. */
    record Item(Booking booking, RefundRow refund) {
        boolean isRefund() {
            return refund != null;
        }

        Booking subject() {
            return isRefund() ? refund.booking() : booking;
        }
    }

    final List<Item> items;
    final int pendingCount;
    final int refundCount;

    private Attention(List<Item> items, int pendingCount, int refundCount) {
        this.items = items;
        this.pendingCount = pendingCount;
        this.refundCount = refundCount;
    }

    /** Loads the owner's items, optionally for one property only (propertyId null = all). */
    static Attention load(Portal portal, List<Booking> ownerBookings, Integer propertyId) {
        AttentionQueue<Item> queue = new AttentionQueue<>();
        int pending = 0;
        int refunds = 0;
        for (Booking b : ownerBookings) {
            if (b.getStatus() == BookingStatus.PENDING && matches(b, propertyId)) {
                queue.add(b.getExpiresAt(), new Item(b, null));   // due when the 48 hours end
                pending++;
            }
        }
        for (RefundRow r : portal.owner.refundRequests(portal.user().getId())) {
            if (matches(r.booking(), propertyId)) {
                // Same 48-hour courtesy window for reviewing refunds, so both kinds compare fairly
                queue.add(r.requestedAt().plusHours(48), new Item(null, r));
                refunds++;
            }
        }
        return new Attention(queue.inOrder(), pending, refunds);
    }

    private static boolean matches(Booking b, Integer propertyId) {
        return propertyId == null || b.getPropertyId() == propertyId;
    }

    int total() {
        return items.size();
    }
}
