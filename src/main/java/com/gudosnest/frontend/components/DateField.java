package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import java.awt.Cursor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A date input: shows "Nov 18, 2025" with a calendar icon and opens a month calendar when clicked.
 * Days before {@link #setMinimum} (and any day the optional filter rejects) can't be picked.
 */
public class DateField extends JButton {

    private LocalDate value;
    private LocalDate minimum;
    private LocalDate maximum;
    private final String placeholder;
    private final List<Consumer<LocalDate>> listeners = new ArrayList<>();

    public DateField(String placeholder) {
        this.placeholder = placeholder;
        setHorizontalAlignment(SwingConstants.LEFT);
        setIcon(Icons.get("calendar-days", 16, Colors.NEUTRAL));
        setIconTextGap(8);
        setFont(Fonts.body());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.STYLE, "arc:8;focusWidth:0;margin:6,12,6,12;background:#FFFFFF"
                + ";borderColor:" + Colors.hex(Colors.BORDER) + ";hoverBorderColor:" + Colors.hex(Colors.SECONDARY));
        addActionListener(e -> openCalendar());
        refreshText();
    }

    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate date) {
        this.value = date;
        refreshText();
        listeners.forEach(l -> l.accept(date));
    }

    public void setMinimum(LocalDate minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(LocalDate maximum) {
        this.maximum = maximum;
    }

    public void onChange(Consumer<LocalDate> listener) {
        listeners.add(listener);
    }

    private void refreshText() {
        setText(value == null ? placeholder : Formats.date(value));
        setForeground(value == null ? Colors.TEXT_MUTED : Colors.TEXT);
    }

    private boolean selectable(LocalDate day) {
        return (minimum == null || !day.isBefore(minimum)) && (maximum == null || !day.isAfter(maximum));
    }

    private void openCalendar() {
        JPopupMenu popup = new JPopupMenu();
        YearMonth start = YearMonth.from(value != null ? value : minimum != null ? minimum : LocalDate.now());
        popup.add(new MonthPanel(start, day -> {
            popup.setVisible(false);
            setValue(day);
        }));
        java.awt.Point at = popupLocation(popup.getPreferredSize());
        popup.show(this, at.x, at.y);
    }

    /** Space kept between the calendar and the edges of the app window (and the screen). */
    private static final int EDGE_MARGIN = 16;

    /**
     * Where the calendar goes, relative to this field: below it, or above it when there's no room
     * below, moved sideways if needed, and always at least EDGE_MARGIN inside the app window and the
     * screen, so it never touches their edges.
     */
    private java.awt.Point popupLocation(java.awt.Dimension size) {
        int margin = com.formdev.flatlaf.util.UIScale.scale(EDGE_MARGIN);
        int gap = com.formdev.flatlaf.util.UIScale.scale(4);
        java.awt.Rectangle area = usableArea();
        java.awt.Point field = getLocationOnScreen();
        int top = area.y + margin;
        int bottom = area.y + area.height - margin;
        int left = area.x + margin;
        int right = area.x + area.width - margin;

        int y = field.y + getHeight() + gap;                       // below
        if (y + size.height > bottom) {
            int above = field.y - gap - size.height;
            y = above >= top ? above : Math.max(top, bottom - size.height);
        }
        int x = Math.max(left, Math.min(field.x, right - size.width));
        return new java.awt.Point(x - field.x, y - field.y);
    }

    /** The app window's bounds on screen, trimmed to the screen's usable area (without the taskbar). */
    private java.awt.Rectangle usableArea() {
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        java.awt.Rectangle screen = gc.getBounds();
        java.awt.Insets insets = getToolkit().getScreenInsets(gc);
        screen = new java.awt.Rectangle(screen.x + insets.left, screen.y + insets.top,
                screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        while (w != null && w.getOwner() != null) {
            w = w.getOwner();
        }
        if (w == null || !w.isShowing()) {
            return screen;
        }
        java.awt.Rectangle window = w.getBounds().intersection(screen);
        return window.isEmpty() ? screen : window;
    }

    /** One month grid with previous/next month buttons. */
    private final class MonthPanel extends JPanel {
        private YearMonth month;
        private final Consumer<LocalDate> onPick;
        private final Predicate<LocalDate> canPick = DateField.this::selectable;

        MonthPanel(YearMonth month, Consumer<LocalDate> onPick) {
            super(new MigLayout("insets 12, gap 2, wrap 7", "[34!,center]".repeat(7)));
            this.month = month;
            this.onPick = onPick;
            setBackground(Colors.CARD);
            build();
        }

        private void build() {
            removeAll();
            JButton prev = navButton("chevron-left", -1);
            JButton next = navButton("chevron-right", 1);
            JLabel title = Ui.label(Formats.monthYear(month.atDay(1)), Fonts.bodyBold(), Colors.TEXT);
            add(prev, "span 1");
            add(title, "span 5, center");
            add(next, "span 1, wrap");
            for (DayOfWeek dow : List.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                add(Ui.caption(dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2)));
            }
            int blanks = month.atDay(1).getDayOfWeek().getValue() % 7;   // Sunday first
            for (int i = 0; i < blanks; i++) {
                add(new JLabel());
            }
            for (int d = 1; d <= month.lengthOfMonth(); d++) {
                LocalDate day = month.atDay(d);
                JButton b = new JButton(String.valueOf(d));
                b.setFont(Fonts.body());
                b.setFocusPainted(false);
                boolean chosen = day.equals(value);
                b.putClientProperty(FlatClientProperties.STYLE, "arc:8;borderWidth:0;focusWidth:0;margin:2,2,2,2"
                        + ";background:" + (chosen ? Colors.hex(Colors.PRIMARY) : "#FFFFFF")
                        + ";foreground:" + (chosen ? "#FFFFFF" : Colors.hex(Colors.TEXT))
                        + ";hoverBackground:" + (chosen ? Colors.hex(Colors.PRIMARY) : Colors.hex(Colors.PRIMARY_TINT)));
                b.setEnabled(canPick.test(day));
                b.addActionListener(e -> onPick.accept(day));
                add(b, "w 34!, h 32!");
            }
            revalidate();
            repaint();
        }

        private JButton navButton(String icon, int delta) {
            JButton b = new JButton(Icons.get(icon, 16, Colors.TEXT));
            b.setFocusPainted(false);
            b.putClientProperty(FlatClientProperties.STYLE, "arc:8;borderWidth:0;focusWidth:0;background:#FFFFFF"
                    + ";hoverBackground:" + Colors.hex(Colors.PRIMARY_TINT));
            b.addActionListener(e -> {
                month = month.plusMonths(delta);
                build();
                JPopupMenu popup = (JPopupMenu) javax.swing.SwingUtilities.getAncestorOfClass(JPopupMenu.class, this);
                if (popup != null && popup.isShowing()) {
                    // A month can need one more row of weeks: resize, and keep it clear of the edges
                    java.awt.Point at = popupLocation(popup.getPreferredSize());
                    popup.setVisible(false);
                    popup.show(DateField.this, at.x, at.y);
                }
            });
            return b;
        }
    }
}
