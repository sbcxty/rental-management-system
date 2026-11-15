package com.gudosnest.frontend.auth;

import com.gudosnest.backend.model.User;
import com.gudosnest.frontend.theme.Colors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The sign-out screens, drawn exactly as in the LogoutFrame design: a confirmation card
 * ("Sign out of your account") and, after signing out, "You have been signed out".
 * It fills the whole window, so it carries the design's own 72px icon sidebar.
 *
 * It knows nothing about the database: whoever opens it passes the signed-in user, the booking
 * line and what each button does (see Portal.showLogout and MainWindow).
 */
public class LogoutScreen extends JPanel {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private final User user;
    private final String activeLabel;
    private final String activeValue;
    private final Consumer<String> navigate;   // sidebar icon -> portal page id; null hides the nav icons
    private final Runnable backToPortal;
    private final Runnable endSession;
    private final Runnable closeWindow;
    private final Runnable signInAgain;

    private CardLayout cardLayout;
    private JPanel cardHost;
    private JLabel signedOutSubtitle;

    /**
     * @param activeLabel  left text of the last session row, e.g. "Active booking"
     * @param activeValue  right text of that row, e.g. "Ref #GN-2026-8841" or "None"
     * @param navigate     opens a renter page (browse, history, messages, cancellations); null for owners
     * @param backToPortal Cancel: returns to the page the person was on
     * @param endSession   Sign out: ends the login session (the card then switches to "signed out")
     * @param closeWindow  Close window
     * @param signInAgain  Sign in again: shows the login screen
     */
    public LogoutScreen(User user, String activeLabel, String activeValue, Consumer<String> navigate,
                        Runnable backToPortal, Runnable endSession, Runnable closeWindow, Runnable signInAgain) {
        super(new BorderLayout());
        this.user = user;
        this.activeLabel = activeLabel;
        this.activeValue = activeValue;
        this.navigate = navigate;
        this.backToPortal = backToPortal;
        this.endSession = endSession;
        this.closeWindow = closeWindow;
        this.signInAgain = signInAgain;

        setBackground(Theme.PAGE_BG);

        // 1. Left Navigation Sidebar (Bottom user profile active)
        add(createSidebar(), BorderLayout.WEST);

        // 2. Centered Canvas Area on Light Lavender (#EAE7EE)
        JPanel centerCanvas = new JPanel(new GridBagLayout());
        centerCanvas.setBackground(Theme.PAGE_BG);
        centerCanvas.setBorder(new EmptyBorder(24, 24, 24, 24));

        cardLayout = new CardLayout();
        cardHost = new JPanel(cardLayout);
        cardHost.setOpaque(false);

        cardHost.add(createConfirmSignOutCard(), "CONFIRM");
        cardHost.add(createSignedOutSuccessCard(), "SIGNED_OUT");

        centerCanvas.add(cardHost);
        add(centerCanvas, BorderLayout.CENTER);
    }

