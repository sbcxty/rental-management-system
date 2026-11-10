package com.gudosnest.frontend.renter;

import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;

/** Placeholder: cancel bookings and request refunds. Not built yet (see docs/TODO.md). */
public class CancellationsPage extends BasePage {

    public CancellationsPage(Portal portal, Integer bookingId) {
        super(portal);
    }

    @Override
    protected void build() {
        add(new PageHeader("Cancellations"), "gapbottom 24");
        add(Ui.emptyState("Coming soon"), "grow");
    }
}
