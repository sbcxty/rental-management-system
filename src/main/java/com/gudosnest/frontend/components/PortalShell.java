package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Sidebar on the left, the current page on the right. Because the sidebar sits in
 * BorderLayout.WEST, expanding it pushes the page instead of covering it.
 */
public class PortalShell extends JPanel {

    private final Sidebar sidebar;
    private final JPanel pageArea = new JPanel(new BorderLayout());

    public PortalShell(Sidebar sidebar) {
        super(new BorderLayout());
        this.sidebar = sidebar;
        setBackground(Colors.PAGE);
        pageArea.setBackground(Colors.PAGE);
        add(sidebar, BorderLayout.WEST);
        add(pageArea, BorderLayout.CENTER);
    }

    /** Replaces the page shown on the right and highlights its sidebar item. */
    public void showPage(String sidebarItemId, JComponent page) {
        pageArea.removeAll();
        pageArea.add(page, BorderLayout.CENTER);
        sidebar.setActive(sidebarItemId);
        pageArea.revalidate();
        pageArea.repaint();
    }

    public Sidebar getSidebar() {
        return sidebar;
    }
}
