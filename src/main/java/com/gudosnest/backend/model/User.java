package com.gudosnest.backend.model;

/**
 * A person with an account. One account can be a renter, an owner, or both.
 */
public class User {

    private final int id;
    private final String fullName;
    private final String email;
    private final boolean owner;
    private final boolean renter;

    public User(int id, String fullName, String email, boolean owner, boolean renter) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.owner = owner;
        this.renter = renter;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isOwner() {
        return owner;
    }

    public boolean isRenter() {
        return renter;
    }

    public boolean hasBothRoles() {
        return owner && renter;
    }

    /** "Miguel Alvarez" -> "MA". Used for avatars. */
    public String getInitials() {
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
