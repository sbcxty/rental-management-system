package com.gudosnest.frontend.shared;

import com.formdev.flatlaf.util.UIScale;
import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.Contact;
import com.gudosnest.backend.model.Records.Conversation;
import com.gudosnest.backend.model.Records.Message;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppDialog;
import com.gudosnest.frontend.components.Avatar;
import com.gudosnest.frontend.components.Badge;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.ChatBubble;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Segmented;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.owner.BookingReviewDialog;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.renter.RenterPortal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conversations between renters and owners, shared by both portals. Three columns: the people
 * on the left (search, All / Unread), the chat in the middle, and the booking the conversation
 * is about on the right. A conversation is everything between one renter-owner pair; it starts
 * from a booking ("Message renter"), so there is no "new conversation" button.
 */
public class MessagesPage extends BasePage {

    private Integer selectedId;
    private String selectedName;
    private Booking selectedBooking;

    // Kept across reloads so sending a message or switching chats doesn't reset the list
    private String search = "";
    private boolean unreadOnly;
    private boolean focusInput;

    private List<Conversation> conversations = List.of();
    private JPanel listRows;

    public MessagesPage(Portal portal) {
        super(portal);
    }

    /** Opens with the conversation with this person selected (shown even before the first message). */
    public MessagesPage(Portal portal, int otherUserId, String otherName, Booking booking) {
        super(portal);
        selectedId = otherUserId;
        selectedName = otherName;
        selectedBooking = booking;
    }

    /** Each column scrolls by itself, so the page fills the window instead of scrolling. */
    @Override
    public boolean scrolls() {
        return false;
    }

    private boolean forOwner() {
        return portal.kind() == SessionManager.Portal.OWNER;
    }

    private int me() {
        return portal.user().getId();
    }

    @Override
    protected void build() {
        add(new PageHeader("Messages"), "gapbottom 24");
        loadConversations();

        // Side columns shrink a little on narrow windows (a 125% display is ~1280px wide) so the
        // chat keeps a usable width and nothing gets cut off on the right.
        JPanel columns = Ui.transparent("insets 0, gap 16, fill", "[240:280:280][grow,fill][256:300:300]");
        // hmin 0: each column scrolls inside, so short windows shrink the columns, not overflow
        columns.add(buildList(), "grow, wmin 0, hmin 0");
        columns.add(buildChat(), "grow, wmin 320, hmin 0");
        columns.add(buildBookingPanel(), "grow, wmin 0, hmin 0");
        add(columns, "grow, push, hmin 0");
    }

    /**
     * Loads the list and marks the open conversation as read. With nothing selected, opens the most
     * recent conversation. A person opened from a booking or the search but never messaged gets a
     * temporary row at the top until the first message is sent.
     */
    private void loadConversations() {
        if (selectedId != null) {
            portal.messages.markRead(me(), selectedId);
        }
        conversations = new ArrayList<>(portal.messages.conversations(me(), portal.kind()));

        if (selectedId == null && !conversations.isEmpty()) {
            Conversation latest = conversations.get(0);
            selectedId = latest.otherUserId();
            portal.messages.markRead(me(), selectedId);
            conversations = new ArrayList<>(portal.messages.conversations(me(), portal.kind()));
        }

        if (selectedId != null) {
            Conversation current = find(selectedId);
            if (current == null) {
                conversations.add(0, new Conversation(selectedId, selectedName, null, null, 0, selectedBooking));
            } else {
                selectedName = current.otherName();
                if (selectedBooking == null) {
                    selectedBooking = current.booking();
                }
            }
        }
        portal.refreshBadges();
    }

    private Conversation find(int otherId) {
        for (Conversation c : conversations) {
            if (c.otherUserId() == otherId) {
                return c;
            }
        }
        return null;
    }

    private void open(Conversation c) {
        if (selectedId != null && c.otherUserId() == selectedId) {
            return;
        }
        selectedId = c.otherUserId();
        selectedName = c.otherName();
        selectedBooking = c.booking();
        reload();
    }

