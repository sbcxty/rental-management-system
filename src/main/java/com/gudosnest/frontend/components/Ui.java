package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

/**
 * Small factory methods so every screen builds labels, fields and scroll areas the same way.
 */
public final class Ui {

    private Ui() {
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        // 2px spare on the right: characters drawn from the fallback font (₱, ×) render slightly
        // wider than Java measures them, which cut off the last digit of amounts like ₱16,200.00.
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JLabel pageTitle(String text) {
        return label(text, Fonts.pageTitle(), Colors.TEXT);
    }

    public static JLabel sectionTitle(String text) {
        return label(text, Fonts.sectionTitle(), Colors.TEXT);
    }

    public static JLabel cardTitle(String text) {
        return label(text, Fonts.cardTitle(), Colors.TEXT);
    }

    public static JLabel body(String text) {
        return label(text, Fonts.body(), Colors.TEXT);
    }

    public static JLabel bold(String text) {
        return label(text, Fonts.bodyBold(), Colors.TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, Fonts.body(), Colors.TEXT_MUTED);
    }

    public static JLabel caption(String text) {
        return label(text, Fonts.caption(), Colors.TEXT_MUTED);
    }

    /** Fee and refund amounts: bold Primary (never red). */
    public static JLabel fee(String text) {
        return label(text, Fonts.bodyBold(), Colors.PRIMARY);
    }

    /** Error message: the only non-button use of red. */
    public static JLabel error(String text) {
        JLabel label = label(text, Fonts.caption(), Colors.DANGER);
        label.setIcon(Icons.get("circle-alert", 14, Colors.DANGER));
        label.setIconTextGap(Theme.XS);
        return label;
    }

    public static JLabel iconLabel(String text, String icon, Font font, Color color) {
        JLabel label = label(text, font, color);
        label.setIcon(Icons.get(icon, 16, color));
        label.setIconTextGap(Theme.S);
        return label;
    }

    public static JTextField textField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return field;
    }

    public static JTextField searchField(String placeholder) {
        JTextField field = textField(placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                Icons.get("search", 16, Colors.NEUTRAL));
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        return field;
    }

    public static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Fonts.body());
        return area;
    }

    /** A wrapping, read-only paragraph of muted text. */
    public static JTextArea paragraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setFont(Fonts.body());
        area.setForeground(Colors.TEXT_MUTED);
        area.setBorder(null);
        return area;
    }

    /** A label above an input, 4px apart. */
    public static JPanel field(String labelText, JComponent input) {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 0, fillx, wrap 1", "[grow,fill]", "[]4[]"));
        panel.setOpaque(false);
        panel.add(label(labelText, Fonts.bodyMedium(), Colors.TEXT));
        panel.add(input, "h " + Theme.CONTROL_HEIGHT + "!");
        return panel;
    }

    /** Same as field() but lets the input choose its own height (text areas, lists). */
    public static JPanel fieldFree(String labelText, JComponent input) {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 0, fillx, wrap 1", "[grow,fill]", "[]4[]"));
        panel.setOpaque(false);
        panel.add(label(labelText, Fonts.bodyMedium(), Colors.TEXT));
        panel.add(input);
        return panel;
    }

    /** "Label ........ value" row used in price summaries and detail lists. */
    public static JPanel row(String labelText, JComponent value) {
        JPanel panel = new JPanel(new MigLayout("insets 0, fillx", "[grow][right]"));
        panel.setOpaque(false);
        panel.add(muted(labelText));
        panel.add(value);
        return panel;
    }

    public static JPanel row(String labelText, String value) {
        return row(labelText, body(value));
    }

    public static JSeparator divider() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(Colors.BORDER);
        return separator;
    }

    /**
     * Vertical scroll area with the page background and smooth wheel speed.
     * The content always matches the visible width, so it reflows when the sidebar expands.
     */
    public static JScrollPane scroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(new WidthTrackingPanel(content));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Colors.PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    /** Centered muted message for empty lists, such as "No bookings yet". */
    public static JPanel emptyState(String message) {
        JPanel panel = new JPanel(new MigLayout("insets 32, fill", "[center]", "[center]"));
        panel.setOpaque(false);
        panel.add(muted(message));
        return panel;
    }

    /** A transparent panel, handy for rows and stacks. */
    public static JPanel transparent(String migConstraints, String columns) {
        JPanel panel = new JPanel(new MigLayout(migConstraints, columns));
        panel.setOpaque(false);
        return panel;
    }

    /** Fills the scroll area's width (no horizontal scrolling) and scrolls only vertically. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(JComponent content) {
            super(new BorderLayout());
            setBackground(Colors.PAGE);
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Stretch to fill short pages; scroll once content is taller than the window.
            return getParent() != null && getPreferredSize().height < getParent().getHeight();
        }
    }
}
