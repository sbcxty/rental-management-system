package com.gudosnest.backend.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Run this to check that MySQL is reachable with the settings in config.properties.
 *
 *   .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=com.gudosnest.backend.database.DbConnectionTest"
 */
public final class DbConnectionTest {

    public static void main(String[] args) {
        System.out.println("Gudo's Nest - database check");
        System.out.println("----------------------------");
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            System.out.println("[OK] Connected to " + conn.getMetaData().getURL());
            printCount(conn, "Users");
            printCount(conn, "Properties");
            printCount(conn, "Units");
            printCount(conn, "Bookings");
            System.out.println("[OK] Database is ready.");
        } catch (IllegalStateException e) {
            System.out.println("[FAILED] " + e.getMessage());
            System.exit(1);
        } catch (SQLException e) {
            System.out.println("[FAILED] Could not connect to MySQL.");
            System.out.println("         Reason: " + e.getMessage());
            System.out.println("         Check that the MySQL80 service is running, that db/schema.sql was run,");
            System.out.println("         and that db.url, db.user and db.password in config.properties are correct.");
            System.exit(1);
        } finally {
            DatabaseConnection.getInstance().close();
        }
        // Exit right away; otherwise the MySQL driver's cleanup thread keeps Maven waiting ~15 seconds.
        System.exit(0);
    }

    private static void printCount(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            System.out.printf("     %-11s %d rows%n", table, rs.getInt(1));
        }
    }
}
