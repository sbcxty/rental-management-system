package com.gudosnest.backend.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Small JDBC helper so repositories only contain SQL and row mapping.
 * Every statement is a PreparedStatement (no string-built SQL), and LocalDate/LocalDateTime
 * parameters are converted for MySQL here in one place.
 */
public final class Db {

    /** Turns one result row into an object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Work that must succeed or fail as a whole (see {@link #transaction}). */
    @FunctionalInterface
    public interface Work<T> {
        T run() throws SQLException;
    }

    private Db() {
    }

    private static Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public static <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = prepare(sql, false, params); ResultSet rs = ps.executeQuery()) {
            List<T> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapper.map(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new DataException("Could not load data from the database.", e);
        }
    }

    public static <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = list(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** A single number, for example a COUNT(*) or SUM(...). Null sums come back as zero. */
    public static BigDecimal decimal(String sql, Object... params) {
        return one(sql, rs -> rs.getBigDecimal(1), params).filter(v -> v != null).orElse(BigDecimal.ZERO);
    }

    public static int count(String sql, Object... params) {
        return decimal(sql, params).intValue();
    }

    /** INSERT/UPDATE/DELETE. Returns the number of changed rows. */
    public static int update(String sql, Object... params) {
        try (PreparedStatement ps = prepare(sql, false, params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not save the change to the database.", e);
        }
    }

    /** INSERT that returns the new AUTO_INCREMENT id. */
    public static int insert(String sql, Object... params) {
        try (PreparedStatement ps = prepare(sql, true, params)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataException("Could not save the new record to the database.", e);
        }
    }

    /**
     * Runs several statements as one unit: all are saved, or (on any error) none are.
     * Example: a booking, its payment and its receipt path are written together.
     */
    public static synchronized <T> T transaction(Work<T> work) {
        Connection c;
        try {
            c = connection();
            c.setAutoCommit(false);
        } catch (SQLException e) {
            throw new DataException("Could not connect to the database.", e);
        }
        try {
            T result = work.run();
            c.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // the original error is the useful one
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new DataException("Could not save the change to the database.", e);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignored) {
                // connection is re-validated on next use
            }
        }
    }

    private static PreparedStatement prepare(String sql, boolean keys, Object... params) throws SQLException {
        PreparedStatement ps = keys
                ? connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : connection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, convert(params[i]));
        }
        return ps;
    }

    private static Object convert(Object value) {
        if (value instanceof LocalDateTime dt) {
            return Timestamp.valueOf(dt);
        }
        if (value instanceof LocalDate d) {
            return java.sql.Date.valueOf(d);
        }
        if (value instanceof Enum<?> e) {
            return e.name().toLowerCase();
        }
        return value;
    }

    // Null-safe readers used by the row mappers

    public static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        java.sql.Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    public static Integer integer(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
