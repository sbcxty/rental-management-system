package com.gudosnest.backend.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// PATTERN: Singleton
// One shared database connection for the whole desktop app. Every DAO asks this
// class for the connection instead of opening its own.
public final class DatabaseConnection {

    private static DatabaseConnection instance;

    private final AppConfig config;
    private Connection connection;

    private DatabaseConnection() {
        this.config = AppConfig.load();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Returns an open connection, reconnecting if the old one was closed or timed out. */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            connection = DriverManager.getConnection(
                    config.getDbUrl(), config.getDbUser(), config.getDbPassword());
        }
        return connection;
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Closing on exit; nothing useful to do if it fails.
            }
            connection = null;
        }
    }
}