    // ---------------------------------------------------------------- left: conversation list

    private JComponent buildList() {
        Card card = new Card(new MigLayout("insets 16, fill, gap 0, wrap 1", "[grow,fill]", "[]12[]8[grow]"));

        JTextField searchField = Ui.searchField(forOwner() ? "Search renters" : "Search owners or properties");
        searchField.setText(search);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                search = searchField.getText().strip();
                fillRows();   // only the rows, so the search box keeps focus
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
        card.add(searchField, "h " + Theme.CONTROL_HEIGHT + "!");

        Segmented chips = new Segmented("All", "Unread");
        long unread = conversations.stream().filter(c -> c.unread() > 0).count();
        if (unread > 0) {
            chips.setText(1, "Unread (" + unread + ")");
        }
        if (unreadOnly) {
            chips.setSelectedIndex(1);
        }
        chips.onChange(i -> {
            unreadOnly = i == 1;
            fillRows();
        });
        card.add(chips, "growx 0, alignx left");

        listRows = Ui.transparent("insets 0, gap 0 4, fillx, wrap 1", "[grow,fill]");
        fillRows();
        card.add(whiteScroll(listRows), "grow, hmin 0");
        return card;
    }

    /**
     * The conversations that match the search and filter, then (while searching) people to start a
     * new conversation with: owners or properties for renters, renters for owners.
     */
    private void fillRows() {
        listRows.removeAll();
        String query = search.toLowerCase(Locale.ROOT);
        int shown = 0;
        for (Conversation c : conversations) {
            if (unreadOnly && c.unread() == 0) {
                continue;
            }
            if (!query.isEmpty() && !matches(c, query)) {
                continue;
            }
            listRows.add(conversationRow(c));
            shown++;
        }

        if (!query.isEmpty() && !unreadOnly) {
            try {
                List<Contact> people = new ArrayList<>();
                for (Contact person : portal.messages.findPeople(me(), portal.kind(), search)) {
                    if (find(person.userId()) == null) {   // already in the list above
                        people.add(person);
                    }
                }
                if (!people.isEmpty()) {
                    listRows.add(Ui.label("START A NEW CONVERSATION", Fonts.tableHeader(), Colors.TEXT_MUTED),
                            "gaptop " + (shown > 0 ? 12 : 4) + ", gapbottom 4, gapleft 12");
                    for (Contact person : people) {
                        listRows.add(personRow(person));
                        shown++;
                    }
                }
            } catch (DataException e) {
                listRows.add(Ui.error(e.getMessage()), "gapleft 12");
                shown++;
            }
        }

        if (shown == 0) {
            String empty;
            if (unreadOnly) {
                empty = "No unread messages";
            } else if (!query.isEmpty()) {
                empty = forOwner() ? "No renters match" : "No owners or properties match";
            } else {
                empty = forOwner() ? "No conversations yet. Search for a renter to start one."
                        : "No conversations yet. Search for an owner or a property to start one.";
            }
            listRows.add(Widgets.wrap(empty, Fonts.body(), Colors.TEXT_MUTED), "wmin 0, gaptop 16, gapleft 12");
        }
        listRows.revalidate();
        listRows.repaint();
    }

    /** Someone found by the search: name, and for owners the properties they rent out. */
    private JComponent personRow(Contact person) {
        RowPanel row = new RowPanel(false, () -> openPerson(person));
        JPanel top = Ui.transparent("insets 0, gap 8, fillx", "[][grow,fill]");
        top.add(new Avatar(initials(person.name()), 32), "spany 2, aligny center");
        top.add(Ui.label(person.name(), Fonts.bodyMedium(), Colors.TEXT), "wmin 0, wrap");
        String detail = person.properties() != null ? person.properties() : forOwner() ? "Renter" : "Owner";
        top.add(Ui.caption(detail), "wmin 0");
        row.add(top, "wmin 0");
        return row;
    }

