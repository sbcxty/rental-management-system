package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;

/**
 * A number with a plain label, for example "Active bookings  14".
 */
public class StatCard extends Card {

    private final JLabel title;
    private final JLabel value;
    private final JLabel note;

    public StatCard(String titleText, String iconName) {
        super(new MigLayout("insets 24, fillx, gap 0", "[grow][]", "[]12[]4[]"));
        title = Ui.label(titleText, Fonts.body(), Colors.TEXT_MUTED);
        value = new FitLabel("-");
        value.setFont(Fonts.statValue());
        value.setForeground(Colors.TEXT);
        note = Ui.caption(" ");
        add(title, "growx, wmin 0");
        add(new JLabel(iconName == null ? null : Icons.get(iconName, 18, Colors.NEUTRAL)), "wrap");
        add(value, "span, wrap, growx, wmin 0");
        add(note, "span, growx, wmin 0");
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setValue(String text) {
        value.setText(text);
    }

    /**
     * The big number. Asks for its full width, but when the card is narrower (a large amount in a
     * four-card row) it draws the text smaller to fit instead of widening the card.
     */
    private static final class FitLabel extends JLabel {
        FitLabel(String text) {
            super(text);
        }

        @Override
        public java.awt.Dimension getMinimumSize() {
            return new java.awt.Dimension(0, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            java.awt.Font font = getFont();
            // The peso sign comes from a fallback font and draws wider than Java measures it, so
            // keep a few pixels spare (as Ui.label does) before deciding the text fits.
            int room = getWidth() - com.formdev.flatlaf.util.UIScale.scale(8);
            int full = g2.getFontMetrics(font).stringWidth(getText());
            if (full > room && full > 0 && room > 0) {
                font = font.deriveFont(font.getSize2D() * room / full);
            }
            g2.setFont(font);
            g2.setColor(getForeground());
            // Same baseline as the full-size text, so a shrunk amount lines up with its neighbours
            int baseline = getHeight() - g2.getFontMetrics(getFont()).getDescent();
            g2.drawString(getText(), 0, baseline);
            g2.dispose();
        }
    }

    /** Small line under the value, for example "Last 6 months". */
    public void setNote(String text) {
        note.setText(text == null || text.isEmpty() ? " " : text);
    }
}
