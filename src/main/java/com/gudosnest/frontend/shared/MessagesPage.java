package com.gudosnest.frontend.shared;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;

/** Placeholder: conversations between owners and renters. Not built yet (see docs/TODO.md). */
public class MessagesPage extends BasePage {

    public MessagesPage(Portal portal) {
        super(portal);
    }

    /** Opens with a conversation selected; the extra arguments are ignored until Messages is built. */
    public MessagesPage(Portal portal, int otherUserId, String otherName, Booking booking) {
        super(portal);
    }

    @Override
    protected void build() {
        add(new PageHeader("Messages"), "gapbottom 24");
        add(Ui.emptyState("Coming soon"), "grow");
    }
}
