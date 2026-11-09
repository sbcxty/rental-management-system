package com.gudosnest.frontend.components;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Table model over a list of objects: each object becomes one row via {@code toRow}. Column
 * classes come from the first row, so TwoLine and Pill cells get their AppTable renderers.
 */
public class RowTableModel<T> extends AbstractTableModel {

    private final String[] columns;
    private final Function<T, Object[]> toRow;
    private List<T> rows = List.of();
    private List<Object[]> cells = List.of();

    public RowTableModel(Function<T, Object[]> toRow, String... columns) {
        this.columns = columns;
        this.toRow = toRow;
    }

    public void setRows(List<T> rows) {
        this.rows = List.copyOf(rows);
        this.cells = this.rows.stream().map(toRow).toList();
        fireTableDataChanged();
    }

    public T rowAt(int index) {
        return rows.get(index);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        for (Object[] row : cells) {
            if (row[column] != null) {
                return row[column].getClass();
            }
        }
        return Object.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return cells.get(row)[column];
    }

    /**
     * Makes the table as tall as its rows, so on a page that already scrolls there is one
     * scrollbar (the page's) instead of a second one inside the table.
     */
    public static void fitToRows(JTable table) {
        int rows = Math.max(1, table.getRowCount());
        table.setPreferredScrollableViewportSize(new java.awt.Dimension(0, rows * table.getRowHeight()));
        table.revalidate();
    }

    /** Runs the action with the clicked row's object (single click), and shows a hand cursor on rows. */
    public static <T> void onRowClick(JTable table, RowTableModel<T> model, Consumer<T> action) {
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    action.accept(model.rowAt(table.convertRowIndexToModel(row)));
                }
            }
        });
    }
}
