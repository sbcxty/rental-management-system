package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.service.FinanceService;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.DateField;
import com.gudosnest.frontend.components.MoneyInput;
import com.gudosnest.frontend.components.Segmented;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** "Add expense": property, category, amount, date (not in the future) and an optional note. */
final class ExpenseDialog {

    private ExpenseDialog() {
    }

    /** Returns true if an expense was added. {@code propertyId} (may be null) is selected first. */
    static boolean show(Component parent, Portal portal, List<Property> properties, Integer propertyId) {
        if (properties.isEmpty()) {
            AppDialog.info(parent, "Add a property first; expenses are recorded per property.");
            return false;
        }
        boolean[] added = {false};
        AppDialog dialog = new AppDialog(parent, "Add expense");
        JPanel body = dialog.getBody();

        JComboBox<PropertyFilter.Choice> property = new JComboBox<>();
        for (Property p : properties) {
            PropertyFilter.Choice c = new PropertyFilter.Choice(p);
            property.addItem(c);
            if (propertyId != null && p.getId() == propertyId) {
                property.setSelectedItem(c);
            }
        }
        body.add(Ui.field("Property", property), "wmin 0, gapbottom 8");

        List<String> typeKeys = new ArrayList<>(FinanceService.EXPENSE_TYPES.keySet());
        Segmented category = new Segmented(FinanceService.EXPENSE_TYPES.values().toArray(new String[0]));
        category.setSelectedIndex(0);
        JPanel categoryField = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        categoryField.add(Ui.label("Category", Fonts.bodyMedium(), Colors.TEXT), "gapbottom 4");
        categoryField.add(category);
        body.add(categoryField, "gapbottom 8");

        JPanel row = Ui.transparent("insets 0, gap 12 0, fillx", "[grow,fill,sg r][grow,fill,sg r]");
        JTextField amount = Ui.textField("For example 2,500");
        row.add(Ui.field("Amount (₱)", amount), "aligny top");
        DateField date = new DateField("Date");
        date.setMaximum(LocalDate.now());
        date.setValue(LocalDate.now());
        row.add(Ui.field("Date", date), "aligny top");
        body.add(row, "gapbottom 8");

        JTextField note = Ui.textField("For example: aircon cleaning, Room 2");
        body.add(Ui.field("Note (optional, up to 255 characters)", note), "wmin 0");

        dialog.addCloseButton("Cancel");
        AppButton save = new AppButton("Add expense", AppButton.Style.PRIMARY);
        save.addActionListener(e -> {
            boolean ok = BookingReviewDialog.run(dialog, () -> {
                BigDecimal value = MoneyInput.parse(amount.getText(), "Amount");
                PropertyFilter.Choice chosen = (PropertyFilter.Choice) property.getSelectedItem();
                portal.finance.addExpense(portal.user().getId(), chosen == null ? null : chosen.property(),
                        typeKeys.get(category.getSelectedIndex()), value, date.getValue(), note.getText());
            });
            if (ok) {
                added[0] = true;
                dialog.dispose();
            }
        });
        dialog.addButton(save);
        dialog.getRootPane().setDefaultButton(save);
        dialog.showDialog(600);   // 600 square fits all four fields without scrolling
        return added[0];
    }
}
