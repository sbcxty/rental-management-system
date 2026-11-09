package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.CardGrid;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.util.List;

/**
 * Owner's properties (Stitch "Properties"): a location filter, a one-line summary, and a card per
 * property. A card opens the property's units; "Open inventory" goes straight to the inventory.
 */
public class PropertiesPage extends BasePage {

    private static final String ALL_LOCATIONS = "All locations";

    private String location = ALL_LOCATIONS;

    public PropertiesPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        List<Property> all = portal.owner.properties(portal.user().getId());
        List<Property> shown = all.stream()
                .filter(p -> ALL_LOCATIONS.equals(location) || p.getLocation().equals(location)).toList();

        PageHeader header = new PageHeader("Properties");
        JComboBox<String> locations = new JComboBox<>();
        locations.addItem(ALL_LOCATIONS);
        all.stream().map(Property::getLocation).distinct().sorted().forEach(locations::addItem);
        locations.setSelectedItem(location);
        locations.addActionListener(e -> {
            location = (String) locations.getSelectedItem();
            reload();
        });
        header.addAction(locations);
        AppButton add = new AppButton("Add property", AppButton.Style.PRIMARY).withIcon("plus");
        add.addActionListener(e -> {
            Integer id = PropertyDialog.add(this, portal);
            if (id != null) {
                reload();
            }
        });
        header.addAction(add);
        add(header, "gapbottom 16");

        add(summary(shown), "gapbottom 12");
        add(Ui.divider(), "gapbottom 24");

        if (shown.isEmpty()) {
            add(Ui.emptyState(all.isEmpty()
                    ? "No properties yet. Use Add property to list your first one."
                    : "No properties in " + location + "."), "growx");
            return;
        }
        CardGrid grid = new CardGrid(300, 3);
        for (Property p : shown) {
            grid.addCard(card(p));
        }
        add(grid);
    }

    /** "Properties 6 · Bookable units 24 (A whole house counts as 1.) · Occupied now 9 of 24" */
    private JComponent summary(List<Property> shown) {
        int units = shown.stream().mapToInt(p -> p.getUnits().size()).sum();
        long occupied = shown.stream().mapToLong(Property::getOccupiedCount).sum();
        JPanel row = Ui.transparent("insets 0, gap 8", "");
        row.add(Ui.label("Properties", Fonts.bodyBold(), Colors.TEXT));
        row.add(new Badge(String.valueOf(shown.size()), Badge.Tone.NEUTRAL));
        row.add(Ui.caption("•"), "gapx 8 8");
        row.add(Ui.label("Bookable units", Fonts.bodyBold(), Colors.TEXT));
        row.add(new Badge(String.valueOf(units), Badge.Tone.NEUTRAL));
        row.add(Ui.caption("(A whole house counts as 1.)"));
        row.add(Ui.caption("•"), "gapx 8 8");
        row.add(Ui.label("Occupied now", Fonts.bodyBold(), Colors.TEXT));
        row.add(new Badge(occupied + " of " + units, Badge.Tone.NEUTRAL));
        return row;
    }

    private JComponent card(Property p) {
        Card card = Widgets.clickableCard(new MigLayout("insets 0, gap 0, fillx, wrap 1", "[grow,fill]"),
                () -> portal.open(new UnitsPage(portal, p.getId())));
        card.add(new Widgets.Photo(p.getCoverImage(), 12, false), "h 200!, wmin 0");

        JPanel body = Ui.transparent("insets 20 24 16 24, gap 0, fillx, wrap 1", "[grow,fill]");
        JPanel title = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill][]");
        title.add(Ui.label(p.getName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        title.add(status(p), "aligny top");
        body.add(title);
        body.add(Ui.iconLabel(p.getLocation(), "map-pin", Fonts.caption(), Colors.TEXT_MUTED), "wmin 0, gapbottom 12");
        body.add(Ui.divider(), "gapbottom 12");

        JPanel facts = Ui.transparent("insets 0, fillx", "[grow][right]");
        facts.add(Ui.muted(p.getUnitSummary()));
        facts.add(Ui.label(p.getUnits().isEmpty() ? "No units yet"
                : "From " + Formats.money(p.getLowestRate()) + " per night", Fonts.bodyBold(), Colors.TEXT));
        body.add(facts, "gapbottom 8");

        // A button, not a link: buttons keep their own click, so this doesn't also open the card
        AppButton inventory = new AppButton("Open inventory", AppButton.Style.TEXT).withIcon("arrow-right");
        inventory.setHorizontalTextPosition(SwingConstants.LEFT);
        inventory.addActionListener(e -> openInventory(p));
        body.add(inventory, "growx 0, gapleft -16");
        card.add(body);
        return card;
    }

    /** A house has one unit, so its inventory opens directly; a multi-unit property asks which unit first. */
    private void openInventory(Property p) {
        if (p.isHouse() && !p.getUnits().isEmpty()) {
            portal.open(new InventoryPage(portal, p.getId(), p.getUnits().get(0).getId()));
        } else {
            portal.open(new UnitsPage(portal, p.getId()));
        }
    }

    private static Badge status(Property p) {
        if (p.getUnits().isEmpty()) {
            return new Badge("No units", Badge.Tone.MUTED);
        }
        if (p.isHouse()) {
            return p.getOccupiedCount() > 0
                    ? new Badge("Occupied", Badge.Tone.NEUTRAL)
                    : new Badge("Available", Badge.Tone.INFO);
        }
        return new Badge(p.getOccupiedCount() + " of " + p.getUnits().size() + " units occupied", Badge.Tone.NEUTRAL);
    }
}
