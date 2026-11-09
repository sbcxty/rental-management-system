package com.gudosnest.backend.service;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Item;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.model.Records.PaymentRow;
import com.gudosnest.backend.model.Records.RefundRow;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.repository.MoneyRepository;
import com.gudosnest.backend.repository.OwnerOpsRepository;
import com.gudosnest.backend.repository.PropertyRepository;
import com.gudosnest.backend.structures.InventoryTree;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Owner actions on properties, units, inventory, refunds and damage reports. */
public class OwnerService {

    private final PropertyRepository properties = new PropertyRepository();
    private final OwnerOpsRepository ops = new OwnerOpsRepository();
    private final MoneyRepository money = new MoneyRepository();

    // -------------------------------------------------------------------------- properties

    public List<Property> properties(int ownerId) {
        return properties.forOwner(ownerId);
    }

    public Property property(int propertyId, int ownerId) {
        Property p = properties.byId(propertyId).orElseThrow(() -> new RuleException("That property no longer exists."));
        if (p.getOwnerId() != ownerId) {
            throw new RuleException("That property belongs to another owner.");
        }
        return p;
    }

    /**
     * Adds a property. A house gets its one unit ("Whole house") right away from the given rates,
     * so it can be booked immediately; a multi-unit property starts empty and units are added next.
     */
    public int addProperty(int ownerId, String name, String location, Property.Type type, BigDecimal cost,
                           LocalDate acquired, List<String> photos, Unit houseUnit) {
        checkPropertyFields(name, location, cost);
        if (type == Property.Type.HOUSE) {
            checkUnit(houseUnit);
        }
        return Db.transaction(() -> {
            int id = properties.insertProperty(ownerId, name.trim(), location.trim(), type, cost, acquired);
            properties.replaceImages(id, photos);
            if (type == Property.Type.HOUSE) {
                properties.insertUnit(new Unit(0, id, Property.Type.HOUSE.label(), houseUnit.getDescription(),
                        houseUnit.getCapacity(), houseUnit.getDailyRate(), houseUnit.getCleaningFee(), false));
            }
            return id;
        });
    }

    public void updateProperty(int ownerId, int propertyId, String name, String location, BigDecimal cost,
                               LocalDate acquired, List<String> photos) {
        property(propertyId, ownerId);
        checkPropertyFields(name, location, cost);
        Db.transaction(() -> {
            properties.updateProperty(propertyId, name.trim(), location.trim(), cost, acquired);
            properties.replaceImages(propertyId, photos);
            return null;
        });
    }

    public void saveUnit(int ownerId, Unit unit) {
        Property p = property(unit.getPropertyId(), ownerId);
        checkUnit(unit);
        boolean nameTaken = p.getUnits().stream()
                .anyMatch(u -> u.getId() != unit.getId() && u.getName().equalsIgnoreCase(unit.getName().trim()));
        if (nameTaken) {
            throw new RuleException("This property already has a unit called \"" + unit.getName().trim() + "\".");
        }
        unit.setName(unit.getName().trim());
        if (unit.getId() == 0) {
            if (p.isHouse()) {
                throw new RuleException("A house has exactly one unit. Change it, or list a multi-unit property.");
            }
            properties.insertUnit(unit);
        } else {
            properties.updateUnit(unit);
        }
    }

    public void deleteUnit(int ownerId, Unit unit) {
        Property p = property(unit.getPropertyId(), ownerId);
        if (p.isHouse()) {
            throw new RuleException("A house needs its one unit.");
        }
        if (properties.unitHasBookings(unit.getId())) {
            throw new RuleException(unit.getName() + " has bookings, so it can't be deleted.");
        }
        Db.transaction(() -> {
            properties.deleteUnit(unit.getId());
            return null;
        });
    }

    private void checkPropertyFields(String name, String location, BigDecimal cost) {
        if (name == null || name.isBlank()) {
            throw new RuleException("Enter the property name.");
        }
        if (location == null || location.isBlank()) {
            throw new RuleException("Enter the location, for example Tagaytay, Cavite.");
        }
        if (cost == null || cost.signum() < 0) {
            throw new RuleException("Enter the acquisition cost (0 or more).");
        }
    }

    private void checkUnit(Unit unit) {
        if (unit.getName() == null || unit.getName().isBlank()) {
            throw new RuleException("Enter the unit name.");
        }
        if (unit.getCapacity() < 1) {
            throw new RuleException("Guests must be at least 1.");
        }
        if (unit.getDailyRate() == null || unit.getDailyRate().signum() <= 0) {
            throw new RuleException("Enter a nightly rate above ₱0.00.");
        }
        if (unit.getCleaningFee() == null || unit.getCleaningFee().signum() < 0) {
            throw new RuleException("Enter a cleaning fee (₱0.00 or more).");
        }
    }

    // --------------------------------------------------------------------------- inventory

    /** The property's inventory as a tree: property → units → items (see InventoryTree). */
    public InventoryTree inventory(Property property) {
        InventoryTree root = new InventoryTree(property.getName());
        Map<Integer, InventoryTree> unitNodes = property.getUnits().stream()
                .collect(Collectors.toMap(Unit::getId, u -> root.addChild(new InventoryTree(u.getName())),
                        (a, b) -> a, java.util.LinkedHashMap::new));
        for (Item item : ops.itemsForProperty(property.getId())) {
            InventoryTree node = item.getUnitId() == null ? root : unitNodes.getOrDefault(item.getUnitId(), root);
            node.addItem(item);
        }
        return root;
    }

