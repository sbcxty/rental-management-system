package com.gudosnest.backend.service;

import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.Conversation;
import com.gudosnest.backend.model.Records.Message;
import com.gudosnest.backend.repository.BookingRepository;
import com.gudosnest.backend.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversations between a renter and an owner. A two-role user sees, in each portal, only the
 * conversations that belong to it: as an owner, people who booked their properties; as a
 * renter, owners of places they booked.
 */
public class MessageService {

    public static final int MAX_LENGTH = 1000;

    private final MessageRepository messages = new MessageRepository();
    private final BookingRepository bookings = new BookingRepository();

    public List<Conversation> conversations(int userId, SessionManager.Portal portal) {
        List<Conversation> result = new ArrayList<>();
        for (MessageRepository.Thread t : messages.threads(userId)) {
            Booking booking = t.lastBookingId() == null ? null : bookings.byId(t.lastBookingId()).orElse(null);
            if (booking != null && !belongsTo(booking, userId, portal)) {
                continue;
            }
            result.add(new Conversation(t.otherId(), t.otherName(), t.lastText(), t.lastAt(), t.unread(), booking));
        }
        return result;
    }

    private static boolean belongsTo(Booking booking, int userId, SessionManager.Portal portal) {
        return portal == SessionManager.Portal.OWNER ? booking.getOwnerId() == userId : booking.getRenterId() == userId;
    }

    public boolean hasUnread(int userId, SessionManager.Portal portal) {
        return conversations(userId, portal).stream().anyMatch(c -> c.unread() > 0);
    }

    public List<Message> messages(int userId, int otherId) {
        return messages.between(userId, otherId);
    }

    public void markRead(int userId, int otherId) {
        messages.markRead(userId, otherId);
    }

    public void send(int senderId, int receiverId, Integer bookingId, String text) {
        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            throw new RuleException("Write a message first.");
        }
        if (clean.length() > MAX_LENGTH) {
            throw new RuleException("Messages can be up to " + MAX_LENGTH + " characters.");
        }
        messages.insert(senderId, receiverId, bookingId, clean, LocalDateTime.now().withNano(0));
    }
}
