package com.gudosnest.frontend.portal;

import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.User;
import com.gudosnest.backend.service.BookingService;
import com.gudosnest.backend.service.FinanceService;
import com.gudosnest.backend.service.MessageService;
import com.gudosnest.backend.service.OwnerService;
import com.gudosnest.frontend.MainWindow;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.PortalShell;
import com.gudosnest.frontend.components.Sidebar;
import com.gudosnest.frontend.owner.OwnerPortal;
import com.gudosnest.frontend.renter.RenterPortal;
import com.gudosnest.frontend.shared.MessagesPage;

import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.util.List;

// PATTERN: Factory Method
// The app opens a portal without knowing which kind it is: Portal.create(...) returns an
// OwnerPortal or a RenterPortal, and the shared code here (sidebar, navigation, back history,
// unread badge) calls the factory method createPage(id). Each subclass decides which page class
// to build for each sidebar item, for example "messages" or "browse". Adding a new kind of portal
// means adding a subclass, not editing this class.
public abstract class Portal {

    protected final MainWindow window;
    protected final User user;

    // Services every page uses (each one is stateless, so one per portal is enough)
    public final BookingService bookings = new BookingService();
    public final OwnerService owner = new OwnerService();
    public final FinanceService finance = new FinanceService();
    public final MessageService messages = new MessageService();

    private Sidebar sidebar;
    private PortalShell shell;

    /** How often statuses are brought up to date while the portal is open (auto check-out etc.). */
    private static final int STATUS_REFRESH_MS = 3 * 60 * 1000;
    private Timer statusTimer;
    private boolean refreshRunning;
    private boolean reloadPending;

    protected Portal(MainWindow window, User user) {
        this.window = window;
        this.user = user;
    }

    /** Picks the portal class for the role the person is using. */
    public static Portal create(MainWindow window, User user, SessionManager.Portal kind) {
        return kind == SessionManager.Portal.OWNER ? new OwnerPortal(window, user) : new RenterPortal(window, user);
    }

    /** The factory method: builds the page for one sidebar item. */
    protected abstract BasePage createPage(String itemId);

    protected abstract List<Sidebar.Item> menuItems();

    public abstract SessionManager.Portal kind();

    protected abstract String portalName();

    /** Builds the sidebar and shell and opens the first page. */
    public JComponent build() {
        SessionManager.Portal other = kind() == SessionManager.Portal.OWNER
                ? SessionManager.Portal.RENTER : SessionManager.Portal.OWNER;
        String switchLabel = user.hasBothRoles()
                ? (other == SessionManager.Portal.OWNER ? "Switch to Owner portal" : "Switch to Renter portal") : null;
        sidebar = new Sidebar(portalName(), menuItems(), user, switchLabel, new Sidebar.Listener() {
            @Override
            public void onNavigate(String itemId) {
                navigate(itemId);
            }

            @Override
            public void onSwitchPortal() {
                window.showPortal(user, other);
            }

            @Override
            public void onLogOut() {
                if (AppDialog.confirm(window, "Log out of Gudo's Nest?", "Log out")) {
                    window.logOut();
                }
            }
        });
        shell = new PortalShell(sidebar);
        navigate(menuItems().get(0).id());
        statusTimer = new Timer(STATUS_REFRESH_MS, e -> refreshStatusesInBackground());
        statusTimer.start();
        return shell;
    }

    /** Opens a sidebar page (starts a fresh back history). */
    public void navigate(String itemId) {
        refreshStatuses();
        BasePage page = createPage(itemId);
        page.reload();
        shell.showPage(itemId, page);
        refreshBadges();
    }

    /** Shows an already built page as the top-level page of a sidebar item. */
    protected void shellShow(String itemId, BasePage page) {
        shell.showPage(itemId, page);
        refreshBadges();
    }

    /** Opens a page on top of the current one; its Back link returns here. */
    public void open(BasePage page) {
        page.reload();
        shell.push(page);
    }

    public void back() {
        shell.back();
    }

    /** Opens Messages with the conversation with this person selected (created if new). */
    public void openConversation(int otherUserId, String otherName, Booking booking) {
        MessagesPage page = new MessagesPage(this, otherUserId, otherName, booking);
        page.reload();
        shell.showPage("messages", page);
        refreshBadges();
    }

    /** Expires overdue pending bookings and updates occupancy (see BookingService). */
    public void refreshStatuses() {
        try {
            bookings.refreshStatuses();
        } catch (DataException e) {
            // pages show their own error when they can't load
        }
    }

    /**
     * Timer tick: runs {@link BookingService#refreshStatuses()} off the Event Dispatch Thread, then
     * reloads the open page if anything changed. Stops itself once this portal is closed (log out
     * or switching portals replaces the window's content).
     */
    private void refreshStatusesInBackground() {
        if (!shell.isDisplayable()) {
            statusTimer.stop();
            return;
        }
        if (refreshRunning) {
            return;
        }
        refreshRunning = true;
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    return bookings.refreshStatuses();
                } catch (DataException e) {
                    return false;   // pages show their own error when they can't load
                }
            }

            @Override
            protected void done() {
                refreshRunning = false;
                try {
                    reloadPending |= get();
                } catch (Exception e) {
                    // nothing changed that we know of
                }
                reloadCurrentPageIfSafe();
            }
        }.execute();
    }

    /**
     * Reloads the open page after a background status change. Waits (until the next tick) while a
     * dialog is open, and leaves Messages alone so a half-typed message isn't lost.
     */
    private void reloadCurrentPageIfSafe() {
        BasePage page = shell.getCurrentPage();
        if (!reloadPending || page == null || !shell.isDisplayable()) {
            return;
        }
        if (java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()
                instanceof java.awt.Dialog) {
            return;
        }
        reloadPending = false;
        if (!(page instanceof MessagesPage)) {
            page.reload();
        }
    }

    /** Updates the unread dot on Messages. */
    public void refreshBadges() {
        try {
            sidebar.setUnread("messages", messages.hasUnread(user.getId(), kind()));
        } catch (DataException e) {
            sidebar.setUnread("messages", false);
        }
    }

    public User user() {
        return user;
    }

    public MainWindow window() {
        return window;
    }

    public Sidebar sidebar() {
        return sidebar;
    }
}
