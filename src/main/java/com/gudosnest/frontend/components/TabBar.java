package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Pill tabs such as "All  Pending (3)  Upcoming (5)". The same look is used on every page.
 */
public class TabBar extends JPanel {

    private final List<JToggleButton> tabs = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<IntConsumer> listeners = new ArrayList<>();
    private final ButtonGroup group = new ButtonGroup();

    public TabBar(String... tabNames) {
        super(new MigLayout("insets 0, gap 8", ""));
        setOpaque(false);
        for (String name : tabNames) {
            addTab(name);
        }
        if (!tabs.isEmpty()) {
            tabs.get(0).setSelected(true);
        }
    }

    private void addTab(String name) {
        int index = tabs.size();
        JToggleButton tab = new JToggleButton(name);
        tab.setFont(Fonts.bodyMedium());
        tab.setFocusPainted(false);
        tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tab.putClientProperty(FlatClientProperties.STYLE,
                "arc:999;focusWidth:0;innerFocusWidth:0;borderWidth:1;margin:6,16,6,16"
                        + ";background:#FFFFFF;foreground:" + Colors.hex(Colors.TEXT)
                        + ";borderColor:" + Colors.hex(Colors.BORDER)
                        + ";focusedBorderColor:" + Colors.hex(Colors.SECONDARY)
                        + ";focusedBackground:#FFFFFF"
                        + ";hoverBackground:" + Colors.hex(Colors.SURFACE_HOVER)
                        + ";selectedBackground:" + Colors.hex(Colors.PRIMARY)
                        + ";selectedForeground:#FFFFFF");
        tab.addActionListener(e -> listeners.forEach(l -> l.accept(index)));
        group.add(tab);
        tabs.add(tab);
        names.add(name);
        add(tab, "h 36!");
    }

    /** Shows a count after the tab name, for example "Pending (3)". */
    public void setCount(int index, int count) {
        tabs.get(index).setText(names.get(index) + " (" + count + ")");
    }

    public int getSelectedIndex() {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).isSelected()) {
                return i;
            }
        }
        return -1;
    }

    /** Selects a tab and notifies listeners. */
    public void setSelectedIndex(int index) {
        tabs.get(index).setSelected(true);
        listeners.forEach(l -> l.accept(index));
    }

    public void onChange(IntConsumer listener) {
        listeners.add(listener);
    }
}
