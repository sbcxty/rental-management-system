package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.DateField;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.ImageUtil;
import com.gudosnest.frontend.components.Segmented;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Add or edit a property: name, location, type, acquisition cost and date, photos. A house also
 * gets its one unit's rates here, so it can be booked as soon as it is saved.
 */
final class PropertyDialog {

    private PropertyDialog() {
    }

    /** Returns the new property's id, or null if the owner cancelled. */
    static Integer add(Component parent, Portal portal) {
        return show(parent, portal, null);
    }

    /** Returns true if the property was changed. */
    static boolean edit(Component parent, Portal portal, Property property) {
        return show(parent, portal, property) != null;
    }

    private static Integer show(Component parent, Portal portal, Property existing) {
        boolean adding = existing == null;
        Integer[] result = {null};
        AppDialog dialog = new AppDialog(parent, adding ? "Add property" : "Edit property");
        JPanel body = dialog.getBody();

        JTextField name = Ui.textField("For example Highland Glass Cabin");
        JTextField location = Ui.textField("For example Tagaytay Ridge, Cavite");
        JTextField cost = Ui.textField("For example 4500000");
        DateField acquired = new DateField("Pick a date");
        acquired.setMaximum(LocalDate.now());
        if (!adding) {
            name.setText(existing.getName());
            location.setText(existing.getLocation());
            cost.setText(Formats.plainMoney(existing.getAcquisitionCost()));
            acquired.setValue(existing.getAcquisitionDate());
        }
        body.add(Ui.field("Property name", name));
        body.add(Ui.field("Location", location));

        // Type: chosen once when adding (a house has exactly one unit, so it can't become multi-unit later)
        Segmented type = new Segmented(Property.Type.HOUSE.label(), Property.Type.MULTI_UNIT.label());
        JLabel typeNote = Ui.caption("");
        if (adding) {
            JPanel typeField = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
            typeField.add(Ui.label("Type", Fonts.bodyMedium(), Colors.TEXT), "gapbottom 4");
            typeField.add(type);
            typeField.add(typeNote, "gaptop 4");
            body.add(typeField);
        } else {
            body.add(Ui.row("Type", existing.getType().label()));
        }

        JPanel money = Ui.transparent("insets 0, gap 12 0, fillx", "[grow,fill,sg m][grow,fill,sg m]");
        money.add(Ui.field("Acquisition cost (₱)", cost));
        money.add(Ui.field("Acquisition date", acquired));
        body.add(money);

        PhotoList photos = new PhotoList(adding ? List.of() : existing.getImages());
        body.add(Ui.fieldFree("Photos (the first one is the cover)", photos.panel));

        // The house's single unit
        Unit houseUnit = adding || !existing.isHouse() || existing.getUnits().isEmpty()
                ? new Unit(0, 0, Property.Type.HOUSE.label(), "", 2, null, BigDecimal.ZERO, false)
                : existing.getUnits().get(0);
        UnitDialog.Fields unitFields = new UnitDialog.Fields(houseUnit, false);
        JPanel houseSection = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");
        houseSection.add(Ui.divider(), "gaptop 8, gapbottom 8");
        houseSection.add(Ui.label("Rates", Fonts.bodyBold(), Colors.TEXT), "gapbottom 4");
        houseSection.add(unitFields.panel);
        body.add(houseSection);

        Runnable showType = () -> {
            boolean house = adding ? type.getSelectedIndex() == 0 : existing.isHouse();
            houseSection.setVisible(house);
            typeNote.setText(house ? "One bookable unit: the whole house."
                    : "You add the rooms or suites next, each with its own rates.");
            dialog.pack();
        };
        type.onChange(i -> showType.run());

        dialog.addCloseButton("Cancel");
        AppButton save = new AppButton(adding ? "Add property" : "Save changes", AppButton.Style.PRIMARY);
        save.addActionListener(e -> {
            int ownerId = portal.user().getId();
            boolean ok = BookingReviewDialog.run(dialog, () -> {
                BigDecimal acquisitionCost = Formats.parseMoney(cost.getText());
                if (acquisitionCost == null) {
                    throw new RuleException("Enter the acquisition cost as a number, for example 4500000.");
                }
                boolean house = adding ? type.getSelectedIndex() == 0 : existing.isHouse();
                Unit unit = house ? unitFields.toUnit(houseUnit.getId(), adding ? 0 : existing.getId(),
                        houseUnit.isOccupied()) : null;
                List<String> paths = photos.save();
                if (adding) {
                    result[0] = portal.owner.addProperty(ownerId, name.getText(), location.getText(),
                            house ? Property.Type.HOUSE : Property.Type.MULTI_UNIT, acquisitionCost,
                            acquired.getValue(), paths, unit);
                } else {
                    portal.owner.updateProperty(ownerId, existing.getId(), name.getText(), location.getText(),
                            acquisitionCost, acquired.getValue(), paths);
                    if (unit != null) {
                        portal.owner.saveUnit(ownerId, unit);
                    }
                    result[0] = existing.getId();
                }
            });
            if (ok) {
                dialog.dispose();
            }
        });
        dialog.addButton(save);
        showType.run();
        dialog.showDialog(600);
        return result[0];
    }

