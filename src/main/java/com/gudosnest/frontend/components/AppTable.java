package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Builds tables with one consistent look: white rows, 1px row lines, no vertical lines,
 * roomy padding, and renderers for two-line cells and status pills.
 */
public final class AppTable {

    private AppTable() {
    }

    public static final int ROW_HEIGHT = 60;

    /** Cell value shown as a bold-ish first line and a muted second line. */
    public record TwoLine(String main, String sub) {
        @Override
        public String toString() {
            return main + " " + sub;   // used by search filters
        }
    }

    /** Cell value shown as a status pill. */
    public record Pill(String text, Badge.Tone tone) {
        @Override
        public String toString() {
            return text;
        }
    }

    public static JTable create(TableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setRowHeight(ROW_HEIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(Colors.CARD);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 44));
        header.setDefaultRenderer(new HeaderRenderer());

        table.setDefaultRenderer(Object.class, new TextRenderer());
        table.setDefaultRenderer(TwoLine.class, new TwoLineRenderer());
        table.setDefaultRenderer(Pill.class, new PillRenderer());
        return table;
    }

    /** Puts the table in a card with a border and no extra scroll border. */
    public static Card inCard(JTable table) {
        Card card = new Card(new MigLayout("insets 1, fill", "[grow,fill]", "[grow,fill]"));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Colors.CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll);
        return card;
    }

    public static void setColumnWidth(JTable table, int column, int width) {
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            String text = value == null ? "" : value.toString().toUpperCase();
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, text, false, false, row, column);
            label.setFont(Fonts.tableHeader());
            label.setForeground(Colors.TEXT_MUTED);
            label.setBackground(Colors.TABLE_HEADER);
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.BORDER),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)));
            return label;
        }
    }

    private static final class TextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, false, row, column);
            label.setFont(Fonts.body());
            label.setForeground(Colors.TEXT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            return label;
        }
    }

    private static final class TwoLineRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new MigLayout(
                "insets 0 16 0 16, gap 0, wrap 1, fillx, aligny center", "[grow,fill]"));
        private final JLabel main = Ui.label("", Fonts.bodyMedium(), Colors.TEXT);
        private final JLabel sub = Ui.caption("");

        TwoLineRenderer() {
            panel.add(main, "wmin 0");   // wmin 0 lets long text end in "..." instead of being cut
            panel.add(sub, "wmin 0");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            TwoLine cell = (TwoLine) value;
            main.setText(cell == null ? "" : cell.main());
            sub.setText(cell == null ? "" : cell.sub());
            panel.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return panel;
        }
    }

    private static final class PillRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new MigLayout("insets 0 16 0 16, aligny center"));
        private final Badge badge = new Badge("", Badge.Tone.NEUTRAL);

        PillRenderer() {
            panel.add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Pill pill = (Pill) value;
            badge.setText(pill == null ? "" : pill.text());
            badge.setTone(pill == null ? Badge.Tone.MUTED : pill.tone());
            panel.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return panel;
        }
    }
}
