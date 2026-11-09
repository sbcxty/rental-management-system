package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Two or three options in a tinted track, the chosen one on white ("6 months | This year",
 * "Monthly | Annual", "Request | History"), as in the Stitch designs.
 */
public class Segmented extends JPanel {

    private final List<JToggleButton> options = new ArrayList<>();
    private final List<IntConsumer> listeners = new ArrayList<>();

    public Segmented(String... names) {
        super(new MigLayout("insets 4, gap 4", ""));
        setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < names.length; i++) {
            int index = i;
            JToggleButton option = new JToggleButton(names[i]);
            option.setFont(Fonts.bodyMedium());
            option.setFocusPainted(false);
            option.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            option.putClientProperty(FlatClientProperties.STYLE,
                    "arc:8;focusWidth:0;innerFocusWidth:0;borderWidth:0;margin:4,14,4,14"
                            + ";background:" + Colors.hex(Colors.PRIMARY_TINT)
                            + ";foreground:" + Colors.hex(Colors.TEXT_MUTED)
                            + ";focusedBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                            + ";hoverBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                            + ";selectedBackground:#FFFFFF"
                            + ";selectedForeground:" + Colors.hex(Colors.TEXT));
            option.addActionListener(e -> listeners.forEach(l -> l.accept(index)));
            group.add(option);
            options.add(option);
            add(option, "h 30!");
        }
        options.get(0).setSelected(true);
    }

    public int getSelectedIndex() {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isSelected()) {
                return i;
            }
        }
        return 0;
    }

    public void setSelectedIndex(int index) {
        options.get(index).setSelected(true);
        listeners.forEach(l -> l.accept(index));
    }

    /** Changes an option's text, for example to add a count: "History (4)". */
    public void setText(int index, String text) {
        options.get(index).setText(text);
    }

    public void onChange(IntConsumer listener) {
        listeners.add(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Colors.PRIMARY_TINT);
        int arc = com.formdev.flatlaf.util.UIScale.scale(12);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
    }
}
