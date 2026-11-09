package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Item;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.structures.InventoryTree;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Stepper;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory of one unit (Stitch "Inventory"): a unit dropdown, item count and how many need
 * attention, search, category tabs, and a table where quantity and condition change in place.
 */
public class InventoryPage extends BasePage {

    private static final String COLUMNS = "[grow,fill,wmin 160][120!][130!,center][160!][120!][]";

    private final int propertyId;
    private Integer unitId;          // null = items shared by the whole property
    private int category;            // 0 all, 1 consumable, 2 equipment
    private String query = "";

    private Property property;
    private List<Item> items = List.of();
    private final JPanel tableHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");
    private final JLabel summary = Ui.muted("");

    public InventoryPage(Portal portal, int propertyId, Integer unitId) {
        super(portal);
        this.propertyId = propertyId;
        this.unitId = unitId;
    }

    /** One choice in the unit dropdown. */
    private record Place(Integer unitId, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void build() {
        property = portal.owner.property(propertyId, portal.user().getId());
        InventoryTree tree = portal.owner.inventory(property);
        List<Place> places = new ArrayList<>();
        for (Unit u : property.getUnits()) {
            places.add(new Place(u.getId(), u.getName()));
        }
        if (!tree.getItems().isEmpty()) {
            places.add(new Place(null, "Shared (whole property)"));
        }
        if (places.stream().noneMatch(p -> java.util.Objects.equals(p.unitId(), unitId)) && !places.isEmpty()) {
            unitId = places.get(0).unitId();
        }
        items = itemsFor(tree);

        PageHeader header = new PageHeader("Inventory");
        header.setBack("Back to " + property.getName(), portal::back);
        if (places.size() > 1) {
            JComboBox<Place> picker = new JComboBox<>(places.toArray(new Place[0]));
            places.stream().filter(p -> java.util.Objects.equals(p.unitId(), unitId)).findFirst()
                    .ifPresent(picker::setSelectedItem);
            picker.addActionListener(e -> {
                unitId = ((Place) picker.getSelectedItem()).unitId();
                reload();
            });
            header.addNextToTitle(picker);
        } else if (!places.isEmpty()) {
            header.addNextToTitle(Ui.label(places.get(0).name(), Fonts.bodyMedium(), Colors.TEXT_MUTED));
        }
        if (unitId != null && property.getUnits().size() > 1) {
            AppButton copy = new AppButton("Copy from another unit", AppButton.Style.INVERTED).withIcon("copy");
            copy.addActionListener(e -> copyFromUnit());
            header.addAction(copy);
        }
        if (!places.isEmpty()) {
            AppButton add = new AppButton("Add item", AppButton.Style.PRIMARY).withIcon("plus");
            add.addActionListener(e -> {
                if (ItemDialog.add(this, portal, propertyId, unitId, placeName())) {
                    reload();
                }
            });
            header.addAction(add);
        }
        add(header, "gapbottom 4");
        updateSummary();
        add(summary, "gapbottom 24");

        if (places.isEmpty()) {
            add(Ui.emptyState("This property has no units yet. Add a unit first, then its inventory."), "growx");
            return;
        }

        JPanel controls = Ui.transparent("insets 0, fillx", "[][grow][]");
        JTextField search = Ui.searchField("Search items");
        search.setText(query);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                query = search.getText().trim().toLowerCase();
                refreshTable();
            }
        });
        controls.add(search, "w 320!, h 40!");
        TabBar tabs = new TabBar("All", "Consumable", "Equipment");
        tabs.setSelectedIndex(category);
        tabs.onChange(i -> {
            category = i;
            refreshTable();
        });
        controls.add(tabs, "cell 2 0");
        add(controls, "gapbottom 16");
        add(tableHolder);
        refreshTable();
    }

    private List<Item> itemsFor(InventoryTree tree) {
        if (unitId == null) {
            return tree.getItems();
        }
        String name = property.getUnits().stream().filter(u -> u.getId() == unitId)
                .map(Unit::getName).findFirst().orElse("");
        return tree.getChildren().stream().filter(t -> t.getName().equals(name)).findFirst()
                .map(InventoryTree::getItems).orElse(List.of());
    }

    private String placeName() {
        if (unitId == null) {
            return property.getName() + " · Shared (whole property)";
        }
        return property.getName() + " · " + property.getUnits().stream().filter(u -> u.getId() == unitId)
                .map(Unit::getName).findFirst().orElse("");
    }

    private void updateSummary() {
        long attention = items.stream().filter(i -> i.getCondition().needsAttention()).count();
        java.math.BigDecimal value = items.stream().map(Item::getTotalValue)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        summary.setText(items.size() + (items.size() == 1 ? " item" : " items") + " · "
                + attention + (attention == 1 ? " needs" : " need") + " attention · Total value " + Formats.money(value));
    }

    // ----------------------------------------------------------------------- table

    private void refreshTable() {
        tableHolder.removeAll();
        List<Item> shown = items.stream()
                .filter(i -> category == 0 || (category == 1) == (i.getType() == Item.Type.CONSUMABLE))
                .filter(i -> query.isEmpty() || i.getName().toLowerCase().contains(query))
                .sorted(Comparator.comparing((Item i) -> !i.getCondition().needsAttention())
                        .thenComparing(i -> i.getName().toLowerCase()))
                .toList();

        Card table = new Card(new MigLayout("insets 0, gap 0, fillx, wrap 6", COLUMNS));
        JPanel head = new JPanel(new MigLayout("insets 0 16 0 16, gap 0, fillx", COLUMNS, "[44!]"));
        head.setBackground(Colors.TABLE_HEADER);
        head.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.BORDER));
        for (String title : new String[]{"ITEM", "CATEGORY", "QUANTITY", "CONDITION", "LAST UPDATED", ""}) {
            head.add(Ui.label(title, Fonts.tableHeader(), Colors.TEXT_MUTED));
        }
        table.add(head, "span 6, growx");
        if (shown.isEmpty()) {
            table.add(Ui.muted(items.isEmpty() ? "No items yet. Use Add item, or copy them from another unit."
                    : "No items match."), "span 6, gap 16 16 20 20");
        }
        for (int i = 0; i < shown.size(); i++) {
            table.add(row(shown.get(i), i < shown.size() - 1), "span 6, growx");
        }
        tableHolder.add(table);
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    private JPanel row(Item item, boolean divider) {
        JPanel row = new JPanel(new MigLayout("insets 0 16 0 16, gap 0, fillx", COLUMNS, "[56!]"));
        row.setBackground(item.getCondition().needsAttention() ? Colors.SURFACE_HOVER : Colors.CARD);
        if (divider) {
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.BORDER));
        }
        row.add(Ui.label(item.getName(), Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
        row.add(Ui.muted(item.getType().label()));

        // Quantity saves on every click; the row isn't redrawn so repeated clicks keep working
        Stepper quantity = new Stepper(item.getQuantity(), 0, 999);
        quantity.onChange(q -> {
            if (BookingReviewDialog.run(this, () -> portal.owner.setQuantity(item, q))) {
                items = items.stream().map(i -> i.getId() == item.getId() ? withQuantity(i, q) : i).toList();
                updateSummary();
            }
        });
        row.add(quantity);

        JComboBox<Item.Condition> condition = new JComboBox<>(Item.Condition.values());
        condition.setSelectedItem(item.getCondition());
        condition.setForeground(item.getCondition().needsAttention() ? Colors.DANGER : Colors.TEXT);
        condition.addActionListener(e -> {
            Item.Condition chosen = (Item.Condition) condition.getSelectedItem();
            if (chosen != item.getCondition()
                    && BookingReviewDialog.run(this, () -> portal.owner.setCondition(item, chosen))) {
                reload();
            }
        });
        row.add(condition, "w 140!, h 32!");
        row.add(Ui.muted(item.getUpdatedAt() == null ? "" : Formats.date(item.getUpdatedAt())));

        JPanel actions = Ui.transparent("insets 0, gap 0", "[][]");
        AppButton edit = new AppButton("", AppButton.Style.TEXT).withIcon("pencil");
        edit.setToolTipText("Edit item");
        edit.addActionListener(e -> {
            if (ItemDialog.edit(this, portal, item, placeName())) {
                reload();
            }
        });
        AppButton delete = new AppButton("", AppButton.Style.TEXT).withIcon("trash-2");
        delete.setToolTipText("Delete item");
        delete.addActionListener(e -> {
            if (AppDialog.confirm(this, "Delete " + item.getName() + " from this inventory?", "Delete item")) {
                act(() -> portal.owner.deleteItem(item), null);
            }
        });
        actions.add(edit);
        actions.add(delete);
        row.add(actions);
        return row;
    }

    private static Item withQuantity(Item i, int quantity) {
        return new Item(i.getId(), i.getPropertyId(), i.getUnitId(), i.getName(), i.getType(), quantity,
                i.getUnitPrice(), i.getCondition(), i.getUpdatedAt());
    }

    // ----------------------------------------------------------------------- copy from another unit

    /** Copies every item of a chosen unit into this one (Prototype: Item.copyTo), skipping names it already has. */
    private void copyFromUnit() {
        InventoryTree tree = portal.owner.inventory(property);
        List<Unit> others = property.getUnits().stream().filter(u -> u.getId() != unitId).toList();
        AppDialog dialog = new AppDialog(this, "Copy inventory");
        dialog.getBody().add(com.gudosnest.frontend.components.Widgets.wrap(
                "Copies every item of the chosen unit into " + placeName() + ", with condition set to Good. "
                        + "Items this unit already has (same name) are skipped.", Fonts.body(), Colors.TEXT), "wmin 0");
        JComboBox<Unit> source = new JComboBox<>(others.toArray(new Unit[0]));
        dialog.getBody().add(Ui.field("Copy from", source));
        dialog.addCloseButton("Cancel");
        AppButton copy = new AppButton("Copy items", AppButton.Style.PRIMARY);
        copy.addActionListener(e -> {
            Unit from = (Unit) source.getSelectedItem();
            if (from == null) {
                return;
            }
            List<Item> sourceItems = tree.getChildren().stream().filter(t -> t.getName().equals(from.getName()))
                    .findFirst().map(InventoryTree::getItems).orElse(List.of());
            if (sourceItems.isEmpty()) {
                AppDialog.error(dialog, from.getName() + " has no items to copy.");
                return;
            }
            int[] copied = {0};
            if (BookingReviewDialog.run(dialog, () -> copied[0] = portal.owner.copyInventory(sourceItems, unitId, items))) {
                dialog.dispose();
                AppDialog.info(this, "Copied " + copied[0] + (copied[0] == 1 ? " item." : " items."));
                reload();
            }
        });
        dialog.addButton(copy);
        dialog.showDialog(480);
    }
}
