package com.gudosnest.frontend.components;

import com.gudosnest.backend.structures.ImageRing;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The property photo carousel (renter's property page, as in the Stitch "Property Detail"
 * design): a large photo with previous/next buttons and a "2 / 5" counter, and a row of
 * thumbnails underneath. Photos live in an ImageRing (circular doubly linked list), so
 * "next" on the last photo wraps to the first.
 */
public class Carousel extends JPanel {

    private static final int BUTTON = 36;
    private static final int THUMB_W = 80;
    private static final int THUMB_H = 52;

    private ImageRing<String> photos = new ImageRing<>(List.of());
    private final PhotoView view = new PhotoView();
    private final JPanel thumbRow = Ui.transparent("insets 0, gap 8", "");
    private final List<Thumb> thumbs = new ArrayList<>();

    public Carousel(int photoHeight) {
        super(new MigLayout("insets 0, gap 0, fillx, wrap 1", "[grow,fill]", "[]12[]"));
        setOpaque(false);
        add(view, "h " + photoHeight + "!");
        add(thumbRow);
    }

    /** Shows these photo paths (see ImageUtil for the path format). */
    public void setPhotos(List<String> paths) {
        photos = new ImageRing<>(paths);
        thumbRow.removeAll();
        thumbs.clear();
        if (paths.size() > 1) {
            for (int i = 0; i < paths.size(); i++) {
                Thumb thumb = new Thumb(paths.get(i), i + 1);
                thumbs.add(thumb);
                thumbRow.add(thumb, "w " + THUMB_W + "!, h " + THUMB_H + "!");
            }
        }
        refresh();
    }

    private void refresh() {
        view.repaint();
        thumbs.forEach(Thumb::repaint);
        revalidate();
    }

    // ----------------------------------------------------------------------------------------

    /** The large photo with round previous/next buttons and the counter. */
    private final class PhotoView extends JComponent {
        private final Icon prevIcon = Icons.get("chevron-left", 18, Colors.TEXT);
        private final Icon nextIcon = Icons.get("chevron-right", 18, Colors.TEXT);
        private String cachedPath;
        private BufferedImage cachedImage;   // scaled photo, rebuilt only when the photo or size changes

        PhotoView() {
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (photos.size() < 2) {
                        return;
                    }
                    if (prevButton().contains(e.getPoint())) {
                        photos.previous();
                        refresh();
                    } else if (nextButton().contains(e.getPoint())) {
                        photos.next();
                        refresh();
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

        private Rectangle prevButton() {
            int b = UIScale.scale(BUTTON);
            return new Rectangle(UIScale.scale(Theme.M), (getHeight() - b) / 2, b, b);
        }

        private Rectangle nextButton() {
            int b = UIScale.scale(BUTTON);
            return new Rectangle(getWidth() - UIScale.scale(Theme.M) - b, (getHeight() - b) / 2, b, b);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            String path = photos.current();
            if (cachedImage == null || cachedImage.getWidth() != getWidth()
                    || cachedImage.getHeight() != getHeight() || !java.util.Objects.equals(path, cachedPath)) {
                cachedImage = ImageUtil.cover(ImageUtil.load(path), getWidth(), getHeight(), UIScale.scale(Theme.RADIUS + 4));
                cachedPath = path;
            }
            g2.drawImage(cachedImage, 0, 0, null);

            if (photos.size() > 1) {
                drawRoundButton(g2, prevButton(), prevIcon);
                drawRoundButton(g2, nextButton(), nextIcon);

                String counter = photos.position() + " / " + photos.size();
                g2.setFont(Fonts.captionMedium());
                FontMetrics fm = g2.getFontMetrics();
                int pad = UIScale.scale(10);
                int w = fm.stringWidth(counter) + 2 * pad;
                int h = UIScale.scale(26);
                int x = getWidth() - UIScale.scale(Theme.M) - w;
                int y = UIScale.scale(Theme.M);
                g2.setColor(Colors.BURGUNDY);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(Colors.TEXT_ON_DARK);
                g2.drawString(counter, x + pad, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }

        private void drawRoundButton(Graphics2D g2, Rectangle r, Icon icon) {
            g2.setColor(Colors.CARD);
            g2.fillOval(r.x, r.y, r.width, r.height);
            icon.paintIcon(this, g2, r.x + (r.width - icon.getIconWidth()) / 2,
                    r.y + (r.height - icon.getIconHeight()) / 2);
        }
    }

    /** A small thumbnail; the current photo gets a Primary outline. Click to jump to it. */
    private final class Thumb extends JComponent {
        private final int position;
        private final String path;
        private BufferedImage image;   // built at the real on-screen size (which grows with display scaling)

        Thumb(String path, int position) {
            this.position = position;
            this.path = path;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    photos.moveTo(Thumb.this.position);
                    refresh();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = UIScale.scale(Theme.CONTROL_RADIUS);
            if (image == null || image.getWidth() != getWidth() || image.getHeight() != getHeight()) {
                image = ImageUtil.cover(ImageUtil.load(path), getWidth(), getHeight(), arc);
            }
            g2.drawImage(image, 0, 0, null);
            if (photos.position() == position) {
                float stroke = UIScale.scale(2f);
                g2.setColor(Colors.PRIMARY);
                g2.setStroke(new java.awt.BasicStroke(stroke));
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(stroke / 2, stroke / 2,
                        getWidth() - stroke, getHeight() - stroke, arc, arc));
            }
            g2.dispose();
        }
    }
}
