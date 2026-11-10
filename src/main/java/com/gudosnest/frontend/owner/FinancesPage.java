package com.gudosnest.frontend.owner;

import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;

/** Placeholder: income, expenses and profit per property. Not built yet (see docs/TODO.md). */
public class FinancesPage extends BasePage {

    public FinancesPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        add(new PageHeader("Finances"), "gapbottom 24");
        add(Ui.emptyState("Coming soon"), "grow");
    }
}
