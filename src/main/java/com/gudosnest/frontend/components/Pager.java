package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

/**
 * Page arrows under a table: "Showing 11–20 of 40" on the left, "Page 2 of 4" with previous and
 * next arrows on the right. The table shows one page of rows at a time, so long lists stay short
 * and the page keeps the same shape. It is always shown (arrows greyed out on a single page), so
 * the layout doesn't jump when a filter changes the number of rows.
 * <p>
 * Use: {@code pager.setTotal(rows.size())}, then show {@code pager.slice(rows)}; {@link #onChange}
 * runs when an arrow is clicked.
 */
public class Pager extends JPanel {

    /** Rows per page on every paged table. */
    public static final int PAGE_SIZE = 10;

    private final int pageSize;
    private int page;       // 0-based
    private int total;
    private Runnable onChange = () -> { };

    private final JLabel range = Ui.caption(" ");
    private final JLabel pageLabel = Ui.label(" ", Fonts.bodyMedium(), Colors.TEXT);
    private final AppButton previous = arrow("chevron-left");
    private final AppButton next = arrow("chevron-right");

    public Pager() {
        this(PAGE_SIZE);
    }

    public Pager(int pageSize) {
        super(new MigLayout("insets 0, fillx, gap 8", "[grow][][][]"));
        this.pageSize = pageSize;
        setOpaque(false);
        add(range, "wmin 0, aligny center");
        add(pageLabel, "aligny center, gapright 8");
        add(previous, "w 40!, h 40!");
        add(next, "w 40!, h 40!");
        previous.setToolTipText("Previous page");
        next.setToolTipText("Next page");
        previous.addActionListener(e -> go(page - 1));
        next.addActionListener(e -> go(page + 1));
        update();
    }

    private static AppButton arrow(String icon) {
        AppButton b = new AppButton("", AppButton.Style.OUTLINED).withIcon(icon);
        b.setFocusable(false);
        return b;
    }

    /** Runs after an arrow changes the page (redraw the table with {@link #slice}). */
    public void onChange(Runnable action) {
        onChange = action;
    }

    /** The number of rows in the whole list. Keeps the page if it still exists, else the last page. */
    public void setTotal(int total) {
        this.total = total;
        page = Math.min(page, pageCount() - 1);
        update();
    }

    /** Back to the first page (when a tab, filter or search changes what is listed). */
    public void reset() {
        page = 0;
        update();
    }

    /** The rows of the current page. */
    public <T> List<T> slice(List<T> rows) {
        int from = Math.min(page * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return rows.subList(from, to);
    }

    private int pageCount() {
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private void go(int target) {
        if (target < 0 || target >= pageCount()) {
            return;
        }
        page = target;
        update();
        onChange.run();
    }

    private void update() {
        int from = total == 0 ? 0 : page * pageSize + 1;
        int to = Math.min((page + 1) * pageSize, total);
        range.setText(total == 0 ? "Nothing to show" : "Showing " + from + "–" + to + " of " + total);
        pageLabel.setText("Page " + (page + 1) + " of " + pageCount());
        previous.setEnabled(page > 0);
        next.setEnabled(page < pageCount() - 1);
    }
}
