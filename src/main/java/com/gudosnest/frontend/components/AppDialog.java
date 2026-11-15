package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Modal dialog with a title, a body, and buttons in the footer (right aligned).
 * <p>
 * Size: never taller than it is wide (at most square) and never bigger than the app window. A body
 * taller than that scrolls, so the footer buttons always stay on screen. It opens centred on the
 * app window.
 * <p>
 * Pages: a step that follows from this dialog (a confirmation, a reason to enter, a receipt) opens
 * as the next page of the same dialog with {@link #nextPage} and a Back button, never as a second
 * dialog on top. Esc goes back a page, or closes the dialog on its first page.
 */
public class AppDialog extends JDialog {

    /** Share of the app window a dialog may take at most. */
    private static final double MAX_SHARE = 0.9;

    private final JLabel title;
    private final JPanel bodyHolder = new JPanel(new BorderLayout());
    private final JScrollPane bodyScroll;
    private final JPanel footer = Ui.transparent("insets 0, fillx", "[][grow][]");

    // The page on show
    private JPanel body;
    private JPanel footerLeft;
    private JPanel footerRight;

    private final Deque<Page> history = new ArrayDeque<>();
    private int minWidth;
    private boolean fitting;

    /** A page that was left with nextPage(), kept so Back can show it again exactly as it was. */
    private record Page(String title, JPanel body, JPanel footerLeft, JPanel footerRight,
                        JButton defaultButton, int scrollY) {
    }

    public AppDialog(Component parent, String titleText) {
        super(parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent),
                "Gudo's Nest", ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new MigLayout("insets 24, fill, gap 0", "[grow,fill]", "[]16[grow,fill]24[]"));
        root.setBackground(Colors.CARD);
        title = Ui.sectionTitle(titleText);
        root.add(title, "wrap");

        bodyHolder.setOpaque(false);
        bodyScroll = Ui.scroll(bodyHolder);
        bodyScroll.getViewport().setBackground(Colors.CARD);
        bodyScroll.getViewport().getView().setBackground(Colors.CARD);
        root.add(bodyScroll, "wrap, wmin 0, hmin 0");
        root.add(footer);
        showPageParts(newBody(), newFooterSide(), newFooterSide());

        setContentPane(root);
        setResizable(false);
        getRootPane().registerKeyboardAction(e -> {
            if (history.isEmpty()) {
                dispose();
            } else {
                previousPage();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** The content area of the page on show. Uses a one-column MigLayout with 8px gaps. */
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

    /** A button that returns to the previous page (for pages opened with nextPage). */
    public AppButton addBackButton(String text) {
        AppButton back = new AppButton(text, AppButton.Style.OUTLINED);
        back.addActionListener(e -> previousPage());
        addButton(back);
        return back;
    }

    /** Sizes the dialog (at least the given width), centres it on the app window and shows it. Blocks until closed. */
    public void showDialog(int minWidth) {
        this.minWidth = minWidth;
        fit(false);
        setVisible(true);
    }

    // ---------------------------------------------------------------------------------- pages

    /**
     * Leaves the page on show (Back returns to it) and starts an empty one with this title.
     * Fill the returned body and the footer buttons, then call {@link #refit()}.
     */
    public JPanel nextPage(String titleText) {
        history.push(new Page(title.getText(), body, footerLeft, footerRight,
                getRootPane().getDefaultButton(), bodyScroll.getVerticalScrollBar().getValue()));
        startPage(titleText);
        return body;
    }

    /** Swaps the page on show for a new one with no way back (for example "Payment received"). */
    public JPanel replacePage(String titleText) {
        history.clear();
        startPage(titleText);
        return body;
    }

    /** Shows the page that was on show before the last nextPage(). */
    public void previousPage() {
        Page page = history.pop();
        title.setText(page.title());
        showPageParts(page.body(), page.footerLeft(), page.footerRight());
        getRootPane().setDefaultButton(page.defaultButton());
        refit();
        SwingUtilities.invokeLater(() -> bodyScroll.getVerticalScrollBar().setValue(page.scrollY()));
    }

    /** Lays out a new page. The dialog grows if the page needs more room, but doesn't shrink between pages. */
    public void refit() {
        fit(true);
        bodyScroll.getVerticalScrollBar().setValue(0);
        getContentPane().repaint();
    }

    /**
     * A question as the next page: "Go back" returns to the page before, and the confirm button
     * runs {@code onConfirm}, which closes the dialog or calls previousPage() when it is done.
     */
    public void confirmPage(String titleText, String message, String confirmText, AppButton.Style style,
                            Runnable onConfirm) {
        JPanel page = nextPage(titleText);
        page.add(Widgets.wrap(message, Fonts.body(), Colors.TEXT), "wmin 0");
        addBackButton("Go back");
        AppButton confirm = new AppButton(confirmText, style);
        confirm.addActionListener(e -> onConfirm.run());
        addButton(confirm);
        refit();
    }

    private void startPage(String titleText) {
        title.setText(titleText);
        showPageParts(newBody(), newFooterSide(), newFooterSide());
        getRootPane().setDefaultButton(null);
    }

    private void showPageParts(JPanel newBody, JPanel left, JPanel right) {
        body = newBody;
        footerLeft = left;
        footerRight = right;
        bodyHolder.removeAll();
        bodyHolder.add(body, BorderLayout.CENTER);
        footer.removeAll();
        footer.add(footerLeft, "cell 0 0");
        footer.add(footerRight, "cell 2 0");
    }

    private static JPanel newBody() {
        JPanel panel = new JPanel(new MigLayout("insets 0, fillx, gap 8, wrap 1", "[grow,fill]"));
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel newFooterSide() {
        return Ui.transparent("insets 0, gap 8", "");
    }

    // ---------------------------------------------------------------------------------- sizing

    /** Dialogs call pack() when their content changes; it keeps the size rules and re-centres. */
    @Override
    public void pack() {
        if (fitting) {
            super.pack();
        } else {
            fit(false);
        }
    }

    /**
     * Width: what the content asks for, at least minWidth. Height: what the content asks for, but no
     * more than the width (so at most square). Both are capped by the app window, and the body
     * scrolls when it doesn't fit.
     */
    private void fit(boolean neverShrink) {
        boolean wasShowing = isShowing();
        Rectangle before = getBounds();
        fitting = true;
        try {
            getContentPane().invalidate();
            super.pack();
        } finally {
            fitting = false;
        }
        Rectangle area = maxArea();
        int maxWidth = (int) (area.width * MAX_SHARE);
        int maxHeight = (int) (area.height * MAX_SHARE);

        int width = Math.min(Math.max(getWidth(), minWidth), maxWidth);
        if (neverShrink && wasShowing) {
            width = before.width;       // moving between pages keeps the width; wrapped text fits it
        }
        int height = Math.min(Math.min(getHeight(), width), maxHeight);
        if (neverShrink && wasShowing) {
            height = Math.max(height, before.height);
        }
        setSize(width, height);
        validate();
        if (!wasShowing || width != before.width || height != before.height) {
            setLocationRelativeTo(appWindow());
        } else {
            setLocation(before.getLocation());
        }
    }

    /** The app window's bounds (or the screen's, before it is on screen), whichever is smaller. */
    private Rectangle maxArea() {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Window app = appWindow();
        if (app == null || !app.isShowing()) {
            return screen;
        }
        Rectangle window = app.getBounds();
        return new Rectangle(Math.min(window.width, screen.width), Math.min(window.height, screen.height));
    }

    /** The main app window, even when this dialog belongs to another dialog. */
    private Window appWindow() {
        Window w = getOwner();
        while (w != null && w.getOwner() != null) {
            w = w.getOwner();
        }
        return w;
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
