package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Modal dialog with a title, a body, and buttons in the footer (right aligned).
 * Every dialog has the same width; tall content scrolls. Esc closes it.
 */
public class AppDialog extends JDialog {

    /** Every dialog is this wide (the Bookings dialog's width). */
    public static final int STANDARD_WIDTH = 640;
    /** Short dialogs are stretched to at least this height. */
    public static final int MIN_HEIGHT = 420;

    private final JPanel body;
    private final JPanel footerLeft;
    private final JPanel footerRight;

    public AppDialog(Component parent, String title) {
        super(parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent),
                "Gudo's Nest", ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new MigLayout("insets 24, fill, gap 0", "[grow,fill]", "[]16[grow,fill]24[]"));
        root.setBackground(Colors.CARD);
        root.add(Ui.sectionTitle(title), "wrap");

        body = new BodyPanel(new MigLayout("insets 0, fillx, gap 8, wrap 1", "[grow,fill]"));
        body.setOpaque(false);
        JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, "wrap");

        JPanel footer = Ui.transparent("insets 0, fillx", "[][grow][]");
        footerLeft = Ui.transparent("insets 0, gap 8", "");
        footerRight = Ui.transparent("insets 0, gap 8", "");
        footer.add(footerLeft, "cell 0 0");
        footer.add(footerRight, "cell 2 0");
        root.add(footer);

        setContentPane(root);
        setResizable(false);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Standard width; height follows the content, but never below MIN_HEIGHT or above the screen. */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int maxHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height - 40;
        return new Dimension(STANDARD_WIDTH, Math.min(Math.max(d.height, MIN_HEIGHT), maxHeight));
    }

    /** The content area. Uses a one-column MigLayout with 8px gaps. */
    public JPanel getBody() {
        return body;
    }

    /** Adds a button on the right of the footer. Add them in reading order, primary last. */
    public void addButton(JButton button) {
        footerRight.add(button);
    }

    /** Adds a button on the left of the footer (for example "Message renter"). */
    public void addLeftButton(JButton button) {
        footerLeft.add(button);
    }

    /** A plain "Close" button. */
    public AppButton addCloseButton(String text) {
        AppButton close = new AppButton(text, AppButton.Style.OUTLINED);
        close.addActionListener(e -> dispose());
        addButton(close);
        return close;
    }

    /** Shows the dialog at the standard size. Blocks until closed. */
    public void showDialog() {
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    /** Old callers still compile; the width argument is ignored. */
    public void showDialog(int ignoredWidth) {
        showDialog();
    }

    /** Body that follows the viewport's width and scrolls only vertically. */
    private static final class BodyPanel extends JPanel implements Scrollable {
        BodyPanel(LayoutManager layout) {
            super(layout);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // Simple shared message boxes

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Gudo's Nest", JOptionPane.PLAIN_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Gudo's Nest", JOptionPane.ERROR_MESSAGE);
    }

    /** Yes/No question. Returns true if the user chose the confirm option. */
    public static boolean confirm(Component parent, String message, String confirmText) {
        Object[] options = {confirmText, "Go back"};
        int choice = JOptionPane.showOptionDialog(parent, message, "Gudo's Nest",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        return choice == 0;
    }
}