    /** Opens an empty chat with someone from the search; the first message creates the conversation. */
    private void openPerson(Contact person) {
        selectedId = person.userId();
        selectedName = person.name();
        selectedBooking = null;
        search = "";          // so the new conversation shows at the top of the full list
        unreadOnly = false;
        reload();
    }

    private static boolean matches(Conversation c, String query) {
        return c.otherName().toLowerCase(Locale.ROOT).contains(query)
                || (c.booking() != null && c.booking().getPropertyName().toLowerCase(Locale.ROOT).contains(query));
    }

    /**
     * Owner: renter name, unread dot and time; the property; the last message (cut off with "…").
     * Renter: the property first, then "Elena Vance • Host", the last message, and the stay dates.
     */
    private JComponent conversationRow(Conversation c) {
        boolean selected = selectedId != null && c.otherUserId() == selectedId;
        RowPanel row = new RowPanel(selected, () -> open(c));
        Booking b = c.booking();

        JPanel top = Ui.transparent("insets 0, gap 6, fillx", "[grow,fill][][]");
        if (forOwner() || b == null) {
            top.add(Ui.label(c.otherName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        } else {
            top.add(Ui.label(b.getPropertyName(), Fonts.captionMedium(), Colors.PRIMARY), "wmin 0");
        }
        if (c.unread() > 0) {
            top.add(new Dot(), "aligny center");
        }
        top.add(Ui.caption(Formats.when(c.lastAt())), "aligny center");
        row.add(top, "wmin 0");

        if (forOwner()) {
            if (b != null) {
                row.add(Ui.label(b.getPropertyName(), Fonts.caption(), Colors.SECONDARY), "wmin 0");
            }
        } else if (b == null) {
            row.add(Ui.caption("Host"), "wmin 0");
        } else {
            JPanel who = Ui.transparent("insets 0, gap 4", "[][]");
            who.add(Ui.label(c.otherName(), Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
            who.add(Ui.caption("• Host"));
            row.add(who, "wmin 0, gaptop 2");
        }
        JLabel preview = c.lastText() == null
                ? Ui.caption("No messages yet")
                : Ui.label(c.lastText().replace('\n', ' '), Fonts.body(),
                        c.unread() > 0 ? Colors.TEXT : Colors.TEXT_MUTED);
        row.add(preview, "wmin 0, gaptop 2");
        if (!forOwner() && b != null) {
            row.add(Ui.iconLabel(BookingUi.dates(b), "calendar-days", Fonts.caption(), Colors.TEXT_MUTED),
                    "wmin 0, gaptop 4");
        }
        return row;
    }

    // ---------------------------------------------------------------- middle: chat

    private JComponent buildChat() {
        if (selectedId == null) {
            Card empty = new Card(new MigLayout("insets 16, fill", "[center]", "[center]"));
            empty.add(Ui.muted(forOwner() ? "Pick a conversation, or search for a renter to message."
                    : "Pick a conversation, or search for an owner to message."));
            return empty;
        }
        Card card = new Card(new MigLayout("insets 16, fill, gap 0, wrap 1", "[grow,fill]", "[][][grow][]"));

        boolean viewStay = !forOwner() && selectedBooking != null;
        JPanel head = Ui.transparent("insets 0 0 12 0, gap 12 0, fillx", "[][grow,fill][]");
        // Without a booking there's no second line, so the avatar and name share one centered row.
        head.add(new Avatar(initials(selectedName), 40), selectedBooking != null ? "spany 2, aligny center" : "");
        head.add(Ui.label(selectedName, Fonts.sectionTitle(), Colors.TEXT),
                "wmin 0, aligny center" + (viewStay ? "" : ", wrap"));
        if (viewStay) {
            AppButton stay = new AppButton("View stay", AppButton.Style.OUTLINED).withIcon("house");
            stay.addActionListener(e -> showStay(selectedBooking));
            head.add(stay, "spany 2, aligny center, wrap");
        }
        if (selectedBooking != null) {
            head.add(Ui.label(BookingUi.place(selectedBooking), Fonts.caption(), Colors.SECONDARY),
                    "wmin 0, skip 1");
        }
        card.add(head);
        card.add(Ui.divider());

        JPanel history = Ui.transparent("insets 16 4 16 4, fillx, wrap 1, gap 0", "[grow,fill]");
        List<Message> messages = portal.messages.messages(me(), selectedId);
        if (messages.isEmpty()) {
            history.add(Ui.emptyState("No messages yet. Say hello to " + firstName(selectedName) + "."));
        }
        LocalDate lastDay = null;
        for (Message m : messages) {
            LocalDate day = m.sentAt().toLocalDate();
            if (!day.equals(lastDay)) {
                history.add(new Badge(Formats.dayHeading(day), Badge.Tone.MUTED),
                        "growx 0, alignx center, gaptop 4, gapbottom 16");
                lastDay = day;
            }
            history.add(messageRow(m, history), "gapbottom 12");
        }
        JScrollPane scroll = whiteScroll(history);
        // Bubbles size themselves from the history width, so lay out again when it changes.
        history.addComponentListener(new ComponentAdapter() {
            private int width = -1;

            @Override
            public void componentResized(ComponentEvent e) {
                if (history.getWidth() != width) {
                    width = history.getWidth();
                    history.revalidate();
                    scrollToBottom(scroll);
                }
            }
        });
        card.add(scroll, "grow, hmin 0");
        scrollToBottom(scroll);

        card.add(composer(), "gaptop 12");
        return card;
    }

    /** One message: the bubble with its time under it, on the right if I sent it. */
    private JComponent messageRow(Message m, JComponent history) {
        boolean mine = m.senderId() == me();
        String side = mine ? "alignx right" : "alignx left";
        JPanel row = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow]");
        row.add(new ChatBubble(m.text(), mine, history), side);
        row.add(Ui.caption(Formats.time(m.sentAt())), side + ", gaptop 4");
        return row;
    }

    private JComponent composer() {
        JPanel bar = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill][]");
        JTextField input = Ui.textField("Write a message to " + firstName(selectedName) + "...");
        AppButton send = new AppButton("Send", AppButton.Style.PRIMARY).withIcon("send");
        Integer bookingId = selectedBooking == null ? null : selectedBooking.getId();
        Runnable doSend = () -> {
            String text = input.getText();
            if (text.isBlank()) {
                return;
            }
            focusInput = true;
            // On success the page reloads with the new message; on error the text stays in the box.
            act(() -> portal.messages.send(me(), selectedId, bookingId, text), null);
        };
        input.addActionListener(e -> doSend.run());   // Enter sends
        send.addActionListener(e -> doSend.run());
        bar.add(input, "h " + Theme.CONTROL_HEIGHT + "!");
        bar.add(send, "h " + Theme.CONTROL_HEIGHT + "!");

        if (focusInput) {
            focusInput = false;
            SwingUtilities.invokeLater(input::requestFocusInWindow);
        }
        return bar;
    }

    // ---------------------------------------------------------------- right: linked booking

    private JComponent buildBookingPanel() {
        Card card = new Card(new MigLayout("insets 16, fill, gap 0, wrap 1", "[grow,fill]", "[grow][]"));
        Booking b = selectedBooking;
        if (b == null) {
            String text = selectedId == null ? "Booking details show here."
                    : "No booking with " + firstName(selectedName) + " yet.";
            card.add(Widgets.wrap(text, Fonts.body(), Colors.TEXT_MUTED), "wmin 0, aligny top");
            return card;
        }

        // The details scroll if the window is short; the button stays pinned at the bottom.
        JPanel details = Ui.transparent("insets 0, gap 0, fillx, wrap 1", "[grow,fill]");
        details.add(new Widgets.Photo(b.getCoverImage(), Theme.CONTROL_RADIUS, true), "h 96!, gapbottom 12");
        JPanel title = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill][]");
        title.add(Ui.label(b.getPropertyName(), Fonts.cardTitle(), Colors.TEXT), "wmin 0");
        title.add(BookingUi.badge(b), "aligny center");
        details.add(title);
        details.add(Ui.iconLabel(b.getLocation(), "map-pin", Fonts.caption(), Colors.TEXT_MUTED),
                "wmin 0, gaptop 4, gapbottom 16");

        details.add(detail("Reference", Formats.refCode(b.getId(), b.getCreatedAt()), false), "gapbottom 8");
        details.add(detail("Unit", b.getUnitLabel(), false), "gapbottom 8");   // "Whole house" for houses
        JPanel stay = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill,sg stay][grow,fill,sg stay]");
        stay.add(stayTile("Check-in", b.getCheckIn()), "wmin 0");
        stay.add(stayTile("Check-out", b.getCheckOut()), "wmin 0");
        details.add(stay, "gapbottom 8");
        details.add(detail("Guests", Formats.guests(b.getGuests()), false), "gapbottom 8");
        details.add(detail("Total", Formats.money(b.getTotal()), true));
        boolean cancellable = b.getStage() == Booking.Stage.PENDING || b.getStage() == Booking.Stage.UPCOMING;
        if (!forOwner() && cancellable) {
            details.add(Widgets.policyBox(), "gaptop 16, wmin 0");
        }
        card.add(whiteScroll(details), "grow, hmin 0");

        if (forOwner()) {
            AppButton view = new AppButton("View booking", AppButton.Style.PRIMARY);
            view.addActionListener(e -> {
                if (BookingReviewDialog.show(this, portal, b)) {
                    reload();
                }
            });
            card.add(view, "gaptop 16, h " + Theme.CONTROL_HEIGHT + "!");
        } else {
            // Renter: receipt (once paid) and cancellation (only before check-in). Outlined, no second
            // accent: the Cancellations page shows the refund and asks to confirm before cancelling.
            // One row, side by side, so the booking details keep their room on short windows.
            JPanel actions = Ui.transparent("insets 0, gap 8, fillx", "[grow,fill,sg a][grow,fill,sg a]");
            if (b.getPaidAt() != null) {
                AppButton receipt = new AppButton("View receipt", AppButton.Style.OUTLINED);
                receipt.addActionListener(e -> BookingUi.showPaymentReceipt(this, b));
                actions.add(receipt, "wmin 0, h " + Theme.CONTROL_HEIGHT + "!");
            }
            // "Cancel", not "Request": a renter's cancel takes effect at once (only the refund is reviewed).
            if (cancellable && portal instanceof RenterPortal renter) {
                AppButton cancel = new AppButton("Cancel stay", AppButton.Style.OUTLINED);
                cancel.addActionListener(e -> renter.openCancellation(b.getId()));
                actions.add(cancel, "wmin 0, h " + Theme.CONTROL_HEIGHT + "!");
            }
            if (actions.getComponentCount() > 0) {
                card.add(actions, "gaptop 16");
            }
        }
        return card;
    }

