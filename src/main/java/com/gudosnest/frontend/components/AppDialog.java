package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Modal dialog with a title, a body, and buttons in the footer (right aligned).
 * Esc closes it.
 */
public class AppDialog extends JDialog {

    private final JPanel body;
    private final JPanel footerLeft;
    private final JPanel footerRight;

    public AppDialog(Component parent, String title) {
        super(parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent),
                "Gudo's Nest", ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new MigLayout("insets 24, fill, gap 0", "[grow,fill]", "[]16[grow,fill]24[]"));
        root.setBackground(Colors.CARD);
        root.add(Ui.sectionTitle(title), "wrap");

        body = new JPanel(new MigLayout("insets 0, fillx, gap 8, wrap 1", "[grow,fill]"));
        body.setOpaque(false);
        root.add(body, "wrap");

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

    /** Sizes the dialog to its content (at least the given width) and shows it. Blocks until closed. */
    public void showDialog(int minWidth) {
        pack();
        if (getWidth() < minWidth) {
            setSize(minWidth, getHeight());
        }
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    // Simple shared message boxes.
    // They are centred on the app window, not on the component passed in: pages are taller than the
    // screen (they sit in a scroll pane), so centring on the page put the box at the screen edge.

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(windowOf(parent), message, "Gudo's Nest", JOptionPane.PLAIN_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(windowOf(parent), message, "Gudo's Nest", JOptionPane.ERROR_MESSAGE);
    }

    /** Yes/No question. Returns true if the user chose the confirm option. */
    public static boolean confirm(Component parent, String message, String confirmText) {
        Object[] options = {confirmText, "Go back"};
        int choice = JOptionPane.showOptionDialog(windowOf(parent), message, "Gudo's Nest",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    /**
     * The window to centre a message box on. A dialog that was just closed (for example the booking
     * dialog after Approve) is skipped, so the box centres on the main window instead of where that
     * dialog used to be. Falls back to null, which centres it on the screen.
     */
    private static Window windowOf(Component parent) {
        Window w = parent == null ? null
                : parent instanceof Window win ? win : SwingUtilities.getWindowAncestor(parent);
        while (w != null && !w.isShowing()) {
            w = w.getOwner();
        }
        return w;
    }
}
