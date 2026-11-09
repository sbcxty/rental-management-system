package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Item;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Segmented;
import com.gudosnest.frontend.components.Stepper;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.math.BigDecimal;

/** Add or edit one inventory item. */
final class ItemDialog {

    private ItemDialog() {
    }

    /** New item in a unit (unitId null = shared by the whole property). */
    static boolean add(Component parent, Portal portal, int propertyId, Integer unitId, String placeName) {
        Item blank = new Item(0, propertyId, unitId, "", Item.Type.EQUIPMENT, 1, BigDecimal.ZERO, Item.Condition.GOOD, null);
        return show(parent, portal, blank, true, "Add item", placeName);
    }

    static boolean edit(Component parent, Portal portal, Item item, String placeName) {
        return show(parent, portal, item, false, "Edit " + item.getName(), placeName);
    }

    private static boolean show(Component parent, Portal portal, Item item, boolean isNew, String title, String placeName) {
        boolean[] saved = {false};
        AppDialog dialog = new AppDialog(parent, title);
        JPanel body = dialog.getBody();
        body.add(Ui.muted(placeName), "gapbottom 4");

        JTextField name = Ui.textField("For example Towels");
        name.setText(item.getName());
        body.add(Ui.field("Item name", name));

        Segmented type = new Segmented(Item.Type.EQUIPMENT.label(), Item.Type.CONSUMABLE.label());
        type.setSelectedIndex(item.getType() == Item.Type.CONSUMABLE ? 1 : 0);
        JPanel typeField = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        typeField.add(Ui.label("Category", Fonts.bodyMedium(), Colors.TEXT), "gapbottom 4");
        typeField.add(type);
        typeField.add(Ui.caption("Equipment lasts (TV, rice cooker); consumables are used up (soap, towels)."), "gaptop 4");
        body.add(typeField);

        JPanel row = Ui.transparent("insets 0, gap 12 0, fillx", "[][grow,fill,sg r][grow,fill,sg r]");
        Stepper quantity = new Stepper(item.getQuantity(), 0, 999);
        JPanel quantityField = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        quantityField.add(Ui.label("Quantity", Fonts.bodyMedium(), Colors.TEXT), "gapbottom 4");
        quantityField.add(quantity, "h 40!");
        row.add(quantityField, "aligny top");
        JTextField price = Ui.textField("For example 350");
        price.setText(Formats.plainMoney(item.getUnitPrice()));
        row.add(Ui.field("Price per item (₱)", price), "aligny top");
        JComboBox<Item.Condition> condition = new JComboBox<>(Item.Condition.values());
        condition.setSelectedItem(item.getCondition());
        row.add(Ui.field("Condition", condition), "aligny top");
        body.add(row);

        dialog.addCloseButton("Cancel");
        AppButton save = new AppButton(isNew ? "Add item" : "Save changes", AppButton.Style.PRIMARY);
        save.addActionListener(e -> {
            boolean ok = BookingReviewDialog.run(dialog, () -> {
                BigDecimal unitPrice = price.getText().isBlank() ? BigDecimal.ZERO : Formats.parseMoney(price.getText());
                if (unitPrice == null) {
                    throw new RuleException("Enter the price per item as a number, for example 350 (or 0).");
                }
                Item edited = new Item(item.getId(), item.getPropertyId(), item.getUnitId(), name.getText(),
                        type.getSelectedIndex() == 1 ? Item.Type.CONSUMABLE : Item.Type.EQUIPMENT,
                        quantity.getValue(), unitPrice, (Item.Condition) condition.getSelectedItem(), null);
                portal.owner.saveItem(edited, isNew);
            });
            if (ok) {
                saved[0] = true;
                dialog.dispose();
            }
        });
        dialog.addButton(save);
        dialog.getRootPane().setDefaultButton(save);
        dialog.showDialog(560);
        return saved[0];
    }
}
