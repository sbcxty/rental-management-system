package com.gudosnest.backend.repository;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Item;
import com.gudosnest.backend.model.Records.DamageReport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Inventory items and damage reports: the owner's day-to-day records. */
public class OwnerOpsRepository {

    // ------------------------------------------------------------------------------ items

    /** Items of one property: every unit's items plus shared ones (unit_id NULL). */
    public List<Item> itemsForProperty(int propertyId) {
        return Db.list("SELECT * FROM Items WHERE property_id = ? ORDER BY item_name",
                rs -> new Item(rs.getInt("item_id"), rs.getInt("property_id"), Db.integer(rs, "unit_id"),
                        rs.getString("item_name"), Item.Type.valueOf(rs.getString("item_type").toUpperCase()),
                        rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                        Item.Condition.valueOf(rs.getString("item_condition").toUpperCase()),
                        Db.dateTime(rs, "updated_at")), propertyId);
    }

    public void insertItem(Item item) {
        Db.update("INSERT INTO Items (property_id, unit_id, item_name, item_type, quantity, unit_price, item_condition)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)", item.getPropertyId(), item.getUnitId(), item.getName(),
                item.getType(), item.getQuantity(), item.getUnitPrice(), item.getCondition());
    }

    public void updateItem(int itemId, String name, Item.Type type, int quantity, BigDecimal price, Item.Condition condition) {
        Db.update("UPDATE Items SET item_name = ?, item_type = ?, quantity = ?, unit_price = ?, item_condition = ?"
                + " WHERE item_id = ?", name, type, quantity, price, condition, itemId);
    }

    public void setQuantity(int itemId, int quantity) {
        Db.update("UPDATE Items SET quantity = ? WHERE item_id = ?", quantity, itemId);
    }

    public void setCondition(int itemId, Item.Condition condition) {
        Db.update("UPDATE Items SET item_condition = ? WHERE item_id = ?", condition, itemId);
    }

    public void deleteItem(int itemId) {
        Db.update("DELETE FROM Items WHERE item_id = ?", itemId);
    }

    // ---------------------------------------------------------------------- damage reports

    public List<DamageReport> damageReports(int ownerId) {
        return Db.list(BookingRepository.SELECT.replace("FROM Bookings b",
                        ", d.report_id, d.description, d.photo_path, d.requested_fee, d.report_status,"
                                + " d.created_at AS report_created, d.resolved_at FROM DamageReports d"
                                + " JOIN Bookings b ON b.booking_id = d.booking_id")
                        + " WHERE p.owner_id = ? ORDER BY d.created_at DESC",
                rs -> new DamageReport(rs.getInt("report_id"), BookingRepository.map(rs), rs.getString("description"),
                        rs.getString("photo_path"), rs.getBigDecimal("requested_fee"), rs.getString("report_status"),
                        Db.dateTime(rs, "report_created"), Db.dateTime(rs, "resolved_at")), ownerId);
    }

    public Optional<DamageReport> damageReport(int reportId, int ownerId) {
        return damageReports(ownerId).stream().filter(r -> r.id() == reportId).findFirst();
    }

    public void insertDamageReport(int bookingId, String description, String photoPath, BigDecimal fee, LocalDateTime at) {
        Db.update("INSERT INTO DamageReports (booking_id, description, photo_path, requested_fee, created_at)"
                + " VALUES (?, ?, ?, ?, ?)", bookingId, description, photoPath, fee, at);
    }

    public void chargeDamageReport(int reportId, int paymentId, LocalDateTime at) {
        Db.update("UPDATE DamageReports SET report_status = 'charged', payment_id = ?, resolved_at = ?"
                + " WHERE report_id = ?", paymentId, at, reportId);
    }

    public void dismissDamageReport(int reportId, LocalDateTime at) {
        Db.update("UPDATE DamageReports SET report_status = 'dismissed', resolved_at = ? WHERE report_id = ?", at, reportId);
    }
}
