package com.gudosnest.frontend.renter;

import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;

/** Placeholder: search and book properties. Not built yet (see docs/TODO.md). */
public class BrowsePage extends BasePage {

    public BrowsePage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        add(new PageHeader("Browse"), "gapbottom 24");
        add(Ui.emptyState("Coming soon"), "grow");
    }
}
