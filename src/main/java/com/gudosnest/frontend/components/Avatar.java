package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A round avatar with initials, for example "MA".
 */
public class Avatar extends JComponent {

    private String initials;
    private final int size;
    private Color background = Colors.PRIMARY;

    public Avatar(String initials, int size) {
        this.initials = initials;
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    public void setInitials(String initials) {
        this.initials = initials;
        repaint();
    }

    public void setBackgroundColor(Color color) {
        this.background = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillOval(0, 0, size, size);
        g2.setColor(Colors.TEXT_ON_DARK);
        g2.setFont(Fonts.body(size * 0.36f, true));
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(initials)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(initials, x, y);
        g2.dispose();
    }
}