    public void saveItem(Item item, boolean isNew) {
        if (item.getName() == null || item.getName().isBlank()) {
            throw new RuleException("Enter the item name.");
        }
        if (item.getQuantity() < 0) {
            throw new RuleException("Quantity can't be below 0.");
        }
        if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
            throw new RuleException("Enter the price per item (₱0.00 or more).");
        }
        if (isNew) {
            ops.insertItem(item);
        } else {
            ops.updateItem(item.getId(), item.getName().trim(), item.getType(), item.getQuantity(),
                    item.getUnitPrice(), item.getCondition());
        }
    }

    public void setQuantity(Item item, int quantity) {
        ops.setQuantity(item.getId(), Math.max(0, quantity));
    }

    public void setCondition(Item item, Item.Condition condition) {
        ops.setCondition(item.getId(), condition);
    }

    public void deleteItem(Item item) {
        ops.deleteItem(item.getId());
    }

    /** Copies every item of one unit into another (Prototype: each Item clones itself). Returns how many. */
    public int copyInventory(List<Item> sourceItems, int targetUnitId, List<Item> targetItems) {
        Map<String, Item> existing = targetItems.stream()
                .collect(Collectors.toMap(i -> i.getName().toLowerCase(), Function.identity(), (a, b) -> a));
        List<Item> copies = sourceItems.stream()
                .filter(i -> !existing.containsKey(i.getName().toLowerCase()))
                .map(i -> i.copyTo(targetUnitId))
                .toList();
        if (copies.isEmpty()) {
            throw new RuleException("The target unit already has all of these items.");
        }
        Db.transaction(() -> {
            copies.forEach(ops::insertItem);
            return null;
        });
        return copies.size();
    }

    // ----------------------------------------------------------------------------- refunds

    public List<RefundRow> refundRequests(int ownerId) {
        return money.requestedRefunds(ownerId);
    }

    public RefundRow refund(int refundId) {
        return money.refundById(refundId).orElseThrow(() -> new RuleException("That refund no longer exists."));
    }

    /** Approving a refund pays it out at once ("processed") and records it in the ledger. */
    public void approveRefund(int refundId, int ownerId) {
        RefundRow r = ownedRequested(refundId, ownerId);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Db.transaction(() -> {
            money.markRefundProcessed(refundId, now, ReceiptPaths.refund(r.paymentId()));
            money.insertTransaction(r.booking().getPropertyId(), r.paymentId(), "refund", r.amount(), now, null);
            return null;
        });
    }

    public void rejectRefund(int refundId, int ownerId) {
        ownedRequested(refundId, ownerId);
        money.markRefundRejected(refundId, LocalDateTime.now().withNano(0));
    }

    private RefundRow ownedRequested(int refundId, int ownerId) {
        RefundRow r = refund(refundId);
        if (r.booking().getOwnerId() != ownerId) {
            throw new RuleException("That refund is for another owner's property.");
        }
        if (!"requested".equals(r.status())) {
            throw new RuleException("This refund has already been reviewed.");
        }
        return r;
    }

    public List<PaymentRow> payments(int ownerId) {
        return money.paymentRows(ownerId);
    }

    // ---------------------------------------------------------------------- damage reports

    public List<DamageReport> damageReports(int ownerId) {
        return ops.damageReports(ownerId);
    }

    /** Bookings a report can be filed against: the stay has started (staying, check-out due, completed). */
    public List<Booking> reportableBookings(List<Booking> ownerBookings) {
        LocalDateTime now = LocalDateTime.now();
        return ownerBookings.stream()
                .filter(b -> switch (b.getStage(now)) {
                    case STAYING, CHECKOUT_DUE, COMPLETED -> true;
                    default -> false;
                })
                .sorted((a, b) -> b.getCheckOut().compareTo(a.getCheckOut()))
                .toList();
    }

    public void fileDamageReport(int ownerId, Booking booking, String description, String photoPath, BigDecimal fee) {
        if (booking == null) {
            throw new RuleException("Choose the booking the damage happened during.");
        }
        if (booking.getOwnerId() != ownerId) {
            throw new RuleException("That booking is on another owner's property.");
        }
        if (description == null || description.isBlank()) {
            throw new RuleException("Describe the damage.");
        }
        if (fee == null || fee.signum() <= 0) {
            throw new RuleException("Enter the fee to charge (above ₱0.00).");
        }
        ops.insertDamageReport(booking.getId(), description.trim(), photoPath, fee, LocalDateTime.now().withNano(0));
    }

    /** Charges the renter: creates a paid damage-fee payment and records the income. */
    public void chargeDamage(int ownerId, DamageReport report) {
        DamageReport r = openReport(ownerId, report.id());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Db.transaction(() -> {
            int paymentId = money.insertPaidPayment(r.booking().getId(), r.requestedFee(), "damage_fee",
                    ReceiptPaths.damageFee(r.id()), now);
            ops.chargeDamageReport(r.id(), paymentId, now);
            money.insertTransaction(r.booking().getPropertyId(), paymentId, "damage_fee_income", r.requestedFee(), now,
                    "Damage report #" + r.id());
            return null;
        });
    }

    public void dismissDamage(int ownerId, DamageReport report) {
        openReport(ownerId, report.id());
        ops.dismissDamageReport(report.id(), LocalDateTime.now().withNano(0));
    }

    private DamageReport openReport(int ownerId, int reportId) {
        DamageReport r = ops.damageReport(reportId, ownerId)
                .orElseThrow(() -> new RuleException("That report no longer exists."));
        if (!"open".equals(r.status())) {
            throw new RuleException("This report has already been resolved.");
        }
        return r;
    }
}
