package com.gudosnest.frontend;

import com.gudosnest.backend.model.User;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Carousel;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.PortalShell;
import com.gudosnest.frontend.components.Sidebar;
import com.gudosnest.frontend.components.StatCard;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TEMPORARY (Phase 1 only): shows the shared components so the theme and sidebar can be
 * checked before the real screens exist. Removed in Phase 2 when Main opens the login screen.
 */
public final class ComponentPreview {

    private static final List<Sidebar.Item> OWNER_ITEMS = List.of(
            new Sidebar.Item("dashboard", "Dashboard", "layout-dashboard"),
            new Sidebar.Item("properties", "Properties", "building-2"),
            new Sidebar.Item("bookings", "Bookings", "calendar-days"),
            new Sidebar.Item("payments", "Payments", "credit-card"),
            new Sidebar.Item("finances", "Finances", "wallet"),
            new Sidebar.Item("messages", "Messages", "message-square"),
            new Sidebar.Item("damage", "Damage reports", "triangle-alert"));

    private static final List<Sidebar.Item> RENTER_ITEMS = List.of(
            new Sidebar.Item("browse", "Browse", "search"),
            new Sidebar.Item("history", "Booking history", "history"),
            new Sidebar.Item("messages", "Messages", "message-square"),
            new Sidebar.Item("cancellations", "Cancellations", "calendar-x"));

    private ComponentPreview() {
    }

