package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("email"),
                rs.getBoolean("is_owner"), rs.getBoolean("is_renter"));
    }
}
