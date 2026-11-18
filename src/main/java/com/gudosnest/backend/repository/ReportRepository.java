package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;

import java.util.List;

/**
 * The owner's eight report queries (Week 4). Every query is limited to the signed-in owner's
 * properties through the single parameter {@code p.owner_id = ?}, and each one shows a different
 * SQL feature: WHERE, JOIN, COUNT + GROUP BY, subquery, SUM, HAVING, ORDER BY and AVG.
 */
public class ReportRepository {

    /** One report: the business question, the SQL feature it shows, the text for "no rows", and the SQL. */
    public record Query(int number, String question, String feature, String empty, String sql) {
    }

    /** Shared by the queries that need a booking's renter, unit and property. */
    private static final String BOOKING_JOINS = """
            FROM Bookings b
            JOIN Users r ON r.user_id = b.renter_id
            JOIN Units u ON u.unit_id = b.unit_id
            JOIN Properties p ON p.property_id = u.property_id
            """;

    private static final List<Query> QUERIES = List.of(
            new Query(1, "Which pending bookings expire in the next 24 hours?", "WHERE", "No pending bookings are about to expire.", """
                    SELECT b.booking_id AS `Booking`, r.full_name AS `Renter`, u.unit_name AS `Unit`,
                           b.expires_at AS `Expires at`
                    """ + BOOKING_JOINS + """
                    WHERE p.owner_id = ? AND b.booking_status = 'pending'
                      AND b.expires_at BETWEEN NOW() AND NOW() + INTERVAL 24 HOUR
                    ORDER BY b.expires_at"""),

            new Query(2, "Which renters booked a unit this month?", "JOIN", "No renters have booked this month.", """
                    SELECT DISTINCT r.full_name AS `Renter`, r.email AS `Email`
                    """ + BOOKING_JOINS + """
                    WHERE p.owner_id = ?
                      AND YEAR(b.created_at) = YEAR(CURDATE()) AND MONTH(b.created_at) = MONTH(CURDATE())
                    ORDER BY r.full_name"""),

            new Query(3, "How many bookings has each renter made?", "JOIN + COUNT + GROUP BY", "No bookings yet.", """
                    SELECT r.full_name AS `Renter`, COUNT(*) AS `Bookings`
                    """ + BOOKING_JOINS + """
                    WHERE p.owner_id = ?
                    GROUP BY r.user_id, r.full_name
                    ORDER BY COUNT(*) DESC, r.full_name"""),

            new Query(4, "Which units have never been booked?", "Subquery (NOT EXISTS)", "Every unit has been booked at least once.", """
                    SELECT u.unit_name AS `Unit`, p.property_name AS `Property`, u.daily_rate AS `Daily rate`
                    FROM Units u
                    JOIN Properties p ON p.property_id = u.property_id
                    WHERE p.owner_id = ?
                      AND NOT EXISTS (SELECT 1 FROM Bookings b WHERE b.unit_id = u.unit_id)
                    ORDER BY p.property_name, u.unit_name"""),

            new Query(5, "Which five properties earned the most income?", "JOIN + SUM + GROUP BY", "No income recorded yet.", """
                    SELECT p.property_name AS `Property`, SUM(pay.amount) AS `Income`
                    FROM Payments pay
                    JOIN Bookings b ON b.booking_id = pay.booking_id
                    JOIN Units u ON u.unit_id = b.unit_id
                    JOIN Properties p ON p.property_id = u.property_id
                    WHERE p.owner_id = ? AND pay.payment_status = 'paid'
                    GROUP BY p.property_id, p.property_name
                    ORDER BY SUM(pay.amount) DESC
                    LIMIT 5"""),

            new Query(6, "Which renters made more than five bookings?", "GROUP BY + HAVING", "No renter has more than five bookings yet.", """
                    SELECT r.full_name AS `Renter`, COUNT(*) AS `Bookings`
                    """ + BOOKING_JOINS + """
                    WHERE p.owner_id = ?
                    GROUP BY r.user_id, r.full_name
                    HAVING COUNT(*) > 5
                    ORDER BY COUNT(*) DESC, r.full_name"""),

            new Query(7, "What are the five highest-value bookings?", "ORDER BY + LIMIT", "No bookings yet.", """
                    SELECT b.booking_id AS `Booking`, r.full_name AS `Renter`, u.unit_name AS `Unit`,
                           b.total_amount AS `Total`
                    """ + BOOKING_JOINS + """
                    WHERE p.owner_id = ?
                    ORDER BY b.total_amount DESC, b.booking_id
                    LIMIT 5"""),

            new Query(8, "What is the average booking amount per property type?", "AVG + GROUP BY", "No bookings yet.", """
                    SELECT p.property_type AS `Property type`, COUNT(*) AS `Bookings`,
                           ROUND(AVG(b.total_amount), 2) AS `Average amount`
                    """ + BOOKING_JOINS + """
                    WHERE p.owner_id = ?
                    GROUP BY p.property_type
                    ORDER BY AVG(b.total_amount) DESC"""));

    public List<Query> queries() {
        return QUERIES;
    }

    /** Runs one query for one owner. */
    public Db.Table run(Query query, int ownerId) {
        return Db.table(query.sql(), ownerId);
    }
}
