package com.gudosnest.frontend.components;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * The row of stat cards at the top of a page: equal cards side by side, or two per row when the
 * window (or an expanded sidebar) leaves less than 176px per card. Only the width decides, so
 * the layout doesn't jump when the numbers change.
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

    /** Narrowest a card may get before the row switches to two per row. */
    private static final int MIN_CARD_WIDTH = 176;

    private void updateColumns() {
        // Chosen from the width alone, never from the numbers inside, so the page keeps the same
        // layout whatever is selected (a long amount shrinks to fit its card instead, see StatCard).
        int gaps = UIScale.scale(Theme.L) * (count - 1);
        boolean oneRow = getWidth() == 0 || UIScale.scale(MIN_CARD_WIDTH) * count + gaps <= getWidth();
        String mode = oneRow ? "one" : "two";
        if (mode.equals(this.mode)) {
            return;
        }
        this.mode = mode;
        int columns = oneRow ? count : Math.min(2, count);
        layout.setLayoutConstraints("insets 0, gap " + Theme.L + ", fillx, wrap " + columns);
        layout.setColumnConstraints("[grow,fill,sg card]".repeat(columns));
        revalidate();
    }
}
