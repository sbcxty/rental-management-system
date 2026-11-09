package com.gudosnest.frontend.components;

import com.gudosnest.backend.model.User;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;
import com.formdev.flatlaf.util.UIScale;
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
 * The one sidebar used by every page, in both portals (layout taken from the Stitch designs).
 * Collapsed: 72px, icons only. Expanded: 240px, icons and labels; it pushes the page content.
 * The Burgundy logo square at the top is the expand/collapse button. The avatar at the bottom
 * opens a small menu: name and email, "Switch to ... portal" (two-role accounts only), "Log out".
 *
 * Spacing: 12px padding on both sides, so every row is 48px wide when collapsed and the
 * icons sit in the middle with room on the left and the right.
 *
 * HiDPI: MigLayout grows gaps and sizes with the Windows display scale (for example 125%),
 * so the sidebar width and everything painted here are scaled the same way with UIScale.
 */
public class Sidebar extends JPanel {

    public static final int COLLAPSED_WIDTH = 72;
    public static final int EXPANDED_WIDTH = 240;

    private static final int PAD = 12;      // left and right padding
    private static final int ROW = 48;      // logo size, and nav row width when collapsed
    private static final int NAV_HEIGHT = 44;
    private static final int ICON = 20;

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
    private final JPanel logoLabels;
    private final JPanel nameLabels;
    private boolean expanded;

    /**
     * @param portalName  "Owner portal" or "Renter portal", shown under the logo when expanded
     * @param switchLabel "Switch to Renter portal", or null if the user has only one role
     */
    public Sidebar(String portalName, List<Item> items, User user, String switchLabel, Listener listener) {
        // The right inset is 1px smaller because the 1px right border takes that space.
        super(new MigLayout("insets 20 " + PAD + " 16 " + (PAD - 1) + ", gap 0, fillx, wrap 1",
                "[grow,fill]", ""));
        setBackground(Colors.SIDEBAR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colors.BORDER));

        // Logo (also the expand button). Labels use "gapleft" + hidemode 3, so when they are
        // hidden their gap disappears too and nothing pushes the icons to the right.
        JPanel logoRow = Ui.transparent("insets 0, gap 0", "[][grow,fill]");
        LogoButton logo = new LogoButton();
        logo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setExpanded(!expanded);
            }
        });
        logoLabels = Ui.transparent("insets 0, gap 0, wrap 1", "[grow,fill]");
        logoLabels.add(Ui.label("Gudo's Nest", Fonts.logo(), Colors.TEXT), "wmin 0");
        logoLabels.add(Ui.label(portalName, Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        logoRow.add(logo, "w " + ROW + "!, h " + ROW + "!");
        logoRow.add(logoLabels, "gapleft 12, hidemode 3, wmin 0");
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
            add(button, "h " + NAV_HEIGHT + "!, gapbottom 4");
        }

        // Profile at the bottom
        add(new JLabel(), "pushy, growy");
        JPanel line = new JPanel();
        line.setBackground(Colors.SIDEBAR_DIVIDER);
        add(line, "h 1!, gapbottom 16");

        JPanel profileRow = Ui.transparent("insets 0, gap 0", "[][grow,fill]");
        Avatar avatar = new Avatar(user.getInitials(), 40);
        nameLabels = Ui.transparent("insets 0, gap 0, wrap 1", "[grow,fill]");
        nameLabels.add(Ui.label(user.getFullName(), Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
        nameLabels.add(Ui.label(user.getEmail(), Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
        profileRow.add(avatar, "w 40!, h 40!, gapleft 4");
        profileRow.add(nameLabels, "gapleft 12, hidemode 3, wmin 0");
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
        logoLabels.setVisible(expanded);
        nameLabels.setVisible(expanded);
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
        return new Dimension(UIScale.scale(expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH), super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    // ----------------------------------------------------------------------------------------

    /** The Burgundy square with a house icon. Clicking it expands or collapses the sidebar. */
    private final class LogoButton extends JComponent {
        private final Icon icon = Icons.get("house", 22, Colors.TEXT_ON_DARK);

        LogoButton() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Expand or collapse menu");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Colors.SIDEBAR_LOGO);
            int arc = UIScale.scale(Theme.RADIUS + 4);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
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
            this.normalIcon = Icons.get(item.icon(), ICON, Colors.SIDEBAR_TEXT);
            this.activeIcon = Icons.get(item.icon(), ICON, Colors.TEXT_ON_DARK);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                int arc = UIScale.scale(Theme.RADIUS);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            }

            // The icon is centred in a collapsed-width row, so it doesn't move when the sidebar expands.
            Icon icon = active ? activeIcon : normalIcon;
            int iconSize = icon.getIconWidth();
            int iconX = (UIScale.scale(ROW) - iconSize) / 2;
            int iconY = (getHeight() - iconSize) / 2;
            icon.paintIcon(this, g2, iconX, iconY);

            if (unread) {
                // Small dot on the icon's top-right corner, with a ring so it stands out.
                int dot = UIScale.scale(8);
                int ring = UIScale.scale(2);
                int dotX = iconX + iconSize - UIScale.scale(5);
                int dotY = iconY - UIScale.scale(3);
                g2.setColor(active ? Colors.SIDEBAR_ACTIVE : Colors.SIDEBAR);
                g2.fillOval(dotX - ring, dotY - ring, dot + 2 * ring, dot + 2 * ring);
                g2.setColor(active ? Colors.LIGHT_BLUE : Colors.PRIMARY);
                g2.fillOval(dotX, dotY, dot, dot);
            }

            if (expanded) {
                g2.setFont(active ? Fonts.bodyMedium() : Fonts.body());
                g2.setColor(active ? Colors.TEXT_ON_DARK : (hover ? Colors.TEXT : Colors.SIDEBAR_TEXT));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(item.label(), iconX + iconSize + UIScale.scale(12), (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}
