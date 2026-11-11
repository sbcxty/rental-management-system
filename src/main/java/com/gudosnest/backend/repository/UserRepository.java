package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.User;

import com.gudosnest.backend.model.Records.Contact;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    /** A user plus the stored password hash, used only while logging in. */
    public record Credentials(User user, String passwordHash, boolean active) {
    }

    public Optional<Credentials> findCredentials(String email) {
        return Db.one("SELECT * FROM Users WHERE email = ?",
                rs -> new Credentials(map(rs), rs.getString("password_hash"),
                        rs.getString("account_status").equals("active")), email);
    }

    public boolean emailExists(String email) {
        return Db.count("SELECT COUNT(*) FROM Users WHERE email = ?", email) > 0;
    }

    public User insert(String fullName, String email, String passwordHash, boolean owner, boolean renter) {
        int id = Db.insert("INSERT INTO Users (full_name, email, password_hash, is_owner, is_renter) VALUES (?, ?, ?, ?, ?)",
                fullName, email, passwordHash, owner, renter);
        return new User(id, fullName, email, owner, renter);
    }

    public Optional<User> findById(int id) {
        return Db.one("SELECT * FROM Users WHERE user_id = ?", UserRepository::map, id);
    }

    /**
     * Active owners (matched by name or by one of their property names) or active renters (matched
     * by name), never {@code excludeId} itself. Used by the Messages search.
     */
    public List<Contact> searchContacts(String query, boolean owners, int excludeId, int limit) {
        // Escape LIKE wildcards so typing "%" or "_" searches for those characters
        String like = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String match = owners
                ? "u.is_owner = TRUE AND (u.full_name LIKE ? OR p.property_name LIKE ?)"
                : "u.is_renter = TRUE AND u.full_name LIKE ?";
        Object[] params = owners
                ? new Object[]{excludeId, like, like, limit}
                : new Object[]{excludeId, like, limit};
        String sql = "SELECT u.user_id, u.full_name,"
                + " GROUP_CONCAT(DISTINCT p.property_name ORDER BY p.property_name SEPARATOR ', ') AS properties"
                + " FROM Users u LEFT JOIN Properties p ON p.owner_id = u.user_id"
                + " WHERE u.account_status = 'active' AND u.user_id <> ? AND " + match
                + " GROUP BY u.user_id, u.full_name ORDER BY u.full_name LIMIT ?";
        return Db.list(sql, rs -> new Contact(rs.getInt("user_id"), rs.getString("full_name"),
                owners ? rs.getString("properties") : null), params);
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("email"),
                rs.getBoolean("is_owner"), rs.getBoolean("is_renter"));
    }
}
