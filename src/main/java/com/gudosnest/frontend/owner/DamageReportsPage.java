package com.gudosnest.frontend.owner;

import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;

/** Placeholder: damage reports filed against stays. Not built yet (see docs/TODO.md). */
public class DamageReportsPage extends BasePage {

    public DamageReportsPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        add(new PageHeader("Damage reports"), "gapbottom 24");
        add(Ui.emptyState("Coming soon"), "grow");
    }
}
