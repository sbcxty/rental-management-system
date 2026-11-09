package com.gudosnest.frontend.auth;

import com.gudosnest.frontend.theme.Colors;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The flat geometric house illustration from the Stitch login screen, drawn with Java2D shapes
 * (no image file). Drawn on a 520 × 600 design grid and scaled to fit the space it is given.
 */
public class LoginArt extends JComponent {

    private static final double W = 520;
    private static final double H = 600;
    private static final Color PAPER = new Color(0xF3F1F6);

    public LoginArt() {
        setPreferredSize(new Dimension(420, 480));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double scale = Math.min(getWidth() / W, getHeight() / H);
        g2.translate((getWidth() - W * scale) / 2, (getHeight() - H * scale) / 2);
        g2.scale(scale, scale);

        // Roof and chimney with two puffs of smoke
        fill(g2, Colors.PRIMARY, triangle(0, 190, 180, 70, 360, 190));
        fill(g2, Colors.BURGUNDY, new Rectangle2D.Double(275, 70, 38, 66));
        fill(g2, Colors.LIGHT_BLUE, new Ellipse2D.Double(284, 38, 20, 20));
        fill(g2, Colors.TERTIARY, new Ellipse2D.Double(288, 14, 13, 13));

        // Round window in a square
        fill(g2, Colors.SECONDARY, new Rectangle2D.Double(0, 206, 166, 160));
        fill(g2, PAPER, new Ellipse2D.Double(30, 232, 108, 108));
        g2.setColor(Colors.SECONDARY);
        g2.setStroke(new BasicStroke(4f));
        g2.draw(new java.awt.geom.Line2D.Double(84, 232, 84, 340));
        g2.draw(new java.awt.geom.Line2D.Double(30, 286, 138, 286));

        // Four-point star with a light blue center
        fill(g2, Colors.BURGUNDY, star(268, 286, 80, 14));
        fill(g2, Colors.LIGHT_BLUE, new Ellipse2D.Double(253, 271, 30, 30));

        // Stack of circles
        double[][] dots = {{436, 104, 0}, {436, 162, 1}, {436, 220, 2}, {436, 278, 3}};
        Color[] dotColors = {Colors.PRIMARY, Colors.LIGHT_BLUE, Colors.SECONDARY, Colors.LIGHT_BLUE};
        for (double[] d : dots) {
            fill(g2, dotColors[(int) d[2]], new Ellipse2D.Double(d[0] - 26, d[1] - 26, 52, 52));
        }

        // Arched doorway with a dot
        Path2D arch = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        arch.append(archShape(0, 412, 166, 186), false);
        arch.append(archShape(32, 444, 102, 154), false);
        fill(g2, Colors.PRIMARY, arch);
        fill(g2, Colors.BURGUNDY, new Ellipse2D.Double(58, 478, 50, 50));

        // Portrait: a person in a framed square
        fill(g2, Colors.TERTIARY, new Rectangle2D.Double(186, 406, 166, 192));
        fill(g2, Colors.BURGUNDY, new Arc2D.Double(186, 500, 166, 196, 0, 180, Arc2D.CHORD));
        fill(g2, PAPER, new Ellipse2D.Double(225, 448, 88, 88));
        fill(g2, Colors.PRIMARY, new Ellipse2D.Double(246, 469, 46, 46));

        // Tall panel with a wave cut out
        fill(g2, PAPER, new RoundRectangle2D.Double(412, 318, 96, 280, 4, 4));
        GeneralPath wave = new GeneralPath();
        wave.moveTo(412, 318);
        wave.lineTo(462, 318);
        wave.curveTo(520, 390, 500, 430, 462, 470);
        wave.curveTo(420, 520, 430, 570, 462, 598);
        wave.lineTo(412, 598);
        wave.closePath();
        fill(g2, Colors.SECONDARY, wave);
        g2.dispose();
    }

    private static void fill(Graphics2D g2, Color color, java.awt.Shape shape) {
        g2.setColor(color);
        g2.fill(shape);
    }

    private static Path2D triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.closePath();
        return p;
    }

    /** A four-point star with curved (concave) sides. */
    private static Path2D star(double cx, double cy, double r, double waist) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - r);
        p.quadTo(cx + waist, cy - waist, cx + r, cy);
        p.quadTo(cx + waist, cy + waist, cx, cy + r);
        p.quadTo(cx - waist, cy + waist, cx - r, cy);
        p.quadTo(cx - waist, cy - waist, cx, cy - r);
        p.closePath();
        return p;
    }

    /** A rectangle with a half-circle top. */
    private static Path2D archShape(double x, double y, double w, double h) {
        Path2D p = new Path2D.Double();
        double r = w / 2;
        p.moveTo(x, y + h);
        p.lineTo(x, y + r);
        p.append(new Arc2D.Double(x, y, w, w, 180, -180, Arc2D.OPEN), true);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }
}
