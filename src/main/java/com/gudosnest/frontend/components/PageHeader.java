package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Top of every page: an optional back link ("← Back to dashboard"), the page title with an
 * optional subtitle under it, and actions on the right (dropdowns, buttons).
 */
public class PageHeader extends JPanel {

    private final JLabel title;
    private final JLabel subtitle;
    private final JLabel back;
    private final JPanel titleRow;
    private final JPanel actions;
    private Runnable backAction;

    public PageHeader(String titleText) {
        super(new MigLayout("insets 0, fillx, gap 0, hidemode 3", "[grow][]", "[]8[]"));
        setOpaque(false);

        back = Ui.iconLabel("", "arrow-left", Fonts.body(), Colors.TEXT_MUTED);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.setVisible(false);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (backAction != null) {
                    backAction.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                back.setForeground(Colors.TEXT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                back.setForeground(Colors.TEXT_MUTED);
            }
        });
        add(back, "span, wrap");

        JPanel left = Ui.transparent("insets 0, gap 0, wrap 1, hidemode 3", "[]");
        title = Ui.pageTitle(titleText);
        titleRow = Ui.transparent("insets 0, gap 16", "[]");
        titleRow.add(title);
        left.add(titleRow);
        subtitle = Ui.label("", Fonts.body(), Colors.TEXT_MUTED);
        subtitle.setVisible(false);
        left.add(subtitle, "gaptop 4");
        add(left, "growx, aligny top");

        actions = Ui.transparent("insets 0, gap 8", "");
        add(actions, "aligny center");
    }

    /** Adds a control on the right side (buttons, dropdowns). */
    public PageHeader addAction(JComponent component) {
        actions.add(component, "h 40!");
        return this;
    }

    /** Adds a control right after the title (for example the inventory unit dropdown or a badge). */
    public PageHeader addNextToTitle(JComponent component) {
        titleRow.add(component, "aligny center");
        return this;
    }

    public PageHeader setSubtitle(String text) {
        subtitle.setText(text);
        subtitle.setVisible(text != null && !text.isEmpty());
        return this;
    }

    /** Shows "← text" above the title; clicking it runs the action. */
    public PageHeader setBack(String text, Runnable action) {
        back.setText(text);
        backAction = action;
        back.setVisible(true);
        return this;
    }

    public void setTitle(String text) {
        title.setText(text);
    }
}
