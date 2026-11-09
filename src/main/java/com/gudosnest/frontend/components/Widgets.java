package com.gudosnest.frontend.components;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.backend.service.refund.RefundPolicy;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/** Small building blocks shared by many pages. */
public final class Widgets {

    private Widgets() {
    }

    /** A photo that fills its box (cropped, rounded corners), redrawn only when its size changes. */
    public static class Photo extends JComponent {
        private final String path;
        private final int arc;
        private final boolean roundBottom;
        private BufferedImage cached;

        /** @param roundBottom false for photos on top of a card (only the top corners rounded) */
        public Photo(String path, int arc, boolean roundBottom) {
            this.path = path;
            this.arc = arc;
            this.roundBottom = roundBottom;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            int scaledArc = UIScale.scale(arc);
            if (cached == null || cached.getWidth() != w || cached.getHeight() != h) {
                BufferedImage src = ImageUtil.load(path);
                if (roundBottom) {
                    cached = ImageUtil.cover(src, w, h, scaledArc);
                } else {
                    // Round the top only: draw it rounded but taller, then cut off the bottom corners
                    BufferedImage tall = ImageUtil.cover(src, w, h + scaledArc, scaledArc);
                    cached = tall.getSubimage(0, 0, w, h);
                }
            }
            g.drawImage(cached, 0, 0, null);
        }
    }

    /** A white card that highlights on hover and runs an action when clicked anywhere on it. */
    public static Card clickableCard(MigLayout layout, Runnable onClick) {
        Card card = new Card(layout);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(Colors.SURFACE_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                java.awt.Point p = javax.swing.SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    card.setBackground(Colors.CARD);
                }
            }
        };
        installDeep(card, mouse);
        return card;
    }

    /**
     * Adds the listener to the component and to every child added to it, now or later, so a click
     * on a label inside a card still counts as a click on the card. Buttons keep their own action.
     */
    private static void installDeep(java.awt.Component c, MouseAdapter mouse) {
        if (c instanceof javax.swing.AbstractButton) {
            return;
        }
        c.addMouseListener(mouse);
        if (c instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                installDeep(child, mouse);
            }
            container.addContainerListener(new java.awt.event.ContainerAdapter() {
                @Override
                public void componentAdded(java.awt.event.ContainerEvent e) {
                    installDeep(e.getChild(), mouse);
                }
            });
        }
    }

    /** Text link in Primary with an optional trailing arrow: "Open inventory →". */
    public static JLabel link(String text, boolean arrow, Runnable onClick) {
        JLabel label = Ui.label(text, Fonts.bodyMedium(), Colors.PRIMARY);
        if (arrow) {
            label.setIcon(Icons.get("arrow-right", 16, Colors.PRIMARY));
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            label.setIconTextGap(6);
        }
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(Colors.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Colors.PRIMARY);
            }
        });
        return label;
    }

    /** Thin rounded progress bar (investment recovery, expense breakdown). */
    public static JComponent progress(double share, Color fill) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(100, UIScale.scale(8)));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();
                g2.setColor(Colors.PRIMARY_TINT);
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                int w = (int) Math.round(getWidth() * Math.max(0, Math.min(1, share)));
                if (w > 0) {
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, Math.max(w, h), h, h, h);
                }
                g2.dispose();
            }
        };
    }

    /** Round tinted square with an icon, used at the start of list rows (Needs attention). */
    public static JComponent iconTile(String icon, Color background, Color iconColor) {
        JLabel tile = new JLabel(Icons.get(icon, 20, iconColor), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(background);
                int arc = UIScale.scale(Theme.RADIUS);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return tile;
    }

    /** A tinted box (used for summaries inside cards and dialogs). */
    public static Card tintBox(MigLayout layout) {
        Card box = new Card(layout);
        box.setBackground(Colors.PRIMARY_TINT);
        box.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        return box;
    }

    /**
     * The cancellation policy, with the exact same text everywhere it appears
     * (text comes from RefundPolicy.TEXT).
     */
    public static Card policyBox() {
        Card box = tintBox(new MigLayout("insets 16, gap 0, fillx, wrap 1", "[grow,fill]", ""));
        box.add(Ui.iconLabel("Cancellation policy", "shield-check", Fonts.bodyBold(), Colors.TEXT), "gapbottom 8");
        for (String[] line : RefundPolicy.TEXT) {
            box.add(policyLine(line[0], line[1]), "gapbottom 4, wmin 0");
        }
        return box;
    }

    private static JComponent policyLine(String title, String text) {
        // HTML lets the line wrap inside the card width; the bold title is the tier name.
        JLabel label = new JLabel("<html><b>" + title + "</b> " + text + "</html>");
        label.setFont(Fonts.caption());
        label.setForeground(Colors.TEXT);
        return label;
    }

    /** A label followed by a value on the right, both on one line (inside cards). */
    public static JPanel keyValue(String key, JComponent value) {
        JPanel row = Ui.transparent("insets 0, fillx, gap 8", "[grow][right]");
        row.add(Ui.muted(key));
        row.add(value);
        return row;
    }

    /** Wrapping body text (a multi-line label that grows downward). */
    public static JLabel wrap(String text, java.awt.Font font, Color color) {
        JLabel label = new JLabel("<html>" + escape(text).replace("\n", "<br>") + "</html>");
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** Escapes text shown inside an HTML label so names like "A & B" print correctly. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
