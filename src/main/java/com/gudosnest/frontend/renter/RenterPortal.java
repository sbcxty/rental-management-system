package com.gudosnest.frontend.renter;

import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.model.User;
import com.gudosnest.frontend.MainWindow;
import com.gudosnest.frontend.components.Sidebar;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.MessagesPage;

import java.util.List;

/** The renter's portal: browse and book, booking history, messages, cancellations, damage reports. */
public class RenterPortal extends Portal {

    private static final List<Sidebar.Item> MENU = List.of(
            new Sidebar.Item("browse", "Browse", "search"),
            new Sidebar.Item("history", "Booking history", "history"),
            new Sidebar.Item("messages", "Messages", "message-square"),
            new Sidebar.Item("cancellations", "Cancellations", "calendar-x"),
            new Sidebar.Item("damage", "Damage reports", "triangle-alert"));

    public RenterPortal(MainWindow window, User user) {
        super(window, user);
    }

    @Override
    protected BasePage createPage(String itemId) {
        return switch (itemId) {
            case "history" -> new BookingHistoryPage(this);
            case "messages" -> new MessagesPage(this);
            case "cancellations" -> new CancellationsPage(this, null);
            case "damage" -> new RenterDamagePage(this);
            default -> new BrowsePage(this);
        };
    }

    /** Opens Cancellations with this booking already selected (from the booking dialog). */
    public void openCancellation(int bookingId) {
        BasePage page = new CancellationsPage(this, bookingId);
        page.reload();
        shellShow("cancellations", page);
    }

    /** A dot on Damage reports while any report is unpaid. */
    @Override
    protected void refreshOtherBadges() {
        sidebar().setUnread("damage", damage.forRenter(user().getId()).stream().anyMatch(r -> r.isOpen()));
    }

    @Override
    protected List<Sidebar.Item> menuItems() {
        return MENU;
    }

    @Override
    public SessionManager.Portal kind() {
        return SessionManager.Portal.RENTER;
    }

    @Override
    protected String portalName() {
        return "Renter portal";
    }
}
