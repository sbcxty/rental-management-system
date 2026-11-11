package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Records.Message;

import java.time.LocalDateTime;
import java.util.List;

/** Messages between a renter and an owner. A conversation is everything between one pair of people. */
public class MessageRepository {

    /**
     * Summary per other person: id, name, roles, last text, last time, unread count, latest booking id
     * (null when they've never messaged about a booking).
     */
    public record Thread(int otherId, String otherName, boolean otherIsOwner, boolean otherIsRenter,
                         String lastText, LocalDateTime lastAt, int unread, Integer lastBookingId) {
    }

    public List<Thread> threads(int userId) {
        return Db.list("""
                SELECT other.user_id, other.full_name, other.is_owner, other.is_renter,
                       (SELECT m2.message_text FROM Messages m2
                        WHERE (m2.sender_id = ? AND m2.receiver_id = other.user_id)
                           OR (m2.sender_id = other.user_id AND m2.receiver_id = ?)
                        ORDER BY m2.sent_at DESC, m2.message_id DESC LIMIT 1) AS last_text,
                       MAX(m.sent_at) AS last_at,
                       SUM(CASE WHEN m.receiver_id = ? AND m.is_read = FALSE THEN 1 ELSE 0 END) AS unread,
                       (SELECT m3.booking_id FROM Messages m3
                        WHERE ((m3.sender_id = ? AND m3.receiver_id = other.user_id)
                           OR (m3.sender_id = other.user_id AND m3.receiver_id = ?)) AND m3.booking_id IS NOT NULL
                        ORDER BY m3.sent_at DESC LIMIT 1) AS last_booking
                FROM Messages m
                JOIN Users other ON other.user_id = CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END
                WHERE m.sender_id = ? OR m.receiver_id = ?
                GROUP BY other.user_id, other.full_name, other.is_owner, other.is_renter
                ORDER BY last_at DESC""",
                rs -> new Thread(rs.getInt("user_id"), rs.getString("full_name"), rs.getBoolean("is_owner"),
                        rs.getBoolean("is_renter"), rs.getString("last_text"), Db.dateTime(rs, "last_at"),
                        rs.getInt("unread"), Db.integer(rs, "last_booking")),
                userId, userId, userId, userId, userId, userId, userId, userId);
    }

    public List<Message> between(int userId, int otherId) {
        return Db.list("SELECT * FROM Messages WHERE (sender_id = ? AND receiver_id = ?)"
                        + " OR (sender_id = ? AND receiver_id = ?) ORDER BY sent_at, message_id",
                rs -> new Message(rs.getInt("message_id"), rs.getInt("sender_id"), rs.getInt("receiver_id"),
                        Db.integer(rs, "booking_id"), rs.getString("message_text"), Db.dateTime(rs, "sent_at"),
                        rs.getBoolean("is_read")), userId, otherId, otherId, userId);
    }

    public void insert(int senderId, int receiverId, Integer bookingId, String text, LocalDateTime at) {
        Db.update("INSERT INTO Messages (sender_id, receiver_id, booking_id, message_text, sent_at) VALUES (?, ?, ?, ?, ?)",
                senderId, receiverId, bookingId, text, at);
    }

    public void markRead(int userId, int otherId) {
        Db.update("UPDATE Messages SET is_read = TRUE WHERE receiver_id = ? AND sender_id = ? AND is_read = FALSE",
                userId, otherId);
    }

    public int unreadCount(int userId) {
        return Db.count("SELECT COUNT(*) FROM Messages WHERE receiver_id = ? AND is_read = FALSE", userId);
    }
}
