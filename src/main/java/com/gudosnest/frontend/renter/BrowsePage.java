package com.gudosnest.frontend.renter;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.service.BrowseService;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.backend.structures.Trie;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.CardGrid;
import com.gudosnest.frontend.components.DateField;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Stepper;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Cursor;
import java.time.LocalDate;
import java.util.List;

/**
 * Browse stays: search by location (with Trie suggestions), dates and guests, narrow by category,
 * and reserve. The searching is in BrowseService; paying is in BookDialog -> BookingService.
 */
public class BrowsePage extends BasePage {

    private List<Property> listings = List.of();
    private Trie suggestions = new Trie();

    // The search box values survive Search / pill clicks (only reload() rebuilds the page)
    private final JTextField location = Ui.textField("All destinations");
    private final DateField checkIn = new DateField("Check-in");
    private final DateField checkOut = new DateField("Check-out");
    private final Stepper guests = new Stepper(2, 1, 12);
    private String category = BrowseService.ALL_HOMES;
    private final JPanel results = Ui.transparent("insets 0, fillx, gap 0, wrap 1", "[grow,fill]");
    private Badge count;

    public BrowsePage(Portal portal) {
        super(portal);
        // One-time wiring: build() runs again on every reload, so listeners must not be added there.
        location.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.get("map-pin", 16, Colors.NEUTRAL));
        location.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        installSuggestions();
        location.addActionListener(e -> runSearch(true));
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        checkIn.setMinimum(tomorrow);
        checkOut.setMinimum(tomorrow.plusDays(1));
        checkIn.onChange(day -> {
            if (day != null) {
                checkOut.setMinimum(day.plusDays(1));
                if (checkOut.getValue() != null && !checkOut.getValue().isAfter(day)) {
                    checkOut.setValue(null);
                }
            }
        });
    }

    @Override
    protected void build() {
        listings = portal.browse.listings();
        suggestions = portal.browse.suggestions();

        PageHeader header = new PageHeader("Browse stays")
                .setSubtitle("Find and book calm properties curated for living and resting.");
        count = new Badge("", Badge.Tone.NEUTRAL);
        header.addAction(count);
        add(header, "gapbottom 24");

        add(searchCard(), "gapbottom 24");
        results.removeAll();
        add(results, "growx");
        runSearch(false);
    }

    // ------------------------------------------------------------------------ search card

    private JComponent searchCard() {
        Card card = new Card(new MigLayout("insets 16 16 14 16, gap 0, fillx, wrap 1", "[grow,fill]"));

        JPanel inputs = Ui.transparent("insets 0, gap 12, fillx", "[grow 36,fill][grow 22,fill][grow 22,fill][][]");
        inputs.add(Ui.field("Location", location), "wmin 0");
        inputs.add(Ui.field("Check-in", checkIn), "wmin 0");
        inputs.add(Ui.field("Check-out", checkOut), "wmin 0");
        inputs.add(Ui.fieldFree("Guests", guests), "aligny bottom");
        AppButton search = new AppButton("Search", AppButton.Style.PRIMARY).withIcon("search");
        search.addActionListener(e -> runSearch(true));
        inputs.add(search, "aligny bottom, h 40!");
        card.add(inputs, "gapbottom 14");

        JPanel pills = Ui.transparent("insets 0, fillx, gap 8", "[][][][][][][]push[]");
        ButtonGroup group = new ButtonGroup();
        for (String name : portal.browse.categories()) {
            JToggleButton pill = new JToggleButton(name);
            pill.setFont(Fonts.captionMedium());
            pill.setFocusPainted(false);
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.putClientProperty(FlatClientProperties.STYLE,
                    "arc:999;focusWidth:0;innerFocusWidth:0;borderWidth:0;margin:5,14,5,14"
                            + ";background:" + Colors.hex(Colors.PAGE)
                            + ";foreground:" + Colors.hex(Colors.NEUTRAL)
                            + ";hoverBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                            + ";focusedBackground:" + Colors.hex(Colors.PAGE)
                            + ";selectedBackground:" + Colors.hex(Colors.BURGUNDY)
                            + ";selectedForeground:#FFFFFF");
            pill.setSelected(name.equals(category));
            pill.addActionListener(e -> {
                category = name;
                runSearch(true);
            });
            group.add(pill);
            pills.add(pill);
        }
        AppButton clear = new AppButton("Clear filters", AppButton.Style.TEXT).withIcon("x");
        clear.addActionListener(e -> clearFilters());
        pills.add(clear);
        card.add(pills);
        return card;
    }

    /** Shows up to 6 Trie matches under the Location box while the person types. */
    private void installSuggestions() {
        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        location.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                String typed = location.getText().strip();
                popup.setVisible(false);
                popup.removeAll();
                if (typed.isEmpty() || !location.isShowing()) {
                    return;
                }
                for (String word : suggestions.startingWith(typed, 6)) {
                    if (word.equalsIgnoreCase(typed)) {
                        continue;
                    }
                    JMenuItem item = new JMenuItem(word, Icons.get("map-pin", 14, Colors.NEUTRAL));
                    item.addActionListener(e -> {
                        location.setText(word);
                        runSearch(true);
                    });
                    popup.add(item);
                }
                if (popup.getComponentCount() > 0) {
                    popup.setPopupSize(location.getWidth(), popup.getPreferredSize().height);
                    popup.show(location, 0, location.getHeight());
                    location.requestFocusInWindow();
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private void clearFilters() {
        location.setText("");
        checkIn.setValue(null);
        checkOut.setValue(null);
        category = BrowseService.ALL_HOMES;
        reload();
    }

    // -------------------------------------------------------------------------- results

    /** Filters the listings with the search box and redraws only the results. */
    private void runSearch(boolean showErrors) {
        results.removeAll();
        List<Property> found;
        try {
            found = portal.browse.search(listings, location.getText(), category,
                    checkIn.getValue(), checkOut.getValue(), guests.getValue());
        } catch (RuleException e) {
            if (showErrors) {
                AppDialog.error(this, e.getMessage());
            }
            found = List.of();
            results.add(Ui.error(e.getMessage()));
        }
        count.setText(found.size() + (found.size() == 1 ? " property available" : " properties available"));
        if (found.isEmpty() && results.getComponentCount() == 0) {
            results.add(Ui.emptyState(listings.isEmpty()
                    ? "No properties are listed yet."
                    : "No properties match your search. Try other dates, fewer guests or Clear filters."));
        } else if (!found.isEmpty()) {
            CardGrid grid = new CardGrid(300, 3);
            for (Property p : found) {
                grid.addCard(card(p));
            }
            results.add(grid, "growx");
        }
        results.revalidate();
        results.repaint();
    }

    private JComponent card(Property p) {
        List<Unit> fitting = portal.browse.fittingUnits(p, checkIn.getValue(), checkOut.getValue(), guests.getValue());
        Card card = Widgets.clickableCard(new MigLayout("insets 0, gap 0, fillx, wrap 1", "[grow,fill]"),
                () -> reserve(p, fitting));
        card.add(new Widgets.Photo(p.getCoverImage(), 12, false), "h 155!, wmin 0");

        JPanel body = Ui.transparent("insets 14 16 14 16, gap 0, fillx, wrap 1", "[grow,fill]");
        JPanel top = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill][]");
        top.add(Ui.iconLabel(p.getLocation(), "map-pin", Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        top.add(availability(p), "aligny center");
        body.add(top, "gapbottom 6");
        body.add(Ui.label(p.getName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0, gapbottom 2");
        body.add(Ui.caption("Up to " + Formats.guests(p.getMaxCapacity()) + " · " + p.getUnitSummary()), "wmin 0, gapbottom 12");

        JPanel bottom = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill][]");
        JPanel price = Ui.transparent("insets 0, gap 4", "[][]");
        price.add(Ui.label(Formats.money(p.getLowestRate()), Fonts.cardTitle(), Colors.TEXT));
        price.add(Ui.caption(p.getUnits().size() > 1 ? "/ night, from" : "/ night"), "aligny bottom");
        bottom.add(price);
        // A button, not just a click on the card: buttons keep their own action
        AppButton reserve = new AppButton("Reserve", AppButton.Style.INVERTED);
        reserve.addActionListener(e -> reserve(p, fitting));
        bottom.add(reserve);
        body.add(bottom);
        card.add(body);
        return card;
    }

    /** "Available now" when a unit is free today; with dates chosen, "Free on your dates". */
    private Badge availability(Property p) {
        if (checkIn.getValue() != null && checkOut.getValue() != null) {
            return new Badge("Free on your dates", Badge.Tone.INFO);
        }
        return p.getOccupiedCount() < p.getUnits().size()
                ? new Badge("Available now", Badge.Tone.INFO)
                : new Badge("Occupied now", Badge.Tone.MUTED);
    }

    private void reserve(Property p, List<Unit> fitting) {
        Booking made = BookDialog.show(this, portal, p, checkIn.getValue(), checkOut.getValue(),
                guests.getValue(), fitting);
        if (made != null) {
            portal.refreshBadges();
            runSearch(false);
        }
    }
}
