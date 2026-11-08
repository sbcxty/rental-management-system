package com.gudosnest.frontend.components;

import com.gudosnest.backend.structures.ImageRing;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;

import javax.swing.JComponent;
import javax.swing.Icon;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Photo carousel with previous/next buttons and a "2 / 5" counter.
 * Photos are kept in an ImageRing (circular doubly linked list), so it wraps around.
 */
public class Carousel extends JComponent {

    private static final int BUTTON = 36;

    private ImageRing<String> photos = new ImageRing<>(List.of());
    private final Icon prevIcon = Icons.get("chevron-left", 18, Colors.TEXT);
    private final Icon nextIcon = Icons.get("chevron-right", 18, Colors.TEXT);

    public Carousel(int preferredWidth, int preferredHeight) {
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (photos.size() < 2) {
                    return;
                }
                if (prevButton().contains(e.getPoint())) {
                    photos.previous();
                    repaint();
                } else if (nextButton().contains(e.getPoint())) {
                    photos.next();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean overButton = photos.size() > 1
                        && (prevButton().contains(e.getPoint()) || nextButton().contains(e.getPoint()));
                setCursor(Cursor.getPredefinedCursor(overButton ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** Shows these photo paths (see ImageUtil for the path format). */
    public void setPhotos(List<String> paths) {
        photos = new ImageRing<>(paths);
        repaint();
    }

    private Rectangle prevButton() {
        return new Rectangle(Theme.M, (getHeight() - BUTTON) / 2, BUTTON, BUTTON);
    }

    private Rectangle nextButton() {
        return new Rectangle(getWidth() - Theme.M - BUTTON, (getHeight() - BUTTON) / 2, BUTTON, BUTTON);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawImage(ImageUtil.cover(ImageUtil.load(photos.current()), getWidth(), getHeight(), Theme.RADIUS), 0, 0, null);

        if (photos.size() > 1) {
            drawRoundButton(g2, prevButton(), prevIcon);
            drawRoundButton(g2, nextButton(), nextIcon);

            String counter = photos.position() + " / " + photos.size();
            g2.setFont(Fonts.captionMedium());
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(counter) + 20;
            int h = 24;
            int x = getWidth() - Theme.M - w;
            int y = Theme.M;
            g2.setColor(Colors.CARD);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(Colors.TEXT);
            g2.drawString(counter, x + 10, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }

    private void drawRoundButton(Graphics2D g2, Rectangle r, Icon icon) {
        g2.setColor(Colors.CARD);
        g2.fillOval(r.x, r.y, r.width, r.height);
        icon.paintIcon(this, g2, r.x + (r.width - icon.getIconWidth()) / 2, r.y + (r.height - icon.getIconHeight()) / 2);
    }
}