    public static void show() {
        JFrame frame = new JFrame("Gudo's Nest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new java.awt.Dimension(1200, 760));
        User demo = new User(1, "Elena Vance", "elena.vance@example.com", true, true);
        openPortal(frame, demo, true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void openPortal(JFrame frame, User user, boolean owner) {
        List<Sidebar.Item> items = owner ? OWNER_ITEMS : RENTER_ITEMS;
        String switchLabel = owner ? "Switch to Renter portal" : "Switch to Owner portal";
        PortalShell[] shell = new PortalShell[1];
        Sidebar sidebar = new Sidebar(owner ? "Owner portal" : "Renter portal", items, user, switchLabel,
                new Sidebar.Listener() {
                    @Override
                    public void onNavigate(String itemId) {
                        String label = items.stream().filter(i -> i.id().equals(itemId))
                                .findFirst().map(Sidebar.Item::label).orElse(itemId);
                        shell[0].showPage(itemId, samplePage(frame, label));
                    }

                    @Override
                    public void onSwitchPortal() {
                        openPortal(frame, user, !owner);
                    }

                    @Override
                    public void onLogOut() {
                        AppDialog.info(frame, "Log out works from Phase 2, when the login screen exists.");
                    }
                });
        sidebar.setUnread("messages", true);
        shell[0] = new PortalShell(sidebar);
        shell[0].showPage(items.get(0).id(), samplePage(frame, items.get(0).label()));
        frame.setContentPane(shell[0]);
        frame.revalidate();
    }

    private static JComponent samplePage(JFrame frame, String title) {
        JPanel page = new JPanel(new MigLayout("insets 32, fillx, gap 16, wrap 1", "[grow,fill]"));

        PageHeader header = new PageHeader(title);
        header.addAction(new JComboBox<>(new String[]{"All properties", "Highland Glass Cabin"}));
        AppButton openDialog = new AppButton("Open dialog", AppButton.Style.PRIMARY).withIcon("plus");
        header.addAction(openDialog);
        page.add(header);

        JPanel stats = Ui.transparent("insets 0, gap 16, fillx", "[grow,fill][grow,fill][grow,fill][grow,fill]");
        StatCard earned = new StatCard("Total earned", "wallet");
        earned.setValue(Formats.money(new BigDecimal("482650")));
        earned.setNote("Last 6 months");
        StatCard active = new StatCard("Active bookings", "calendar-days");
        active.setValue("14");
        StatCard occupancy = new StatCard("Occupancy rate", "building-2");
        occupancy.setValue("72%");
        occupancy.setNote("Share of days units were booked");
        StatCard managed = new StatCard("Properties managed", "house");
        managed.setValue("6");
        stats.add(earned);
        stats.add(active);
        stats.add(occupancy);
        stats.add(managed);
        page.add(stats);

        TabBar tabs = new TabBar("Pending", "Upcoming", "Completed", "Cancelled", "All");
        tabs.setCount(0, 3);
        tabs.setCount(1, 5);
        page.add(tabs);

        Card buttons = new Card(new MigLayout("insets 16, gap 8", ""));
        buttons.add(new AppButton("Primary", AppButton.Style.PRIMARY));
        buttons.add(new AppButton("Secondary", AppButton.Style.SECONDARY));
        buttons.add(new AppButton("Inverted", AppButton.Style.INVERTED));
        buttons.add(new AppButton("Outlined", AppButton.Style.OUTLINED));
        buttons.add(new AppButton("Reject", AppButton.Style.DANGER), "wrap");
        buttons.add(new AppButton("View all", AppButton.Style.TEXT));
        buttons.add(new Badge("Pending", Badge.Tone.NEUTRAL), "gapleft 16");
        buttons.add(new Badge("Upcoming", Badge.Tone.INFO));
        buttons.add(new Badge("Occupied", Badge.Tone.STRONG));
        buttons.add(new Badge("Completed", Badge.Tone.MUTED));
        page.add(buttons);

        JPanel row = Ui.transparent("insets 0, gap 16, fillx", "[grow 60,fill][grow 40,fill]");
        DefaultTableModel model = new DefaultTableModel(new String[]{"Renter", "Dates", "Total", "Status"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 0 -> AppTable.TwoLine.class;
                    case 3 -> AppTable.Pill.class;
                    default -> Object.class;
                };
            }
        };
        LocalDateTime created = LocalDateTime.now();
        model.addRow(new Object[]{new AppTable.TwoLine("Miguel Alvarez", Formats.ref(8801, created)),
                Formats.dateRange(LocalDate.now().plusDays(12), LocalDate.now().plusDays(15)),
                Formats.money(new BigDecimal("17000")), new AppTable.Pill("Pending", Badge.Tone.NEUTRAL)});
        model.addRow(new Object[]{new AppTable.TwoLine("Sofia Laurel", Formats.ref(8807, created)),
                Formats.dateRange(LocalDate.now().plusDays(10), LocalDate.now().plusDays(13)),
                Formats.money(new BigDecimal("8900")), new AppTable.Pill("Upcoming", Badge.Tone.INFO)});
        model.addRow(new Object[]{new AppTable.TwoLine("David Kim", Formats.ref(8815, created)),
                Formats.dateRange(LocalDate.now().minusDays(20), LocalDate.now().minusDays(17)),
                Formats.money(new BigDecimal("17000")), new AppTable.Pill("Completed", Badge.Tone.MUTED)});
        JTable table = AppTable.create(model);
        row.add(AppTable.inCard(table), "h 260!");

        Carousel carousel = new Carousel(400, 260);
        carousel.setPhotos(List.of("res:images/seed/highland-1.png", "res:images/seed/highland-2.png",
                "res:images/seed/highland-3.png", "missing-photo.png"));
        row.add(carousel, "h 260!");
        page.add(row);

        openDialog.addActionListener(e -> {
            AppDialog dialog = new AppDialog(frame, "Add item");
            dialog.getBody().add(Ui.field("Name", Ui.textField("For example: Bath towel")));
            dialog.getBody().add(Ui.field("Category", new JComboBox<>(new String[]{"Consumable", "Equipment"})));
            dialog.getBody().add(Ui.error("Enter a name."));
            dialog.addCloseButton("Cancel");
            AppButton save = new AppButton("Add item", AppButton.Style.PRIMARY);
            save.addActionListener(ev -> dialog.dispose());
            dialog.addButton(save);
            dialog.showDialog(420);
        });

        return Ui.scroll(page);
    }
}