    /**
     * Thumbnails of the chosen photos with a remove button each, plus "Add photos". New files are
     * copied into uploads/ only when the property is saved, so cancelling leaves nothing behind.
     */
    private static final class PhotoList {
        final JPanel panel = Ui.transparent("insets 0, gap 8", "");
        private final List<String> paths;

        PhotoList(List<String> initial) {
            paths = new ArrayList<>(initial);
            refresh();
        }

        private void refresh() {
            panel.removeAll();
            for (String path : paths) {
                JPanel tile = Ui.transparent("insets 0, gap 0", "[]");
                JButton remove = new JButton(Icons.get("x", 12, Colors.TEXT));
                remove.setToolTipText("Remove photo");
                remove.putClientProperty("JButton.buttonType", "roundRect");
                remove.addActionListener(e -> {
                    paths.remove(path);
                    refresh();
                });
                tile.add(remove, "pos (visual.x2-26) (visual.y+4), w 22!, h 22!");
                tile.add(new JLabel(ImageUtil.photo(path, 96, 72, 8)), "w 96!, h 72!");
                panel.add(tile);
            }
            JButton add = new JButton("Add photos", Icons.get("image-plus", 16, Colors.PRIMARY));
            add.setFont(Fonts.bodyMedium());
            add.setForeground(Colors.PRIMARY);
            add.addActionListener(e -> pick());
            panel.add(add, "w 120!, h 72!");
            panel.revalidate();
            panel.repaint();
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(panel);
            if (window != null) {
                window.pack();
            }
        }

        private void pick() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose property photos");
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new FileNameExtensionFilter("Pictures (JPG, PNG)", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    if (!paths.contains(f.getAbsolutePath())) {
                        paths.add(f.getAbsolutePath());
                    }
                }
                refresh();
            }
        }

        /** Copies newly picked files into uploads/ and returns the paths to store, in order. */
        List<String> save() {
            List<String> stored = new ArrayList<>();
            for (String path : paths) {
                if (path.startsWith(ImageUtil.RESOURCE_PREFIX) || path.startsWith(ImageUtil.UPLOADS_DIR)) {
                    stored.add(path);
                    continue;
                }
                try {
                    stored.add(ImageUtil.copyToUploads(new File(path)));
                } catch (IOException ex) {
                    throw new RuleException("Couldn't add " + new File(path).getName() + ": " + ex.getMessage());
                }
            }
            // Remember the copies, so pressing Save again after a mistake doesn't copy the files twice
            paths.clear();
            paths.addAll(stored);
            return stored;
        }
    }
}
