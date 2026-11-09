package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.Stepper;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.math.BigDecimal;

/** Add, edit or duplicate one unit of a property. */
final class UnitDialog {

    private UnitDialog() {
    }

    static boolean add(Component parent, Portal portal, Property property) {
        Unit blank = new Unit(0, property.getId(), "", "", 2, null, BigDecimal.ZERO, false);
        return show(parent, portal, property, blank, "Add unit", "Add unit");
    }

    static boolean edit(Component parent, Portal portal, Property property, Unit unit) {
        return show(parent, portal, property, unit, "Edit " + unit.getName(), "Save changes");
    }

    /** Starts a new unit from this one (Prototype: Unit.copy) with its rates, guests and description. */
    static boolean duplicate(Component parent, Portal portal, Property property, Unit unit) {
        return show(parent, portal, property, unit.copy(unit.getName() + " (copy)"), "Duplicate unit", "Add unit");
    }

    private static boolean show(Component parent, Portal portal, Property property, Unit unit,
                                String title, String saveText) {
        boolean[] saved = {false};
        AppDialog dialog = new AppDialog(parent, title);
        JPanel body = dialog.getBody();
        body.add(Ui.muted(property.getName()), "gapbottom 4");
        Fields fields = new Fields(unit, !property.isHouse());
        body.add(fields.panel);

        dialog.addCloseButton("Cancel");
        AppButton save = new AppButton(saveText, AppButton.Style.PRIMARY);
        save.addActionListener(e -> {
            if (BookingReviewDialog.run(dialog, () -> portal.owner.saveUnit(portal.user().getId(),
                    fields.toUnit(unit.getId(), property.getId(), unit.isOccupied())))) {
                saved[0] = true;
                dialog.dispose();
            }
        });
        dialog.addButton(save);
        dialog.getRootPane().setDefaultButton(save);
        dialog.showDialog(520);
        return saved[0];
    }

    /**
     * The unit inputs, also used by the Add property dialog for a house's single unit
     * (then without the name, which is always "Whole house").
     */
    static final class Fields {
        final JPanel panel = Ui.transparent("insets 0, gap 12 8, fillx, wrap 2", "[grow,fill,sg f][grow,fill,sg f]");
        private final JTextField name = Ui.textField("For example Courtyard Suite A");
        private final Stepper guests;
        private final JTextField rate = Ui.textField("For example 2800");
        private final JTextField fee = Ui.textField("For example 500");
        private final JTextArea description = Ui.textArea(3);
        private final boolean withName;

        Fields(Unit unit, boolean withName) {
            this.withName = withName;
            guests = new Stepper(Math.max(1, unit.getCapacity()), 1, 30);
            name.setText(unit.getName());
            rate.setText(Formats.plainMoney(unit.getDailyRate()));
            fee.setText(Formats.plainMoney(unit.getCleaningFee()));
            description.setText(unit.getDescription());
            if (withName) {
                panel.add(Ui.field("Unit name", name), "span 2");
            }
            panel.add(Ui.field("Nightly rate (₱)", rate));
            panel.add(Ui.field("Cleaning fee (₱, once per stay)", fee));
            JPanel guestsField = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
            guestsField.add(Ui.label("Maximum guests", Fonts.bodyMedium(), Colors.TEXT), "gapbottom 4");
            guestsField.add(guests, "h 40!");
            panel.add(guestsField, "span 2");
            panel.add(Ui.fieldFree("Description (shown to renters)", new JScrollPane(description)), "span 2");
            panel.add(Widgets.wrap("Example: Ground floor · 1 King bed · En-suite bath", Fonts.caption(),
                    Colors.TEXT_MUTED), "span 2, wmin 0");
        }

        /** Reads the inputs; a typo in an amount is reported the same way as a broken rule. */
        Unit toUnit(int id, int propertyId, boolean occupied) {
            BigDecimal nightly = Formats.parseMoney(rate.getText());
            if (nightly == null) {
                throw new RuleException("Enter the nightly rate as a number, for example 2800.");
            }
            BigDecimal cleaning = fee.getText().isBlank() ? BigDecimal.ZERO : Formats.parseMoney(fee.getText());
            if (cleaning == null) {
                throw new RuleException("Enter the cleaning fee as a number, for example 500 (or 0).");
            }
            String unitName = withName ? name.getText() : Property.Type.HOUSE.label();
            String text = description.getText().isBlank() ? null : description.getText().trim();
            return new Unit(id, propertyId, unitName, text, guests.getValue(), nightly, cleaning, occupied);
        }
    }
}
