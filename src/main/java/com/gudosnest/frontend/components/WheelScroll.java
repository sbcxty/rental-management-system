package com.gudosnest.frontend.components;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Mouse wheel and touchpad scrolling for every scroll area in the app.
 * <ul>
 *   <li>Scrolling works anywhere over the content, not only over the scrollbar: an inner scroll area
 *       (a table, a text box, a column in a dialog) scrolls while it can, then hands the movement to
 *       the scroll area around it. Before, a table inside a page kept the wheel for itself, so the
 *       Bookings page only scrolled with the scrollbar.</li>
 *   <li>Touchpads send many tiny movements (fractions of a wheel notch); they are added up instead of
 *       being rounded away, so two-finger scrolling follows the fingers.</li>
 * </ul>
 */
public final class WheelScroll implements MouseWheelListener {

    /** Pixels per wheel notch. */
    private static final int NOTCH = 48;

    private final JScrollPane scroll;
    private double pending;      // touchpad movement not yet a whole pixel

    private WheelScroll(JScrollPane scroll) {
        this.scroll = scroll;
    }

    /** Replaces the scroll area's own wheel handling with this one. */
    public static <T extends JScrollPane> T install(T scroll) {
        for (MouseWheelListener l : scroll.getMouseWheelListeners()) {
            scroll.removeMouseWheelListener(l);
        }
        scroll.addMouseWheelListener(new WheelScroll(scroll));
        return scroll;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean horizontal = e.isShiftDown();
        JScrollBar bar = horizontal ? scroll.getHorizontalScrollBar() : scroll.getVerticalScrollBar();
        double rotation = e.getPreciseWheelRotation();
        if (rotation == 0) {
            return;
        }
        if (!canScroll(bar, rotation)) {
            pending = 0;
            passToOuterScroll(e);
            return;
        }
        pending += rotation * NOTCH;
        int pixels = (int) pending;     // whole pixels now, the rest waits for the next movement
        pending -= pixels;
        if (pixels != 0) {
            bar.setValue(bar.getValue() + pixels);
        }
        e.consume();
    }

    private boolean canScroll(JScrollBar bar, double rotation) {
        if (!bar.isVisible() || !bar.isEnabled() || bar.getMaximum() - bar.getMinimum() <= bar.getVisibleAmount()) {
            return false;
        }
        return rotation < 0 ? bar.getValue() > bar.getMinimum()
                : bar.getValue() + bar.getVisibleAmount() < bar.getMaximum();
    }

    private void passToOuterScroll(MouseWheelEvent e) {
        JScrollPane outer = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, scroll);
        if (outer != null) {
            outer.dispatchEvent(SwingUtilities.convertMouseEvent(scroll, e, outer));
            e.consume();
        }
    }
}
