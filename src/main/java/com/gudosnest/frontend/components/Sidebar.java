package com.gudosnest.frontend.components;

import com.gudosnest.backend.model.User;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The one sidebar used by every page, in both portals.
 * Collapsed: 72px, icons only. Expanded: 240px, icons and labels; it pushes the page content.
 * The logo square at the top is the expand/collapse button. The avatar at the bottom opens a
 * small menu: name and email, "Switch to ... portal" (two-role accounts only) and "Log out".
 */
public class Sidebar extends JPanel {

    public static final int COLLAPSED_WIDTH = 72;
    public static final int EXPANDED_WIDTH = 240;

    /** One navigation entry, for example new Item("messages", "Messages", "message-square"). */
    public record Item(String id, String label, String icon) {
    }

    /** What the sidebar asks its portal to do. */
    public interface Listener {
        void onNavigate(String itemId);

        void onSwitchPortal();

        void onLogOut();
    }

    private final Map<String, NavButton> buttons = new LinkedHashMap<>();
    private final List<Consumer<Boolean>> expandListeners = new ArrayList<>();
    private final JLabel logoText;
    private final JLabel portalText;
    private final JLabel userName;
    private final JLabel userEmail;
    private boolean expanded;

    /**
     * @param portalName  "Owner portal" or "Renter portal", shown under the logo when expanded
     * @param switchLabel "Switch to Renter portal", or null if the user has only one role
     */
    public Sidebar(String portalName, List<Item> items, User user, String switchLabel, Listener listener) {
        super(new MigLayout("insets 16, gap 0, fillx, wrap 1", "[grow,fill]", ""));
        setBackground(Colors.SIDEBAR);

        // Logo (also the expand button)
        JPanel logoRow = Ui.transparent("insets 0, gap 12", "[][]");
        LogoButton logo = new LogoButton();
        logo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setExpanded(!expanded);
            }
        });
        logoText = Ui.label("Gudo's Nest", Fonts.logo(), Colors.TEXT_ON_DARK);
        portalText = Ui.label(portalName, Fonts.caption(), Colors.SIDEBAR_TEXT);
        JPanel logoLabels = Ui.transparent("insets 0, gap 0, wrap 1", "");
        logoLabels.add(logoText);
        logoLabels.add(portalText);
        logoRow.add(logo, "w 40!, h 40!");
        logoRow.add(logoLabels, "hidemode 3");
        add(logoRow, "gapbottom 24");

        // Navigation
        for (Item item : items) {
            NavButton button = new NavButton(item);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    listener.onNavigate(item.id());
                }
            });
            buttons.put(item.id(), button);
            add(button, "h 44!, gapbottom 4");
        }

        // Profile at the bottom
        add(new JLabel(), "pushy, growy");
        JPanel line = new JPanel();
        line.setBackground(Colors.SIDEBAR_DIVIDER);
        add(line, "h 1!, gapbottom 16");

        JPanel profileRow = Ui.transparent("insets 0, gap 12", "[][grow,fill]");
        Avatar avatar = new Avatar(user.getInitials(), 40);
        userName = Ui.label(user.getFullName(), Fonts.bodyMedium(), Colors.TEXT_ON_DARK);
        userEmail = Ui.label(user.getEmail(), Fonts.caption(), Colors.SIDEBAR_TEXT);
        JPanel nameLabels = Ui.transparent("insets 0, gap 0, wrap 1", "[grow,fill]");
        nameLabels.add(userName, "wmin 0");
        nameLabels.add(userEmail, "wmin 0");
        profileRow.add(avatar, "w 40!, h 40!");
        profileRow.add(nameLabels, "hidemode 3, wmin 0");
        profileRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileRow.setToolTipText("Account");

        JPopupMenu menu = buildProfileMenu(user, switchLabel, listener);
        MouseAdapter openMenu = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Dimension size = menu.getPreferredSize();
                menu.show(profileRow, 0, -size.height - Theme.S);
            }
        };
        profileRow.addMouseListener(openMenu);
        avatar.addMouseListener(openMenu);
        add(profileRow);

        setExpanded(false);
    }

    private JPopupMenu buildProfileMenu(User user, String switchLabel, Listener listener) {
        JPopupMenu menu = new JPopupMenu();
        JPanel header = new JPanel(new MigLayout("insets 8 12 8 12, gap 0, wrap 1", ""));
        header.setOpaque(false);
        header.add(Ui.bold(user.getFullName()));
        header.add(Ui.caption(user.getEmail()));
        menu.add(header);
        menu.addSeparator();
        if (switchLabel != null) {
            JMenuItem switchItem = new JMenuItem(switchLabel, Icons.get("arrow-left-right", 16, Colors.TEXT));
            switchItem.addActionListener(e -> listener.onSwitchPortal());
            menu.add(switchItem);
        }
        JMenuItem logOut = new JMenuItem("Log out", Icons.get("log-out", 16, Colors.TEXT));
        logOut.addActionListener(e -> listener.onLogOut());
        menu.add(logOut);
        return menu;
    }

    /** Highlights the current page. */
    public void setActive(String itemId) {
        buttons.forEach((id, button) -> button.setActive(id.equals(itemId)));
    }

    /** Shows or hides the small unread dot (used on Messages). */
    public void setUnread(String itemId, boolean unread) {
        NavButton button = buttons.get(itemId);
        if (button != null) {
            button.setUnread(unread);
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        logoText.getParent().setVisible(expanded);
        userName.getParent().setVisible(expanded);
        buttons.values().forEach(b -> b.setToolTipText(expanded ? null : b.item.label()));
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
        }
        expandListeners.forEach(l -> l.accept(expanded));
    }

    /** Pages that change layout with the sidebar width (Messages) listen here. */
    public void onExpandChange(Consumer<Boolean> listener) {
        expandListeners.add(listener);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH, super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    // ----------------------------------------------------------------------------------------

    /** The Primary square with a house icon. Clicking it expands or collapses the sidebar. */
    private final class LogoButton extends JComponent {
        private final Icon icon = Icons.get("house", 20, Colors.TEXT_ON_DARK);

        LogoButton() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Expand or collapse menu");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Colors.PRIMARY);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS, Theme.RADIUS);
            icon.paintIcon(this, g2, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);
            g2.dispose();
        }
    }

    /** One navigation row: icon, label (when expanded), active fill, hover fill, unread dot. */
    private final class NavButton extends JComponent {
        private final Item item;
        private final Icon normalIcon;
        private final Icon activeIcon;
        private boolean active;
        private boolean hover;
        private boolean unread;

        NavButton(Item item) {
            this.item = item;
            this.normalIcon = Icons.get(item.icon(), 20, Colors.SIDEBAR_TEXT);
            this.activeIcon = Icons.get(item.icon(), 20, Colors.TEXT_ON_DARK);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        void setUnread(boolean unread) {
            this.unread = unread;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (active || hover) {
                g2.setColor(active ? Colors.SIDEBAR_ACTIVE : Colors.SIDEBAR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.CONTROL_RADIUS * 2, Theme.CONTROL_RADIUS * 2);
            }

            Icon icon = active ? activeIcon : normalIcon;
            int iconX = 10;   // centers a 20px icon in the 40px collapsed row
            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, iconX, iconY);

            if (unread) {
                g2.setColor(Colors.LIGHT_BLUE);
                g2.fillOval(iconX + 15, iconY - 3, 8, 8);
            }

            if (expanded) {
                g2.setFont(active ? Fonts.bodyMedium() : Fonts.body());
                g2.setColor(active ? Colors.TEXT_ON_DARK : Colors.SIDEBAR_TEXT);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(item.label(), iconX + 20 + 14, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}
