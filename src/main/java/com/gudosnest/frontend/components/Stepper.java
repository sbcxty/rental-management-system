package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** "− 2 +" number control (guests, item quantity). Stays between its minimum and maximum. */
public class Stepper extends JPanel {

    private final JButton minus = button("minus");
    private final JButton plus = button("plus");
    private final JLabel label = new JLabel("", SwingConstants.CENTER);
    private final List<IntConsumer> listeners = new ArrayList<>();
    private int value;
    private int min;
    private int max;

    public Stepper(int value, int min, int max) {
        super(new MigLayout("insets 0, gap 6", "[][28!,center][]"));
        setOpaque(false);
        this.min = min;
        this.max = max;
        label.setFont(Fonts.bodyMedium());
        label.setForeground(Colors.TEXT);
        minus.addActionListener(e -> setValue(this.value - 1, true));
        plus.addActionListener(e -> setValue(this.value + 1, true));
        add(minus, "w 28!, h 28!");
        add(label);
        add(plus, "w 28!, h 28!");
        setValue(value, false);
    }

    private static JButton button(String icon) {
        JButton b = new JButton(Icons.get(icon, 14, Colors.TEXT));
        b.setFocusPainted(false);
        b.putClientProperty(FlatClientProperties.STYLE, "arc:8;focusWidth:0;margin:0,0,0,0;background:#FFFFFF"
                + ";borderColor:" + Colors.hex(Colors.BORDER) + ";hoverBackground:" + Colors.hex(Colors.PRIMARY_TINT));
        return b;
    }

    public int getValue() {
        return value;
    }

    public void setRange(int min, int max) {
        this.min = min;
        this.max = Math.max(min, max);
        setValue(value, false);
    }

    private void setValue(int newValue, boolean notify) {
        int clamped = Math.max(min, Math.min(max, newValue));
        boolean changed = clamped != value;
        value = clamped;
        label.setText(String.valueOf(value));
        minus.setEnabled(value > min);
        plus.setEnabled(value < max);
        if (notify && changed) {
            listeners.forEach(l -> l.accept(value));
        }
    }

    public void onChange(IntConsumer listener) {
        listeners.add(listener);
    }
}