    /** Makes a sidebar icon open a page when clicked. */
    private SidebarIcon link(SidebarIcon icon, String pageId) {
        if (navigate != null) {
            icon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigate.accept(pageId);
                }
            });
        }
        return icon;
    }

    // =========================================================================
    // 1. LEFT SIDEBAR (72px width, dead-center icons, User Avatar active)
    // =========================================================================
    private JComponent createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(72, 0));
        sidebar.setMinimumSize(new Dimension(72, 0));
        sidebar.setBackground(Theme.CARD_WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER_LAVENDER));

        JPanel topIcons = new JPanel(new GridBagLayout());
        topIcons.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // Home Brand Icon
        gbc.gridy = 0;
        gbc.insets = new Insets(18, 0, 22, 0);
        topIcons.add(link(new SidebarIcon(IconType.HOME, false, true), "browse"), gbc);

        // The page icons only exist for renters (the owner portal has different pages)
        if (navigate != null) {
            // Search
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 12, 0);
            topIcons.add(link(new SidebarIcon(IconType.SEARCH, false, false), "browse"), gbc);

            // Bookings Calendar
            gbc.gridy = 2;
            topIcons.add(link(new SidebarIcon(IconType.CALENDAR, false, false), "history"), gbc);

            // Messages Chat
            gbc.gridy = 3;
            topIcons.add(link(new SidebarIcon(IconType.CHAT, false, false), "messages"), gbc);

            // Cancellations
            gbc.gridy = 4;
            topIcons.add(link(new SidebarIcon(IconType.CANCEL_CAL, false, false), "cancellations"), gbc);
        }

        sidebar.add(topIcons, BorderLayout.NORTH);

        // Bottom Active Profile/Account Icon
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 18, 0));
        bottomPanel.add(new SidebarIcon(IconType.USER_BADGE, true, false));
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    // =========================================================================
    // 2. CONFIRM SIGN OUT CARD
    // =========================================================================
    private JComponent createConfirmSignOutCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD_WHITE, Theme.BORDER_LAVENDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(460, 430));

        // Top Icon Badge
        RoundedPanel iconCircle = new RoundedPanel(48, Theme.PAGE_BG, Theme.BORDER_LAVENDER);
        iconCircle.setPreferredSize(new Dimension(48, 48));
        iconCircle.setMaximumSize(new Dimension(48, 48));
        iconCircle.setLayout(new GridBagLayout());
        iconCircle.setAlignmentX(Component.LEFT_ALIGNMENT);
        iconCircle.add(new VectorIconLabel(IconType.LOGOUT, Theme.PRIMARY, 20, 20));

        // Heading & Plain Description
        JLabel title = new JLabel("Sign out of your account");
        title.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 20));
        title.setForeground(Theme.BURGUNDY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel(
                "<html><body style='width:370px'>Your active reservation details and message history remain saved. You can sign back in anytime.</body></html>"
        );
        desc.setFont(Theme.FONT_BODY);
        desc.setForeground(Theme.NEUTRAL_TEXT);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Current Session Summary Box
        RoundedPanel sessionBox = new RoundedPanel(12, Theme.PAGE_BG, Theme.BORDER_LAVENDER);
        sessionBox.setLayout(new BoxLayout(sessionBox, BoxLayout.Y_AXIS));
        sessionBox.setBorder(new EmptyBorder(14, 16, 14, 16));
        sessionBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // User Row
        JPanel userRow = new JPanel(new BorderLayout(12, 0));
        userRow.setOpaque(false);
        userRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        userRow.add(new AvatarBadge(user.getInitials().substring(0, 1), 36, Theme.BURGUNDY), BorderLayout.WEST);

        JPanel userMeta = new JPanel(new GridLayout(2, 1, 0, 2));
        userMeta.setOpaque(false);
        JLabel nameLbl = new JLabel(user.getFullName());
        nameLbl.setFont(Theme.FONT_BODY_BOLD);
        nameLbl.setForeground(Theme.BURGUNDY);
        JLabel emailLbl = new JLabel(user.getEmail());
        emailLbl.setFont(Theme.FONT_SMALL);
        emailLbl.setForeground(Theme.NEUTRAL_TEXT);
        userMeta.add(nameLbl);
        userMeta.add(emailLbl);

        RoundedPanel activeTag = new RoundedPanel(10, Theme.CARD_WHITE, Theme.BORDER_LAVENDER);
        activeTag.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 3));
        JLabel tagLbl = new JLabel("Signed in");
        tagLbl.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 10));
        tagLbl.setForeground(Theme.TERTIARY);
        activeTag.add(tagLbl);

        JPanel tagWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        tagWrap.setOpaque(false);
        tagWrap.add(activeTag);

        userRow.add(userMeta, BorderLayout.CENTER);
        userRow.add(tagWrap, BorderLayout.EAST);

        // Divider inside session box
        JPanel innerLine = new JPanel();
        innerLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        innerLine.setPreferredSize(new Dimension(10, 1));
        innerLine.setBackground(Theme.BORDER_LAVENDER);

        // Metadata Rows (Standardized Date, 12-hr Time, and Ref # formats)
        JPanel rowLastLogin = createMetaRow("Last activity", STAMP.format(LocalDateTime.now()));
        JPanel rowActiveRef = createMetaRow(activeLabel, activeValue);

        sessionBox.add(userRow);
        sessionBox.add(Box.createVerticalStrut(12));
        sessionBox.add(innerLine);
        sessionBox.add(Box.createVerticalStrut(10));
        sessionBox.add(rowLastLogin);
        sessionBox.add(Box.createVerticalStrut(6));
        sessionBox.add(rowActiveRef);

        // Action Buttons Row (Outlined Cancel + Primary Sign Out)
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton cancelBtn = Theme.createButton("Cancel", Theme.ButtonStyle.OUTLINED, 12);
        cancelBtn.addActionListener(e -> {
            backToPortal.run();
            JOptionPane.showMessageDialog(
                    this,
                    "Returned to active session.",
                    "Session Active",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        JButton signOutBtn = Theme.createButton("Sign out", Theme.ButtonStyle.PRIMARY, 12);
        signOutBtn.addActionListener(e -> {
            endSession.run();
            signedOutSubtitle.setText("<html><body style='width:370px'>Session ended on " + STAMP.format(LocalDateTime.now())
                    + ". Close this window or sign back in to manage your stays.</body></html>");
            cardLayout.show(cardHost, "SIGNED_OUT");
        });

        btnRow.add(cancelBtn);
        btnRow.add(signOutBtn);

        card.add(iconCircle);
        card.add(Box.createVerticalStrut(18));
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(desc);
        card.add(Box.createVerticalStrut(20));
        card.add(sessionBox);
        card.add(Box.createVerticalStrut(24));
        card.add(btnRow);

        return card;
    }

    // =========================================================================
    // 3. SIGNED OUT CONFIRMATION STATE CARD
    // =========================================================================
    private JComponent createSignedOutSuccessCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD_WHITE, Theme.BORDER_LAVENDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 36, 36, 36));
        card.setPreferredSize(new Dimension(460, 310));

        RoundedPanel checkCircle = new RoundedPanel(48, Theme.PAGE_BG, Theme.BORDER_LAVENDER);
        checkCircle.setPreferredSize(new Dimension(48, 48));
        checkCircle.setMaximumSize(new Dimension(48, 48));
        checkCircle.setLayout(new GridBagLayout());
        checkCircle.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkCircle.add(new VectorIconLabel(IconType.CHECK, Theme.PRIMARY, 20, 20));

        JLabel title = new JLabel("You have been signed out");
        title.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 20));
        title.setForeground(Theme.BURGUNDY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "<html><body style='width:370px'>Session ended. Close this window or sign back in to manage your stays.</body></html>"
        );
        signedOutSubtitle = subtitle;
        subtitle.setFont(Theme.FONT_BODY);
        subtitle.setForeground(Theme.NEUTRAL_TEXT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton closeBtn = Theme.createButton("Close window", Theme.ButtonStyle.OUTLINED, 12);
        closeBtn.addActionListener(e -> closeWindow.run());

        JButton signInAgainBtn = Theme.createButton("Sign in again", Theme.ButtonStyle.PRIMARY, 12);
        signInAgainBtn.addActionListener(e -> signInAgain.run());

        btnRow.add(closeBtn);
        btnRow.add(signInAgainBtn);

        card.add(checkCircle);
        card.add(Box.createVerticalStrut(18));
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(btnRow);

        return card;
    }

    private JPanel createMetaRow(String key, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel kLbl = new JLabel(key);
        kLbl.setFont(Theme.FONT_SMALL);
        kLbl.setForeground(Theme.NEUTRAL_TEXT);

        JLabel vLbl = new JLabel(value);
        vLbl.setFont(Theme.FONT_SMALL_BOLD);
        vLbl.setForeground(Theme.BURGUNDY);

        row.add(kLbl, BorderLayout.WEST);
        row.add(vLbl, BorderLayout.EAST);
        return row;
    }

    // =========================================================================
    // CUSTOM FLAT COMPONENTS & VECTOR ICONS
    // =========================================================================
    private static class AvatarBadge extends JComponent {
        private final String initials;
        private final int size;
        private final Color bg;

        AvatarBadge(String initials, int size, Color bg) {
            this.initials = initials;
            this.size = size;
            this.bg = bg;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bg);
            g2.fillOval(0, 0, size, size);

            g2.setColor(Theme.CARD_WHITE);
            g2.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (size - fm.stringWidth(initials)) / 2;
            int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initials, tx, ty);

            g2.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;
        private final Color borderColor;

        RoundedPanel(int radius, Color bgColor, Color borderColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, radius, radius));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private enum IconType {
        HOME, SEARCH, CALENDAR, CHAT, CANCEL_CAL, USER_BADGE, LOGOUT, CHECK
    }

    private static class SidebarIcon extends JComponent {
        private final IconType type;
        private final boolean active;
        private final boolean darkBrand;

        SidebarIcon(IconType type, boolean active, boolean darkBrand) {
            this.type = type;
            this.active = active;
            this.darkBrand = darkBrand;
            setPreferredSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (darkBrand) {
                g2.setColor(Theme.BURGUNDY);
                g2.fill(new RoundRectangle2D.Float(0, 0, 42, 42, 12, 12));
            } else if (active) {
                g2.setColor(Theme.PRIMARY);
                if (type == IconType.USER_BADGE) {
                    g2.fillOval(3, 3, 36, 36);
                } else {
                    g2.fill(new RoundRectangle2D.Float(0, 0, 42, 42, 12, 12));
                }
            }

            Color iconColor = (darkBrand || active) ? Theme.CARD_WHITE : Theme.NEUTRAL_TEXT;
            new VectorIcon(type, iconColor, 18, 18).paintIcon(this, g2, 12, 12);

            g2.dispose();
        }
    }

    private static class VectorIconLabel extends JLabel {
        VectorIconLabel(IconType type, Color color, int w, int h) {
            super(new VectorIcon(type, color, w, h));
        }
    }

    private static class VectorIcon implements Icon {
        private final IconType type;
        private final Color color;
        private final int w;
        private final int h;

        VectorIcon(IconType type, Color color, int w, int h) {
            this.type = type;
            this.color = color;
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case HOME:
                    Path2D roof = new Path2D.Float();
                    roof.moveTo(2, h * 0.48);
                    roof.lineTo(w / 2.0, 2);
                    roof.lineTo(w - 2, h * 0.48);
                    g2.draw(roof);
                    g2.drawRect(4, (int) (h * 0.45), w - 8, (int) (h * 0.45));
                    break;
                case SEARCH:
                    int r = (int) (w * 0.65);
                    g2.drawOval(1, 1, r, r);
                    g2.drawLine(r, r, w - 2, h - 2);
                    break;
                case CALENDAR:
                    g2.drawRoundRect(2, 3, w - 4, h - 5, 3, 3);
                    g2.drawLine(2, 7, w - 2, 7);
                    g2.drawLine(5, 1, 5, 4);
                    g2.drawLine(w - 5, 1, w - 5, 4);
                    break;
                case CHAT:
                    g2.drawRoundRect(2, 2, w - 4, h - 6, 4, 4);
                    g2.drawLine(5, 6, w - 5, 6);
                    g2.drawLine(5, 9, w - 8, 9);
                    break;
                case CANCEL_CAL:
                    g2.drawRoundRect(2, 3, w - 4, h - 5, 3, 3);
                    g2.drawLine(5, 1, 5, 4);
                    g2.drawLine(w - 5, 1, w - 5, 4);
                    g2.drawLine(6, 7, w - 6, h - 5);
                    g2.drawLine(w - 6, 7, 6, h - 5);
                    break;
                case USER_BADGE:
                    g2.drawOval(w / 2 - 3, 2, 6, 6);
                    g2.drawArc(3, 10, w - 6, 8, 0, 180);
                    break;
                case LOGOUT:
                    // Door frame on left + exit arrow pointing right
                    Path2D door = new Path2D.Float();
                    door.moveTo(w * 0.45, 2);
                    door.lineTo(3, 2);
                    door.lineTo(3, h - 2);
                    door.lineTo(w * 0.45, h - 2);
                    g2.draw(door);
                    g2.drawLine((int) (w * 0.38), h / 2, w - 2, h / 2);
                    g2.drawLine(w - 6, h / 2 - 4, w - 2, h / 2);
                    g2.drawLine(w - 6, h / 2 + 4, w - 2, h / 2);
                    break;
                case CHECK:
                    Path2D chk = new Path2D.Float();
                    chk.moveTo(3, h * 0.52);
                    chk.lineTo(w * 0.42, h - 4);
                    chk.lineTo(w - 3, 4);
                    g2.draw(chk);
                    break;
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }


    // =========================================================================
    // THE DESIGN'S PALETTE AND BUTTON (same colors as the app's Colors class)
    // =========================================================================
    private static final class Theme {
        static final Color PRIMARY = Colors.PRIMARY;
        static final Color SECONDARY = Colors.SECONDARY;
        static final Color TERTIARY = Colors.TERTIARY;
        static final Color LIGHT_BLUE = Colors.LIGHT_BLUE;
        static final Color BURGUNDY = Colors.BURGUNDY;
        static final Color PAGE_BG = Colors.PAGE;
        static final Color CARD_WHITE = Colors.CARD;
        static final Color NEUTRAL_TEXT = Colors.NEUTRAL;
        static final Color BORDER_LAVENDER = Colors.BORDER;
        static final Color DESTRUCTIVE_RED = Colors.DANGER;

        static final String FONT_FAMILY = "SansSerif";
        static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 12);
        static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
        static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);
        static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 11);

        enum ButtonStyle {
            PRIMARY, SECONDARY, INVERTED, OUTLINED, DESTRUCTIVE
        }

        static JButton createButton(String text, ButtonStyle style, int radius) {
            return new FlatButton(text, style, radius);
        }

        static class FlatButton extends JButton {
            private final ButtonStyle style;
            private final int radius;
            private boolean hovered = false;

            FlatButton(String text, ButtonStyle style, int radius) {
                super(text);
                this.style = style;
                this.radius = radius;
                setFont(FONT_BODY_BOLD);
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setMargin(new Insets(8, 16, 8, 16));

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg;
                Color fg;
                Color borderCol = null;

                switch (style) {
                    case PRIMARY:
                        bg = hovered ? BURGUNDY : PRIMARY;
                        fg = CARD_WHITE;
                        break;
                    case SECONDARY:
                        bg = hovered ? PRIMARY : SECONDARY;
                        fg = CARD_WHITE;
                        break;
                    case INVERTED:
                        bg = hovered ? BORDER_LAVENDER : PAGE_BG;
                        fg = PRIMARY;
                        break;
                    case OUTLINED:
                        bg = hovered ? PAGE_BG : CARD_WHITE;
                        fg = NEUTRAL_TEXT;
                        borderCol = BORDER_LAVENDER;
                        break;
                    case DESTRUCTIVE:
                    default:
                        bg = DESTRUCTIVE_RED;
                        fg = CARD_WHITE;
                        break;
                }

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));

                if (borderCol != null) {
                    g2.setColor(borderCol);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, radius, radius));
                }

                g2.dispose();
                setForeground(fg);
                super.paintComponent(g);
            }
        }
    }
}
