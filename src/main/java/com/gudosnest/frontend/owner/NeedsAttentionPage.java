package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.TabBar;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.shared.BookingUi;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the owner has to act on, most urgent first (from the AttentionQueue in {@link Attention}).
 * Opened from the dashboard's "Needs attention" card. Tabs: 0 = All, 1 = Refund requests,
 * 2 = Pending bookings. Clicking a row opens the review dialog for it.
 */
public class NeedsAttentionPage extends BasePage {

    static final int TAB_ALL = 0;
    static final int TAB_REFUNDS = 1;
    static final int TAB_PENDING = 2;

    private int tab;
    private Integer propertyId;

    public NeedsAttentionPage(Portal portal, int tab, Integer propertyId) {
        super(portal);
        this.tab = tab;
        this.propertyId = propertyId;
    }

    @Override
    protected void build() {
        int ownerId = portal.user().getId();
        List<Property> properties = portal.owner.properties(ownerId);
        List<Booking> bookings = portal.bookings.forOwner(ownerId);
        Attention attention = Attention.load(portal, bookings, propertyId);

        PageHeader header = new PageHeader("Needs attention");
        header.setBack("Back to dashboard", portal::back);
        header.setSubtitle("Pending bookings and refund requests, most urgent first.");
        PropertyFilter filter = new PropertyFilter(properties, propertyId);
        filter.onChange(() -> {
            propertyId = filter.selectedId();
            reload();
        });
        header.addAction(filter);
        add(header, "gapbottom 24");

        TabBar tabs = new TabBar("All", "Refund requests", "Pending bookings");
        tabs.setCount(TAB_ALL, attention.total());
        tabs.setCount(TAB_REFUNDS, attention.refundCount);
        tabs.setCount(TAB_PENDING, attention.pendingCount);
        tabs.setSelectedIndex(tab);
        tabs.onChange(index -> {      // listener added after setSelectedIndex so it doesn't rebuild here
            tab = index;
            reload();
        });
        add(tabs, "gapbottom 16");

        List<Attention.Item> shown = attention.items.stream().filter(this::inTab).toList();
        Card list = new Card(new MigLayout("insets 8 16 8 16, gap 0, fillx, wrap 1", "[grow,fill]"));
        if (shown.isEmpty()) {
            list.add(Ui.muted(emptyMessage()), "gap 8 8 16 16");
        }
        for (int i = 0; i < shown.size(); i++) {
            if (i > 0) {
                list.add(Ui.divider());
            }
            list.add(row(shown.get(i)));
        }
        add(list);
    }

    private boolean inTab(Attention.Item item) {
        return switch (tab) {
            case TAB_REFUNDS -> item.isRefund();
            case TAB_PENDING -> !item.isRefund();
            default -> true;
        };
    }

    private String emptyMessage() {
        return switch (tab) {
            case TAB_REFUNDS -> "No refund requests to review.";
            case TAB_PENDING -> "No bookings are waiting for approval.";
            default -> "Nothing to review. New bookings and refund requests appear here.";
        };
    }

    // ----------------------------------------------------------------------- rows

    private JComponent row(Attention.Item item) {
        Booking b = item.subject();
        Card row = Widgets.clickableCard(new MigLayout("insets 14 8 14 8, gap 16, fillx",
                "[][grow,fill][right][]"), () -> review(item));
        row.setBorder(BorderFactory.createEmptyBorder());

        row.add(item.isRefund()
                ? Widgets.iconTile("receipt", Colors.PRIMARY_TINT, Colors.DANGER)
                : Widgets.iconTile("calendar-check", Colors.PRIMARY_TINT, Colors.PRIMARY), "w 40!, h 40!");

        JPanel text = Ui.transparent("insets 0, gap 0 2, wrap 1", "[grow,fill]");
        JPanel titleRow = Ui.transparent("insets 0, gap 8", "[][]");
        titleRow.add(Ui.label(BookingUi.place(b), Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
        titleRow.add(new Badge(item.isRefund() ? "Refund request" : "Pending booking", Badge.Tone.NEUTRAL));
        text.add(titleRow, "wmin 0");
        text.add(Ui.caption(b.getRenterName() + " · " + Formats.ref(b.getId(), b.getCreatedAt())
                + " · " + BookingUi.dates(b) + " · " + Formats.guests(b.getGuests())), "wmin 0");
        row.add(text, "wmin 0");

        JPanel right = Ui.transparent("insets 0, gap 0 2, wrap 1", "[right]");
        if (item.isRefund()) {
            right.add(Ui.label(Formats.money(item.refund().amount()), Fonts.bodyBold(), Colors.TEXT));
            right.add(Ui.caption(refundDue(item)));
        } else {
            right.add(Ui.label(Formats.money(b.getTotal()), Fonts.bodyBold(), Colors.TEXT));
            right.add(Ui.label(BookingUi.respondWithin(b), Fonts.caption(), urgent(b.getExpiresAt())
                    ? Colors.DANGER : Colors.TEXT_MUTED));
        }
        row.add(right);
        row.add(new JLabel(Icons.get("chevron-right", 16, Colors.TEXT_MUTED), SwingConstants.CENTER));
        return row;
    }

    private static String refundDue(Attention.Item item) {
        return "Requested " + Formats.dateTime(item.refund().requestedAt());
    }

    /** Less than 12 hours left. */
    private static boolean urgent(LocalDateTime due) {
        return Duration.between(LocalDateTime.now(), due).toHours() < 12;
    }

    private void review(Attention.Item item) {
        boolean changed = item.isRefund()
                ? RefundReviewDialog.show(this, portal, item.refund())
                : BookingReviewDialog.show(this, portal, item.booking());
        if (changed) {
            reload();
            portal.refreshBadges();
        }
    }
}