    /** Renter's read-only look at the stay: status, dates, payment, and the receipt. */
    private void showStay(Booking b) {
        AppDialog dialog = new AppDialog(this, "Booking " + Formats.refCode(b.getId(), b.getCreatedAt()));
        JPanel body = dialog.getBody();

        JPanel head = Ui.transparent("insets 0, gap 8, fillx", "[grow][]");
        JPanel names = Ui.transparent("insets 0, gap 0, wrap 1", "[]");
        names.add(Ui.label(b.getPropertyName(), Fonts.cardTitle(), Colors.TEXT));
        names.add(Ui.muted(b.getUnitLabel() + " · " + b.getLocation()));
        head.add(names);
        head.add(BookingUi.badge(b), "aligny top");
        body.add(head, "gapbottom 8");

        body.add(BookingUi.banner(b, false), "gapbottom 4");
        body.add(BookingUi.stayStrip(b), "gapbottom 4");
        if (b.getSpecialRequests() != null) {
            body.add(Ui.label("Special requests", Fonts.captionMedium(), Colors.TEXT_MUTED));
            body.add(Widgets.wrap(b.getSpecialRequests(), Fonts.body(), Colors.TEXT), "wmin 0, gapbottom 4");
        }
        body.add(Ui.label("Payment", Fonts.captionMedium(), Colors.TEXT_MUTED));
        body.add(BookingUi.paymentSummary(b));

        if (b.getPaidAt() != null) {
            AppButton receipt = new AppButton("View receipt", AppButton.Style.TEXT).withIcon("receipt");
            receipt.addActionListener(e -> BookingUi.showPaymentReceipt(dialog, b));
            dialog.addLeftButton(receipt);
        }
        dialog.addCloseButton("Close");
        dialog.showDialog(640);
    }

