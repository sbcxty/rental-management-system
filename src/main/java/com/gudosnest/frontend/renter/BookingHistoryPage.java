package com.gudosnest.frontend.renter;

import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;

/** Placeholder: the renter's past and upcoming bookings. Not built yet (see docs/TODO.md). */
public class BookingHistoryPage extends BasePage {

    public BookingHistoryPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        add(new PageHeader("Booking history"), "gapbottom 24");
        add(Ui.emptyState("Coming soon"), "grow");
    }
}
