package com.gudosnest.frontend.components;

import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.theme.Colors;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sidebar on the left, the current page on the right. Because the sidebar sits in
 * BorderLayout.WEST, expanding it pushes the page instead of covering it.
 */
public class PortalShell extends JPanel {

    private final Sidebar sidebar;
    private final JPanel pageArea = new JPanel(new BorderLayout());
    private BasePage current;

    // DSA: Stack
    // Drill-down navigation (Properties → a property's units → a unit's inventory) pushes the page
    // being left; "Back" pops it, so the person returns to exactly the page they came from, in
    // reverse order (last in, first out). Choosing a sidebar item starts a fresh history.
    private final Deque<BasePage> history = new ArrayDeque<>();

    public PortalShell(Sidebar sidebar) {
        super(new BorderLayout());
        this.sidebar = sidebar;
        setBackground(Colors.PAGE);
        pageArea.setBackground(Colors.PAGE);
        pageArea.setFocusable(true);   // holds focus so no field looks "selected" when a page opens
        add(sidebar, BorderLayout.WEST);
        add(pageArea, BorderLayout.CENTER);
    }

    /** Shows a top-level page (from the sidebar) and clears the back history. */
    public void showPage(String sidebarItemId, BasePage page) {
        history.clear();
        sidebar.setActive(sidebarItemId);
        display(page);
    }

    /** Opens a page on top of the current one; {@link #back()} returns to the current one. */
    public void push(BasePage page) {
        if (current != null) {
            history.push(current);
        }
        display(page);
    }

    /** Returns to the previous page and reloads it so it shows any changes made meanwhile. */
    public void back() {
        if (history.isEmpty()) {
            return;
        }
        BasePage previous = history.pop();
        previous.reload();
        display(previous);
    }

    public BasePage getCurrentPage() {
        return current;
    }

    private void display(BasePage page) {
        current = page;
        JComponent view = page.scrolls() ? Ui.scroll(page) : page;
        pageArea.removeAll();
        pageArea.add(view, BorderLayout.CENTER);
        pageArea.revalidate();
        pageArea.repaint();
        pageArea.requestFocusInWindow();
    }

    public Sidebar getSidebar() {
        return sidebar;
    }
}
