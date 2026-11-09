package com.gudosnest;

import com.gudosnest.backend.database.DatabaseConnection;
import com.gudosnest.frontend.MainWindow;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.theme.Theme;

import javax.swing.SwingUtilities;

/** Starts Gudo's Nest: theme, database check, then the login screen. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                DatabaseConnection.getInstance().getConnection();
            } catch (Exception e) {
                AppDialog.error(null, "Gudo's Nest can't reach the database.\n\n" + rootMessage(e)
                        + "\n\nCheck that MySQL is running, that db/schema.sql has been run, and that"
                        + "\nconfig.properties (in the project folder) has the right user and password.");
                System.exit(1);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> DatabaseConnection.getInstance().close()));
            new MainWindow().setVisible(true);
        });
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
