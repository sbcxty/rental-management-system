package com.gudosnest.frontend.components;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Equal-width cards in rows (Properties, a property's units). Shows as many columns as fit
 * with each card at least {@code minCardWidth} wide, up to {@code maxColumns}, so the grid goes
 * 3 → 2 → 1 columns as the window narrows instead of squeezing the cards.
 */
public class CardGrid extends JPanel {

    private final MigLayout layout = new MigLayout();
    private final int minCardWidth;
    private final int maxColumns;
    private int columns;

    public CardGrid(int minCardWidth, int maxColumns) {
        this.minCardWidth = minCardWidth;
        this.maxColumns = maxColumns;
        setOpaque(false);
        setLayout(layout);
        apply(maxColumns);
    }

    /**
     * Picks the column count as soon as the grid is given its size, during layout, rather than
     * from a resize event (which arrives after the first paint, so the cards visibly jumped from
     * 3 columns to 2 when a page opened). PortalShell lays a new page out until it settles.
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        updateColumns();
    }

    public void addCard(JComponent card) {
        add(card, "grow, wmin 0");
    }

    private void updateColumns() {
        int width = getWidth();
        if (width <= 0) {
            return;
        }
        int gap = UIScale.scale(Theme.L);
        int fit = (width + gap) / (UIScale.scale(minCardWidth) + gap);
        apply(Math.max(1, Math.min(maxColumns, fit)));
    }

    private void apply(int count) {
        if (count == columns) {
            return;
        }
        columns = count;
        layout.setLayoutConstraints("insets 0, gap " + Theme.L + ", fillx, wrap " + count);
        layout.setColumnConstraints("[grow,fill,sg card]".repeat(count));
        revalidate();
    }
}
