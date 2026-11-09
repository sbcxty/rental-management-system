package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Properties with their photos and units. */
public class PropertyRepository {

    private static final String SELECT = """
            SELECT p.*, o.full_name AS owner_name
            FROM Properties p JOIN Users o ON o.user_id = p.owner_id
            """;

    public List<Property> forOwner(int ownerId) {
        return load(SELECT + " WHERE p.owner_id = ? ORDER BY p.property_name", ownerId);
    }

    /** Every listing that has at least one unit, for renters to browse. */
    public List<Property> bookable() {
        return load(SELECT + " WHERE EXISTS (SELECT 1 FROM Units u WHERE u.property_id = p.property_id)"
                + " ORDER BY p.property_name");
    }

    public Optional<Property> byId(int propertyId) {
        List<Property> found = load(SELECT + " WHERE p.property_id = ?", propertyId);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    /** Loads the properties, then all of their photos and units in two more queries (not one per property). */
    private List<Property> load(String sql, Object... params) {
        record Row(int id, int ownerId, String ownerName, String name, String location, Property.Type type,
                   BigDecimal cost, LocalDate date) {
        }
        List<Row> rows = Db.list(sql, rs -> new Row(rs.getInt("property_id"), rs.getInt("owner_id"),
                rs.getString("owner_name"), rs.getString("property_name"), rs.getString("location"),
                Property.Type.fromDb(rs.getString("property_type")), rs.getBigDecimal("acquisition_cost"),
                Db.date(rs, "acquisition_date")), params);
        if (rows.isEmpty()) {
            return List.of();
        }
        String ids = String.join(",", rows.stream().map(r -> String.valueOf(r.id())).toList());

        Map<Integer, List<String>> images = new HashMap<>();
        Db.list("SELECT property_id, image_path FROM PropertyImages WHERE property_id IN (" + ids + ")"
                        + " ORDER BY sort_order, image_id",
                rs -> images.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(rs.getString(2)));

        Map<Integer, List<Unit>> units = new HashMap<>();
        Db.list("SELECT * FROM Units WHERE property_id IN (" + ids + ") ORDER BY unit_id",
                rs -> units.computeIfAbsent(rs.getInt("property_id"), k -> new ArrayList<>()).add(mapUnit(rs)));

        List<Property> result = new ArrayList<>();
        for (Row r : rows) {
            result.add(Property.builder()
                    .id(r.id())
                    .owner(r.ownerId(), r.ownerName())
                    .name(r.name())
                    .location(r.location())
                    .type(r.type())
                    .acquisition(r.cost(), r.date())
                    .images(images.getOrDefault(r.id(), List.of()))
                    .units(units.getOrDefault(r.id(), List.of()))
                    .build());
        }
        return result;
    }

    static Unit mapUnit(ResultSet rs) throws SQLException {
        return new Unit(rs.getInt("unit_id"), rs.getInt("property_id"), rs.getString("unit_name"),
                rs.getString("unit_description"), rs.getInt("capacity"), rs.getBigDecimal("daily_rate"),
                rs.getBigDecimal("cleaning_fee"), rs.getString("unit_status").equals("occupied"));
    }

    // ------------------------------------------------------------------------------- writes

    public int insertProperty(int ownerId, String name, String location, Property.Type type,
                              BigDecimal cost, LocalDate acquired) {
        return Db.insert("INSERT INTO Properties (owner_id, property_name, location, property_type, acquisition_cost,"
                + " acquisition_date) VALUES (?, ?, ?, ?, ?, ?)", ownerId, name, location, type, cost, acquired);
    }

    public void updateProperty(int propertyId, String name, String location, BigDecimal cost, LocalDate acquired) {
        Db.update("UPDATE Properties SET property_name = ?, location = ?, acquisition_cost = ?, acquisition_date = ?"
                + " WHERE property_id = ?", name, location, cost, acquired, propertyId);
    }

    public void replaceImages(int propertyId, List<String> paths) {
        Db.update("DELETE FROM PropertyImages WHERE property_id = ?", propertyId);
        for (int i = 0; i < paths.size(); i++) {
            Db.update("INSERT INTO PropertyImages (property_id, image_path, sort_order) VALUES (?, ?, ?)",
                    propertyId, paths.get(i), i);
        }
    }

    public int insertUnit(Unit unit) {
        return Db.insert("INSERT INTO Units (property_id, unit_name, unit_description, capacity, daily_rate, cleaning_fee)"
                        + " VALUES (?, ?, ?, ?, ?, ?)", unit.getPropertyId(), unit.getName(), unit.getDescription(),
                unit.getCapacity(), unit.getDailyRate(), unit.getCleaningFee());
    }

    public void updateUnit(Unit unit) {
        Db.update("UPDATE Units SET unit_name = ?, unit_description = ?, capacity = ?, daily_rate = ?, cleaning_fee = ?"
                        + " WHERE unit_id = ?", unit.getName(), unit.getDescription(), unit.getCapacity(),
                unit.getDailyRate(), unit.getCleaningFee(), unit.getId());
    }

    public boolean unitHasBookings(int unitId) {
        return Db.count("SELECT COUNT(*) FROM Bookings WHERE unit_id = ?", unitId) > 0;
    }

    public void deleteUnit(int unitId) {
        Db.update("DELETE FROM Items WHERE unit_id = ?", unitId);
        Db.update("DELETE FROM Units WHERE unit_id = ?", unitId);
    }

    /** All distinct locations and property names, for the Browse search suggestions. */
    public List<String> searchWords() {
        return Db.list("SELECT location FROM Properties UNION SELECT property_name FROM Properties",
                rs -> rs.getString(1));
    }
}
