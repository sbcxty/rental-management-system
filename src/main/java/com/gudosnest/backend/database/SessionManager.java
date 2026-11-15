package com.gudosnest.backend.database;

import com.gudosnest.backend.model.User;

// PATTERN: Singleton
// Remembers who is logged in and which portal they are using. MainWindow writes it at
// log in, portal switch and log out; Portal.create() reads it to open the right portal.
public final class SessionManager {

    public enum Portal { RENTER, OWNER }

    private static SessionManager instance;

    private User currentUser;
    private Portal currentPortal;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void logIn(User user, Portal portal) {
        this.currentUser = user;
        this.currentPortal = portal;
    }

    public void logOut() {
        this.currentUser = null;
        this.currentPortal = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Portal getCurrentPortal() {
        return currentPortal;
    }
}
