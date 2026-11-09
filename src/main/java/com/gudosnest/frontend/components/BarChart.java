package com.gudosnest.frontend.components;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.List;

/**
 * Flat bar chart drawn with Java2D (no library), as in the Stitch dashboard and finances.
 * One series: grey bars with the last (current) month in Primary and the amount above each bar.
 * Two series: income (Primary) and expenses (light) side by side per month.
 */
public class BarChart extends JComponent {

    private static final Color MUTED_BAR = new Color(0xD8D2DD);
    private static final Color SECOND_BAR = new Color(0xCDBFD6);

    private List<String> labels = List.of();
    private List<BigDecimal> first = List.of();
    private List<BigDecimal> second;

    public BarChart() {
        setPreferredSize(new Dimension(UIScale.scale(400), UIScale.scale(280)));
    }

    /** One series. The last bar is highlighted. */
    public void setData(List<String> labels, List<BigDecimal> values) {
        this.labels = labels;
        this.first = values;
        this.second = null;
        repaint();
    }

    /** Two series (income and expenses). */
    public void setData(List<String> labels, List<BigDecimal> income, List<BigDecimal> expenses) {
        this.labels = labels;
        this.first = income;
        this.second = expenses;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int n = labels.size();
        if (n == 0) {
            g2.dispose();
            return;
        }
        g2.setFont(Fonts.caption());
        FontMetrics fm = g2.getFontMetrics();

        double max = 0;
        for (int i = 0; i < n; i++) {
            max = Math.max(max, first.get(i).doubleValue());
            if (second != null) {
                max = Math.max(max, second.get(i).doubleValue());
            }
        }
        double top = niceTop(max);

        int axisW = fm.stringWidth(Formats.moneyShort(BigDecimal.valueOf(top))) + UIScale.scale(12);
        int labelH = fm.getHeight() + UIScale.scale(8);
        int valueH = second == null ? fm.getHeight() + UIScale.scale(4) : 0;
        int chartTop = valueH + fm.getHeight() / 2;
        int chartBottom = getHeight() - labelH;
        int chartH = Math.max(1, chartBottom - chartTop);
        int chartW = getWidth() - axisW;

        // Grid lines: 0, half, top
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 4f}, 0f));
        for (int step = 0; step <= 2; step++) {
            double v = top * step / 2;
            int y = chartBottom - (int) (chartH * (v / top));
            g2.setColor(Colors.BORDER);
            g2.drawLine(axisW, y, getWidth(), y);
            g2.setColor(Colors.TEXT_MUTED);
            String text = Formats.moneyShort(BigDecimal.valueOf(v));
            g2.drawString(text, axisW - UIScale.scale(8) - fm.stringWidth(text), y + fm.getAscent() / 2 - 1);
        }
        g2.setStroke(new BasicStroke(1f));

        int slot = chartW / n;
        int arc = UIScale.scale(8);
        for (int i = 0; i < n; i++) {
            int slotX = axisW + i * slot;
            if (second == null) {
                int barW = Math.min(UIScale.scale(72), (int) (slot * 0.6));
                int x = slotX + (slot - barW) / 2;
                double v = first.get(i).doubleValue();
                int h = (int) Math.round(chartH * Math.max(0, v) / top);
                g2.setColor(i == n - 1 ? Colors.PRIMARY : MUTED_BAR);
                fillTopRounded(g2, x, chartBottom - h, barW, h, arc);
                g2.setColor(Colors.TEXT);
                String text = Formats.moneyShort(first.get(i));
                g2.drawString(text, x + (barW - fm.stringWidth(text)) / 2, chartBottom - h - UIScale.scale(6));
            } else {
                int barW = Math.min(UIScale.scale(32), (int) (slot * 0.3));
                int gap = UIScale.scale(6);
                int x = slotX + (slot - (barW * 2 + gap)) / 2;
                int h1 = (int) Math.round(chartH * Math.max(0, first.get(i).doubleValue()) / top);
                int h2 = (int) Math.round(chartH * Math.max(0, second.get(i).doubleValue()) / top);
                g2.setColor(Colors.PRIMARY);
                fillTopRounded(g2, x, chartBottom - h1, barW, h1, arc / 2);
                g2.setColor(SECOND_BAR);
                fillTopRounded(g2, x + barW + gap, chartBottom - h2, barW, h2, arc / 2);
            }
            boolean current = i == n - 1;
            g2.setFont(current ? Fonts.captionMedium() : Fonts.caption());
            FontMetrics lfm = g2.getFontMetrics();
            g2.setColor(current ? Colors.TEXT : Colors.TEXT_MUTED);
            String label = labels.get(i);
            g2.drawString(label, slotX + (slot - lfm.stringWidth(label)) / 2, getHeight() - UIScale.scale(4));
            g2.setFont(Fonts.caption());
        }
        g2.dispose();
    }

    /** A bar with rounded top corners and a flat bottom. */
    private static void fillTopRounded(Graphics2D g2, int x, int y, int w, int h, int arc) {
        if (h <= 0) {
            return;
        }
        g2.fillRoundRect(x, y, w, h, arc, arc);
        int flat = Math.min(h, arc / 2);
        g2.fillRect(x, y + h - flat, w, flat);
    }

    /** Rounds the top of the scale up to a tidy number (₱100k, ₱150k, ...). */
    private static double niceTop(double max) {
        if (max <= 0) {
            return 1000;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        for (double step : new double[]{1, 1.5, 2, 2.5, 5, 10}) {
            if (step * magnitude >= max * 1.1) {
                return step * magnitude;
            }
        }
        return 10 * magnitude;
    }

    /** Small legend dot + text used beside two-series charts. */
    public static Color incomeColor() {
        return Colors.PRIMARY;
    }

    public static Color expenseColor() {
        return SECOND_BAR;
    }
}
