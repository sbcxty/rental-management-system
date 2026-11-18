package com.gudosnest.backend.database;

import com.gudosnest.backend.repository.ReportRepository;

import java.util.List;

/**
 * Runs the eight report queries for one owner and prints each result (good for a screenshot).
 * The owner id defaults to 1, the demo owner in db/seed.sql; pass another id as the argument.
 *
 *   .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=com.gudosnest.backend.database.ReportQueryTest"
 */
public final class ReportQueryTest {

    public static void main(String[] args) {
        int ownerId = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        ReportRepository reports = new ReportRepository();
        int failed = 0;
        try {
            for (ReportRepository.Query q : reports.queries()) {
                System.out.printf("%nQ%d. %s  [%s]%n", q.number(), q.question(), q.feature());
                try {
                    print(reports.run(q, ownerId));
                } catch (DataException e) {
                    failed++;
                    System.out.println("   [FAILED] " + e.getMessage() + " (" + e.getCause().getMessage() + ")");
                }
            }
        } finally {
            DatabaseConnection.getInstance().close();
        }
        System.out.println(failed == 0 ? "\n[OK] All queries ran." : "\n[FAILED] " + failed + " queries failed.");
        // Exit right away; otherwise the MySQL driver's cleanup thread keeps Maven waiting ~15 seconds.
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void print(Db.Table table) {
        System.out.println("   " + String.join(" | ", table.columns()));
        if (table.rows().isEmpty()) {
            System.out.println("   (no rows)");
        }
        for (List<Object> row : table.rows()) {
            System.out.println("   " + String.join(" | ", row.stream().map(v -> String.valueOf(v)).toList()));
        }
    }
}
