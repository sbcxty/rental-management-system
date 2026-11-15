package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * A white rounded panel with a 1px border. No shadow.
 */
public class Card extends JPanel {

    /** Card with 16px padding and one growing column. */
    public Card() {
        this(new MigLayout("insets 16, fillx, gap 8", "[grow,fill]"));
    }

    public Card(LayoutManager layout) {
        super(layout);
        setBackground(Colors.CARD);
        // Style first, border second: setting a FlatLaf style reinstalls the panel's defaults, which
        // replaces a FlatLineBorder (a UIResource) with no border at all.
        putClientProperty(FlatClientProperties.STYLE, "arc:" + Theme.RADIUS);
        setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), Colors.BORDER, 1f, Theme.RADIUS));
    }
}
