package com.gudosnest.frontend.owner;

import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.model.User;
import com.gudosnest.frontend.MainWindow;
import com.gudosnest.frontend.components.Sidebar;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
// import com.gudosnest.frontend.shared.MessagesPage;

import java.util.List;

/** The owner's portal: dashboard, properties, bookings, payments, finances, messages, damage reports. */
public class OwnerPortal extends Portal {

    private static final List<Sidebar.Item> MENU = List.of(
            new Sidebar.Item("dashboard", "Dashboard", "layout-dashboard"),
            new Sidebar.Item("properties", "Properties", "building-2"),
            new Sidebar.Item("bookings", "Bookings", "calendar-days"),
            new Sidebar.Item("payments", "Payments", "credit-card"),
            new Sidebar.Item("finances", "Finances", "wallet"),
            new Sidebar.Item("messages", "Messages", "message-square"),
            new Sidebar.Item("damage", "Damage reports", "triangle-alert"));

    public OwnerPortal(MainWindow window, User user) {
        super(window, user);
    }

    @Override
    protected BasePage createPage(String itemId) {
        return switch (itemId) {
            case "properties" -> new PropertiesPage(this);
            case "bookings" -> new BookingsPage(this);
            case "payments" -> new PaymentsPage(this);
            // case "finances" -> new FinancesPage(this);
            // case "messages" -> new MessagesPage(this);
            // case "damage" -> new DamageReportsPage(this);
            default -> new DashboardPage(this);
        };
    }

    @Override
    protected List<Sidebar.Item> menuItems() {
        return MENU;
    }

    @Override
    public SessionManager.Portal kind() {
        return SessionManager.Portal.OWNER;
    }

    @Override
    protected String portalName() {
        return "Owner portal";
    }
}
