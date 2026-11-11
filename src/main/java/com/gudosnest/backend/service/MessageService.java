package com.gudosnest.backend.service;

import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.Contact;
import com.gudosnest.backend.model.Records.Conversation;
import com.gudosnest.backend.model.Records.Message;
import com.gudosnest.backend.repository.BookingRepository;
import com.gudosnest.backend.repository.MessageRepository;
import com.gudosnest.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conversations between a renter and an owner. A conversation can start from a booking or from
 * the Messages search (no booking needed).
 *
 * A two-role user sees, in each portal, only the conversations that belong to it. With a booking,
 * the booking decides: as an owner, people who booked their properties; as a renter, owners of
 * places they booked. Without one, the other person's role decides: owners appear in the Renter
 * portal, renters in the Owner portal (someone with both roles appears in both).
 */
public class MessageService {

    public static final int MAX_LENGTH = 1000;
    public static final int SEARCH_LIMIT = 8;

    private final MessageRepository messages = new MessageRepository();
    private final BookingRepository bookings = new BookingRepository();
    private final UserRepository users = new UserRepository();

    public List<Conversation> conversations(int userId, SessionManager.Portal portal) {
        List<Conversation> result = new ArrayList<>();
        for (MessageRepository.Thread t : messages.threads(userId)) {
            // The booking the messages mention decides which portal the conversation belongs to...
            Booking mentioned = t.lastBookingId() == null ? null : bookings.byId(t.lastBookingId()).orElse(null);
            boolean belongs = mentioned != null
                    ? belongsTo(mentioned, userId, portal)
                    : (portal == SessionManager.Portal.OWNER ? t.otherIsRenter() : t.otherIsOwner());
            if (belongs) {
                // ...but the page always shows their latest booking (older ones are in booking history)
                Booking latest = latestBooking(userId, t.otherId(), portal).orElse(mentioned);
                result.add(new Conversation(t.otherId(), t.otherName(), t.lastText(), t.lastAt(), t.unread(), latest));
            }
        }
        return result;
    }

    private static boolean belongsTo(Booking booking, int userId, SessionManager.Portal portal) {
        return portal == SessionManager.Portal.OWNER ? booking.getOwnerId() == userId : booking.getRenterId() == userId;
    }

    /**
     * The most recent booking between the two people, seen from this portal: in the Owner portal
     * the other person is the renter; in the Renter portal they are the owner.
     */
    public Optional<Booking> latestBooking(int userId, int otherId, SessionManager.Portal portal) {
        return portal == SessionManager.Portal.OWNER
                ? bookings.latestBetween(otherId, userId)
                : bookings.latestBetween(userId, otherId);
    }

    public boolean hasUnread(int userId, SessionManager.Portal portal) {
        return conversations(userId, portal).stream().anyMatch(c -> c.unread() > 0);
    }

    /**
     * People to start a conversation with: in the Renter portal, owners whose name or property
     * name matches; in the Owner portal, renters whose name matches. Blank text finds no one.
     */
    public List<Contact> findPeople(int userId, SessionManager.Portal portal, String text) {
        String query = text == null ? "" : text.strip();
        if (query.isEmpty()) {
            return List.of();
        }
        return users.searchContacts(query, portal == SessionManager.Portal.RENTER, userId, SEARCH_LIMIT);
    }

    public List<Message> messages(int userId, int otherId) {
        return messages.between(userId, otherId);
    }

    public void markRead(int userId, int otherId) {
        messages.markRead(userId, otherId);
    }

    /** Sends a message; bookingId may be null (a conversation started from the search). */
    public void send(int senderId, int receiverId, Integer bookingId, String text) {
        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            throw new RuleException("Write a message first.");
        }
        if (clean.length() > MAX_LENGTH) {
            throw new RuleException("Messages can be up to " + MAX_LENGTH + " characters.");
        }
        if (senderId == receiverId) {
            throw new RuleException("You can't message yourself.");
        }
        if (users.findById(receiverId).isEmpty()) {
            throw new RuleException("This person's account no longer exists.");
        }
        messages.insert(senderId, receiverId, bookingId, clean, LocalDateTime.now().withNano(0));
    }
}
