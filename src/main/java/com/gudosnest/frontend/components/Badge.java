package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A small rounded status pill, for example "Pending" or "3 of 8 units occupied".
 * Tones never use red: red is only for destructive buttons and errors.
 */
public class Badge extends JLabel {

    public enum Tone {
        NEUTRAL(Colors.PRIMARY_TINT, Colors.PRIMARY),   // pending, awaiting approval, counts
        INFO(Colors.BLUE_TINT, Colors.BURGUNDY),        // upcoming, available, good
        STRONG(Colors.PRIMARY, Colors.TEXT_ON_DARK),    // occupied, highlighted
        MUTED(Colors.PAGE, Colors.NEUTRAL);             // completed, cancelled, rejected, expired

        final Color background;
        final Color foreground;

        Tone(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }
    }

    private Tone tone;

    public Badge(String text, Tone tone) {
        super(text);
        setFont(Fonts.captionMedium());
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setOpaque(false);
        setTone(tone);
    }

    public void setTone(Tone tone) {
        this.tone = tone;
        setForeground(tone.foreground);
        repaint();
    }

    public Tone getTone() {
        return tone;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(tone.background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
