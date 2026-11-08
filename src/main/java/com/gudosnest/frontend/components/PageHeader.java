package com.gudosnest.frontend.components;

import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Page title on the left, optional actions on the right. No subtitle, no eyebrow label.
 */
public class PageHeader extends JPanel {

    private final JLabel title;
    private final JPanel left;
    private final JPanel actions;

    public PageHeader(String titleText) {
        super(new MigLayout("insets 0, fillx", "[grow][]", "[center]"));
        setOpaque(false);
        title = Ui.pageTitle(titleText);
        left = Ui.transparent("insets 0, gap 16", "[]");
        left.add(title);
        actions = Ui.transparent("insets 0, gap 8", "");
        add(left, "growx");
        add(actions);
    }

    /** Adds a control on the right side (buttons, dropdowns). */
    public PageHeader addAction(JComponent component) {
        actions.add(component);
        return this;
    }

    /** Adds a control right after the title (for example the inventory "Viewing" dropdown). */
    public PageHeader addNextToTitle(JComponent component) {
        left.add(component);
        return this;
    }

    public void setTitle(String text) {
        title.setText(text);
    }
}
