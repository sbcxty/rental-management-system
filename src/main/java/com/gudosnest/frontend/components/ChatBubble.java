package com.gudosnest.frontend.components;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * One chat message bubble. Mine are filled Primary on the right; theirs are light with a border on
 * the left. The bubble is at most 70% as wide as {@code widthSource} (the chat history) and wraps
 * its text to fit, so long messages grow downward instead of sideways.
 */
public class ChatBubble extends JComponent {

    private static final int PAD_X = 14;
    private static final int PAD_Y = 10;
    private static final int ARC = 16;
    private static final double MAX_SHARE = 0.7;

    private final String text;
    private final boolean mine;
    private final JComponent widthSource;

    public ChatBubble(String text, boolean mine, JComponent widthSource) {
        this.text = text;
        this.mine = mine;
        this.widthSource = widthSource;
        setFont(Fonts.body());
        setForeground(mine ? Colors.TEXT_ON_DARK : Colors.TEXT);
    }

    private int maxTextWidth() {
        int available = widthSource.getWidth();
        if (available <= 0) {
            available = UIScale.scale(600);   // before the first layout; corrected on the next resize
        }
        return Math.max(UIScale.scale(80), (int) (available * MAX_SHARE) - 2 * UIScale.scale(PAD_X));
    }

    /** Splits the text into lines no wider than maxWidth, breaking at spaces (or inside very long words). */
    private List<String> lines(FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                // A single word wider than the bubble: cut it into pieces that fit.
                String rest = word;
                while (fm.stringWidth(rest) > maxWidth && rest.length() > 1) {
                    int cut = rest.length() - 1;
                    while (cut > 1 && fm.stringWidth(rest.substring(0, cut)) > maxWidth) {
                        cut--;
                    }
                    lines.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
                line.append(rest);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int width = 0;
        List<String> lines = lines(fm, maxTextWidth());
        for (String line : lines) {
            width = Math.max(width, fm.stringWidth(line));
        }
        // +2: characters from the fallback font (₱) draw a little wider than measured
        return new Dimension(width + 2 + 2 * UIScale.scale(PAD_X),
                lines.size() * fm.getHeight() + 2 * UIScale.scale(PAD_Y));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float w = getWidth() - 1f;
        float h = getHeight() - 1f;
        float arc = UIScale.scale(ARC);
        float corner = arc / 2f;
        // Rounded everywhere except a small corner on the speaker's side (top-right for mine).
        Area shape = new Area(new RoundRectangle2D.Float(0.5f, 0.5f, w, h, arc, arc));
        shape.add(new Area(new Rectangle2D.Float(mine ? w - corner + 0.5f : 0.5f, 0.5f, corner, corner)));

        g2.setColor(mine ? Colors.PRIMARY : Colors.SURFACE_HOVER);
        g2.fill(shape);
        if (!mine) {
            g2.setColor(Colors.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(shape);
        }

        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int x = UIScale.scale(PAD_X);
        int y = UIScale.scale(PAD_Y) + fm.getAscent();
        for (String line : lines(fm, getWidth() - 2 * UIScale.scale(PAD_X))) {
            g2.drawString(line, x, y);
            y += fm.getHeight();
        }
        g2.dispose();
    }
}
