package com.gudosnest.frontend.portal;

import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.theme.Colors;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;

/**
 * Every portal page: page padding and background, a {@link #reload()} that rebuilds the page from
 * the database, and {@link #act} to run an action and show its error (if any) to the person.
 */
public abstract class BasePage extends JPanel {

    protected final Portal portal;

    protected BasePage(Portal portal) {
        super(new MigLayout("insets 32 40 40 40, fillx, gap 0, wrap 1", "[grow,fill]"));
        this.portal = portal;
        setBackground(Colors.PAGE);
    }

    /** Builds the page content. Called by {@link #reload()}. */
    protected abstract void build();

    /** Clears the page and builds it again from fresh data. */
    public final void reload() {
        removeAll();
        try {
            build();
        } catch (DataException | RuleException e) {
            removeAll();
            add(Ui.emptyState(e.getMessage()), "grow");
        }
        revalidate();
        repaint();
    }

    /** False for pages that manage their own scrolling (Messages). */
    public boolean scrolls() {
        return true;
    }

    /**
     * Runs an action that changes data. On success shows the message (if any) and reloads the page;
     * on a broken rule or database error shows the reason instead.
     */
    protected boolean act(Runnable action, String successMessage) {
        try {
            action.run();
        } catch (RuleException | DataException e) {
            AppDialog.error(this, e.getMessage());
            return false;
        }
        if (successMessage != null) {
            AppDialog.info(this, successMessage);
        }
        reload();
        portal.refreshBadges();
        return true;
    }
}