    /** "Label ..... value" in a tinted box; the total is larger and on a stronger tint. */
    private static JComponent detail(String label, String value, boolean strong) {
        Card box = Widgets.tintBox(new MigLayout("insets 8 12 8 12, gap 8, fillx", "[][grow,right]"));
        if (!strong) {
            box.setBackground(Colors.SURFACE_HOVER);
        }
        box.add(Ui.caption(label));
        box.add(Ui.label(value, strong ? Fonts.cardTitle() : Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
        return box;
    }

    /** Check-in or check-out: label, date, then time, so it fits half the panel width. */
    private static JComponent stayTile(String label, java.time.LocalDateTime at) {
        Card box = Widgets.tintBox(new MigLayout("insets 8 12 8 12, gap 0, fillx, wrap 1", "[grow,fill]"));
        box.setBackground(Colors.SURFACE_HOVER);
        box.add(Ui.caption(label), "gapbottom 2");
        box.add(Ui.label(Formats.date(at), Fonts.bodyMedium(), Colors.TEXT), "wmin 0");
        box.add(Ui.caption(Formats.time(at)), "wmin 0");
        return box;
    }

    // ---------------------------------------------------------------- helpers

    /** A scroll area on white (Ui.scroll uses the page color), for scrolling inside a card. */
    private static JScrollPane whiteScroll(JComponent content) {
        JScrollPane scroll = Ui.scroll(content);
        scroll.getViewport().setBackground(Colors.CARD);
        scroll.getViewport().getView().setBackground(Colors.CARD);
        return scroll;
    }

    /** Scrolls to the newest message once the layout has settled. */
    private static void scrollToBottom(JScrollPane scroll) {
        SwingUtilities.invokeLater(() -> {
            scroll.validate();
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private static String firstName(String name) {
        return name.trim().split("\\s+")[0];
    }

    /** A conversation in the list: rounded, tinted when selected, lighter tint on hover. */
    private static final class RowPanel extends JPanel {
        private final boolean selected;
        private boolean hover;

        RowPanel(boolean selected, Runnable onClick) {
            super(new MigLayout("insets 10 12 10 12, gap 0, fillx, wrap 1", "[grow,fill]", "[]2[]"));
            this.selected = selected;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Labels inside have no mouse listeners, so their clicks and hovers reach this panel.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color fill = selected ? Colors.PRIMARY_TINT : hover ? Colors.SURFACE_HOVER : null;
            if (fill != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                int arc = UIScale.scale(Theme.CONTROL_RADIUS * 2);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** The small Primary dot next to the time of a conversation with unread messages. */
    private static final class Dot extends JComponent {
        private static final int SIZE = 8;

        Dot() {
            Dimension size = new Dimension(UIScale.scale(SIZE), UIScale.scale(SIZE));
            setPreferredSize(size);
            setMinimumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Colors.PRIMARY);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
