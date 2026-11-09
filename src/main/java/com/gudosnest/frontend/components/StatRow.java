package com.gudosnest.frontend.components;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * The row of stat cards at the top of a page. Shows them side by side, and switches to two
 * per row when the window (or an expanded sidebar) leaves too little width, so the cards
 * never get squeezed past their contents or pushed off the right edge.
 */
public class StatRow extends JPanel {

    private final MigLayout layout = new MigLayout();
    private int count;
    private String mode = "";

    public StatRow() {
        setOpaque(false);
        setLayout(layout);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateColumns();
            }
        });
    }

    public void addCard(StatCard card) {
        add(card, "grow, wmin 0");
        count++;
        mode = "";
        updateColumns();
    }

    private void updateColumns() {
        // Three layouts, best first: one row of equal cards; one row where each card is as wide
        // as its own contents needs (so a long amount still fits); two cards per row.
        int widest = 0;
        int total = 0;
        for (var card : getComponents()) {
            int w = card.getPreferredSize().width;
            widest = Math.max(widest, w);
            total += w;
        }
        int gaps = UIScale.scale(Theme.L) * (count - 1);
        String mode;
        if (getWidth() == 0 || widest * count + gaps <= getWidth()) {
            mode = "equal";
        } else if (total + gaps <= getWidth()) {
            mode = "fit";
        } else {
            mode = "two";
        }
        if (mode.equals(this.mode)) {
            return;
        }
        this.mode = mode;
        int columns = mode.equals("two") ? Math.min(2, count) : count;
        String column = mode.equals("fit") ? "[grow,fill]" : "[grow,fill,sg card]";
        layout.setLayoutConstraints("insets 0, gap " + Theme.L + ", fillx, wrap " + columns);
        layout.setColumnConstraints(column.repeat(columns));
        revalidate();
    }
}
