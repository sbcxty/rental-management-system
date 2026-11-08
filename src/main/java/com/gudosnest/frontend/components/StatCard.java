package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;

/**
 * A number with a plain label, for example "Active bookings  14".
 */
public class StatCard extends Card {

    private final JLabel title;
    private final JLabel value;
    private final JLabel note;

    public StatCard(String titleText, String iconName) {
        super(new MigLayout("insets 20, fillx, gap 0", "[grow][]", "[]8[]4[]"));
        title = Ui.label(titleText, Fonts.body(), Colors.TEXT_MUTED);
        value = Ui.label("-", Fonts.statValue(), Colors.TEXT);
        note = Ui.caption(" ");
        add(title);
        add(new JLabel(iconName == null ? null : Icons.get(iconName, 18, Colors.NEUTRAL)), "wrap");
        add(value, "span, wrap");
        add(note, "span");
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setValue(String text) {
        value.setText(text);
    }

    /** Small line under the value, for example "Last 6 months". */
    public void setNote(String text) {
        note.setText(text == null || text.isEmpty() ? " " : text);
    }
}
