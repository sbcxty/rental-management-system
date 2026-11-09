package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.structures.InventoryTree;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Avatar;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.CardGrid;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.StatCard;
import com.gudosnest.frontend.components.StatRow;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * One property's units (Stitch "Unit Picker"): counts, All / Unoccupied / Occupied tabs, a name
 * search, and a card per unit with its rates, who is staying or what's next, and its inventory.
 */
public class UnitsPage extends BasePage {

    private static final int TAB_ALL = 0;
    private static final int TAB_UNOCCUPIED = 1;
    private static final int TAB_OCCUPIED = 2;

    private final int propertyId;
    private int tab;
    private String query = "";

    // Loaded in build(), used when the grid is redrawn for a tab or search change
    private Property property;
    private List<Booking> bookings;
    private Map<String, InventoryTree> inventoryByUnit;
    private final JPanel gridHolder = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");

    public UnitsPage(Portal portal, int propertyId) {
        super(portal);
        this.propertyId = propertyId;
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        property = portal.owner.property(propertyId, ownerId);
        bookings = portal.bookings.forOwner(ownerId).stream().filter(b -> b.getPropertyId() == propertyId).toList();
        InventoryTree tree = portal.owner.inventory(property);
        inventoryByUnit = tree.getChildren().stream()
                .collect(Collectors.toMap(InventoryTree::getName, t -> t, (a, b) -> a));

        add(header(), "gapbottom 24");

        List<Unit> units = property.getUnits();
        long occupied = units.stream().filter(Unit::isOccupied).count();
        StatRow stats = new StatRow();
        stats.addCard(stat("Total units", "building-2", String.valueOf(units.size()),
                property.isHouse() ? "A whole house counts as 1" : null));
        stats.addCard(stat("Occupied", "users", String.valueOf(occupied), null));
        stats.addCard(stat("Vacant", "house", String.valueOf(units.size() - occupied), null));
        stats.addCard(stat("Items needing attention", "triangle-alert", String.valueOf(tree.needsAttentionCount()),
                "Damaged, under maintenance or missing"));
        add(stats, "gapbottom 24");

        JPanel controls = Ui.transparent("insets 0, fillx", "[][grow][]");
        TabBar tabs = new TabBar("All units", "Unoccupied", "Occupied");
        tabs.setCount(TAB_ALL, units.size());
        tabs.setCount(TAB_UNOCCUPIED, (int) (units.size() - occupied));
        tabs.setCount(TAB_OCCUPIED, (int) occupied);
        tabs.setSelectedIndex(tab);
        tabs.onChange(i -> {
            tab = i;
            refreshGrid();
        });
        controls.add(tabs);
        if (!property.isHouse()) {
            JTextField search = Ui.searchField("Search unit name");
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
                    refreshGrid();   // only the cards are redrawn, so the search box keeps focus
                }
            });
            controls.add(search, "cell 2 0, w 280!, h 40!");
        }
        add(controls, "gapbottom 16");
        add(gridHolder);
        refreshGrid();
    }

    private JComponent header() {
        PageHeader header = new PageHeader(property.isHouse() ? property.getName() : property.getName() + ": Units");
        header.setBack("Back to Properties", portal::back);
        header.addNextToTitle(new Badge(property.getType().label(), Badge.Tone.NEUTRAL));
        String acquired = property.getAcquisitionDate() == null ? ""
                : " · Acquired " + Formats.longDate(property.getAcquisitionDate());
        header.setSubtitle(property.getLocation() + " · " + property.getUnitSummary() + acquired);

        AppButton edit = new AppButton("Edit property", AppButton.Style.INVERTED).withIcon("pencil");
        edit.addActionListener(e -> {
            if (PropertyDialog.edit(this, portal, property)) {
                reload();
            }
        });
        header.addAction(edit);
        if (!property.isHouse()) {
            AppButton add = new AppButton("Add unit", AppButton.Style.PRIMARY).withIcon("plus");
            add.addActionListener(e -> {
                if (UnitDialog.add(this, portal, property)) {
                    reload();
                }
            });
            header.addAction(add);
        }
        return header;
    }

    private static StatCard stat(String title, String icon, String value, String note) {
        StatCard card = new StatCard(title, icon);
        card.setValue(value);
        if (note != null) {
            card.setNote(note);
        }
        return card;
    }

    // ----------------------------------------------------------------------- cards

    private void refreshGrid() {
        gridHolder.removeAll();
        List<Unit> shown = property.getUnits().stream()
                .filter(u -> switch (tab) {
                    case TAB_UNOCCUPIED -> !u.isOccupied();
                    case TAB_OCCUPIED -> u.isOccupied();
                    default -> true;
                })
                .filter(u -> query.isEmpty() || u.getName().toLowerCase().contains(query))
                .toList();
        if (shown.isEmpty()) {
            String message = property.getUnits().isEmpty()
                    ? "No units yet. Use Add unit to list the rooms or suites renters can book."
                    : !query.isEmpty() ? "No unit name contains \"" + query + "\"."
                    : tab == TAB_OCCUPIED ? "No unit is occupied right now." : "Every unit is occupied right now.";
            gridHolder.add(Ui.emptyState(message), "growx");
        } else {
            CardGrid grid = new CardGrid(320, 3);
            for (int i = 0; i < shown.size(); i++) {
                grid.addCard(card(shown.get(i), property.getUnits().indexOf(shown.get(i))));
            }
            gridHolder.add(grid);
        }
        gridHolder.revalidate();
        gridHolder.repaint();
    }

    private JComponent card(Unit unit, int index) {
        Card card = new Card(new MigLayout("insets 0, gap 0, fillx, wrap 1", "[grow,fill]"));

        // Units have no photos of their own, so each one shows one of the property's photos
        List<String> images = property.getImages();
        Widgets.Photo photo = new Widgets.Photo(images.isEmpty() ? null : images.get(index % images.size()), 12, false);
        photo.setLayout(new MigLayout("insets 12, fill", "[grow]", "[grow]"));
        photo.add(unit.isOccupied() ? new Badge("● Occupied", Badge.Tone.NEUTRAL) : new Badge("● Unoccupied", Badge.Tone.INFO),
                "alignx right, aligny top");
        card.add(photo, "h 180!, wmin 0");

        JPanel body = Ui.transparent("insets 20 24 20 24, gap 0, fillx, wrap 1", "[grow,fill]");
        JPanel title = Ui.transparent("insets 0, gap 0, fillx", "[grow,fill][][][]");
        title.add(Ui.label(unit.getName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        title.add(iconButton("pencil", property.isHouse() ? "Edit rates" : "Edit unit", () -> {
            if (UnitDialog.edit(this, portal, property, unit)) {
                reload();
            }
        }));
        if (!property.isHouse()) {
            title.add(iconButton("copy", "Duplicate unit", () -> {
                if (UnitDialog.duplicate(this, portal, property, unit)) {
                    reload();
                }
            }));
            title.add(iconButton("trash-2", "Delete unit", () -> delete(unit)));
        }
        body.add(title);
        String details = Formats.guests(unit.getCapacity())
                + (unit.getDescription().isBlank() ? "" : " · " + unit.getDescription());
        body.add(Ui.caption(details), "wmin 0, gapbottom 16");

        Card rates = Widgets.tintBox(new MigLayout("insets 12 16 12 16, gap 0, fillx", "[grow][right]"));
        rates.add(Ui.caption("Nightly rate"));
        rates.add(Ui.caption("Cleaning fee"), "wrap");
        rates.add(Ui.label(Formats.money(unit.getDailyRate()), Fonts.sectionTitle(), Colors.TEXT));
        rates.add(Ui.label(Formats.money(unit.getCleaningFee()), Fonts.bodyMedium(), Colors.TEXT));
        body.add(rates, "gapbottom 12");

        Optional<Booking> current = bookings.stream().filter(b -> b.getUnitId() == unit.getId())
                .filter(b -> b.getStage() == Booking.Stage.STAYING || b.getStage() == Booking.Stage.CHECKOUT_DUE)
                .findFirst();
        if (current.isPresent()) {
            Booking stay = current.get();
            body.add(guestBox(stay), "gapbottom 12");
            JPanel actions = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill,sg a][grow,fill,sg a]");
            AppButton stayDetails = new AppButton("Stay details", AppButton.Style.INVERTED);
            stayDetails.addActionListener(e -> {
                if (BookingReviewDialog.show(this, portal, stay)) {
                    reload();
                }
            });
            AppButton message = new AppButton("Message guest", AppButton.Style.INVERTED);
            message.addActionListener(e -> portal.openConversation(stay.getRenterId(), stay.getRenterName(), stay));
            actions.add(stayDetails);
            actions.add(message);
            body.add(actions, "gapbottom 8");
        } else {
            body.add(nextStayBox(unit), "gapbottom 12");
        }

        InventoryTree inventory = inventoryByUnit.get(unit.getName());
        int attention = inventory == null ? 0 : inventory.needsAttentionCount();
        int items = inventory == null ? 0 : inventory.itemCount();
        AppButton open = new AppButton("Open inventory (" + items + (items == 1 ? " item)" : " items)"),
                AppButton.Style.PRIMARY).withIcon("arrow-right");
        open.setHorizontalTextPosition(SwingConstants.LEFT);
        open.addActionListener(e -> portal.open(new InventoryPage(portal, propertyId, unit.getId())));
        body.add(open, "h 40!");
        if (attention > 0) {
            body.add(Ui.label(attention + (attention == 1 ? " item needs" : " items need") + " attention",
                    Fonts.caption(), Colors.DANGER), "gaptop 6, alignx center, growx 0");
        }
        card.add(body);
        return card;
    }

    private JComponent guestBox(Booking stay) {
        Card box = Widgets.tintBox(new MigLayout("insets 12 16 12 16, gap 12 0, fillx", "[][grow,fill][right]"));
        box.add(new Avatar(BookingReviewDialog.initials(stay.getRenterName()), 32), "spany 2, aligny center");
        box.add(Ui.label(stay.getRenterName(), Fonts.bodyBold(), Colors.TEXT), "wmin 0");
        box.add(Ui.label(checkoutText(stay), Fonts.bodyBold(), Colors.TEXT), "spany 2, aligny center, wrap");
        box.add(Ui.caption(BookingUi.dates(stay)), "wmin 0");
        return box;
    }

    private static String checkoutText(Booking stay) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), stay.getCheckOut().toLocalDate());
        if (days <= 0) {
            return stay.getStage() == Booking.Stage.CHECKOUT_DUE ? "Check-out due" : "Check-out today";
        }
        return "Check-out in " + days + "d";
    }

    /** For a vacant unit: the next approved stay, or that it's free to book. */
    private JComponent nextStayBox(Unit unit) {
        Optional<Booking> next = bookings.stream().filter(b -> b.getUnitId() == unit.getId())
                .filter(b -> b.getStage() == Booking.Stage.UPCOMING)
                .min(Comparator.comparing(Booking::getCheckIn));
        long pending = bookings.stream().filter(b -> b.getUnitId() == unit.getId())
                .filter(b -> b.getStage() == Booking.Stage.PENDING).count();
        Card box = Widgets.tintBox(new MigLayout("insets 12 16 12 16, gap 12 0, fillx", "[][grow,fill][right]"));
        box.add(new JLabel(Icons.get("calendar-check", 18, Colors.PRIMARY)), "spany 2, aligny center");
        if (next.isPresent()) {
            Booking b = next.get();
            box.add(Ui.label("Next stay: " + b.getRenterName(), Fonts.bodyBold(), Colors.TEXT), "wmin 0");
            box.add(Ui.label("Vacant", Fonts.bodyBold(), Colors.TEXT), "spany 2, aligny center, wrap");
            box.add(Ui.caption(BookingUi.dates(b)), "wmin 0");
        } else {
            box.add(Ui.label("Available for booking", Fonts.bodyBold(), Colors.TEXT), "wmin 0");
            box.add(Ui.label("Vacant", Fonts.bodyBold(), Colors.TEXT), "spany 2, aligny center, wrap");
            box.add(Ui.caption(pending > 0 ? pending + " pending " + (pending == 1 ? "request" : "requests")
                    : "No upcoming stays"), "wmin 0");
        }
        return box;
    }

    private static AppButton iconButton(String icon, String tooltip, Runnable action) {
        AppButton b = new AppButton("", AppButton.Style.TEXT).withIcon(icon);
        b.setToolTipText(tooltip);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void delete(Unit unit) {
        if (AppDialog.confirm(this, "Delete " + unit.getName() + "? Its inventory is deleted too. "
                + "Units with bookings can't be deleted.", "Delete unit")) {
            act(() -> portal.owner.deleteUnit(portal.user().getId(), unit), null);
        }
    }
}
