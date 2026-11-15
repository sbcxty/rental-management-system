# Gudo's Nest – build progress

Phase 1 (setup, theme, components, schema) and Phase 2 (login / sign up, portal Factory Method) are done.
Phase 4 (owner portal) is in progress, then Phase 3 (renter portal), then Phase 5 (docs and polish).

Each item: write it → `./mvnw clean compile` (fix errors in that file) → commit.
The business rules are in `BUSINESS_RULES.md`: when a rule changes, update that file in the same commit.
The build passes: pages not built yet are placeholders ("Coming soon"). See `TODO.md` in this folder for what is left.

## Phase 4 – Owner portal

- [x] Dashboard (`DashboardPage`, `PropertyFilter`, `Attention`)
- [x] Booking / refund review dialogs
- [x] Needs attention (`NeedsAttentionPage`)
- [x] Properties (`PropertiesPage`, `PropertyDialog`, `CardGrid`)
- [x] Units of a property (`UnitsPage`, `UnitDialog`)
- [x] Inventory (`InventoryPage`) and its item dialog (`ItemDialog`, copy-from-unit)
- [x] Bookings (`BookingsPage`)
- [x] Payments (`PaymentsPage`)
- [ ] Finances (`FinancesPage`, placeholder) and Add expense dialog
- [x] Messages (shared `MessagesPage` + `ChatBubble`, owner and renter views)
- [ ] Damage reports (`DamageReportsPage`, placeholder) and File report dialog

## Phase 3 – Renter portal

- [x] Browse (`BrowsePage` from PR #2, `BrowseService`, Trie location suggestions)
- [x] Property detail + booking (`BookDialog`: carousel, price card, confirm, "Payment received"). Nov 15: no Close button (Esc and the window's X close it); price card fills its column so it lines up with Reserve; the dialog is smaller (columns 400 + 320, carousel 165, Unit beside its field, special requests as one line with a placeholder); it is only as tall as its content: picking dates adds the price lines and the dialog grows to fit, then centres itself again (and shrinks back if the dates are cleared)
- [ ] Booking history (`BookingHistoryPage`, placeholder)
- [x] Cancellations (`CancellationsPage`, from PR #2; not reviewed yet). Fixed: the "I understand" tick disappeared after clicking and moving the mouse away (focused checkboxes now stay filled, `Theme`); the reservation field's text is centred vertically; the footer says "Cancelled automatically" (was "Step 1 of 1")

## Blocked

(none)

## Broken or missing

- Renters have no "Message host" button on bookings yet (Booking history isn't built); they can start a conversation from the Messages search instead.
- Messages' "Cancel stay" opens `CancellationsPage`, which is still a placeholder.

## Decisions made without asking

- Cards never showed their 1px outline: setting a FlatLaf style on a panel drops its FlatLineBorder, and `Card` set the border first. `Card` (and the sign-up role boxes in `AuthScreen`) now set the style first, so every card has its light outline (approved Nov 15).

- Work is committed on branch `portal-pages`, not `main` (the repo rule is to branch before committing on the default branch). Merge it when you're happy.
- The first commit also contains the earlier uncommitted Phase 2 / Phase 4 work, because `git add -A` was requested.
- Bookings and Payments pages were added to the list: `OwnerPortal` references them, so the app can't run without them.
- Units page follows the Stitch "Unit Picker" screen but without the Maintenance tab/stat (unit status 'maintenance' was removed) and without "Manage calendar" / "Block dates" (no such feature). The fourth stat is "Items needing attention" instead.
- Units have no photos of their own, so each unit card shows one of the property's photos in turn.
- A house's rates are edited in Edit property (and the unit card's pencil); a house can't add, duplicate or delete units.
- Picked photos are copied into `uploads/properties/` only when the property is saved, under a new random name.
- Buttons, not links, are used for actions inside clickable cards, so clicking them doesn't also open the card.
- Inventory: items shared by the whole property (no unit) appear under a "Shared (whole property)" choice in the unit dropdown, only when some exist. Quantity saves on each +/− click; items needing attention are listed first with a tinted row.
- Bookings: typing a booking reference (e.g. "GN-2026-8815" or "8815") finds it by binary search over the id-sorted list (SortedSearch) and ignores the selected tab; other text filters by renter, property or unit. Rejected and expired bookings are under the Cancelled tab. Tables use a new `RowTableModel` and are sized to their rows so only the page scrolls.
- Payments: the Stitch screenshot only partly rendered, so the layout follows the other list pages: 4 stats (Received, Refunded, Net received, Refunds to review), type tabs, table. A row opens its receipt; a "Requested" refund opens the refund review instead. The property filter matches by property name (payment rows carry no property id).
- Placeholder pages were added for every missing page so the app compiles and runs; they show only a header and "Coming soon".
- Markdown docs live in `docs/` (README.md stays in the root). Skill files under `.claude/skills/` were left in place because Claude Code loads them from there.
- Messages: built from the two Stitch screens, minus what the app has no data for (attach file, online status, reply time, "Inquiries" filter, settings button, house rules). Both portals use All / Unread filters. Each column scrolls inside itself so the page fits a 125% display (~1280×770); the side columns shrink a little on narrow windows.
- Messages (renter): the panel's cancel button says "Cancel stay", not the design's "Request cancellation", because a renter's cancel takes effect at once (only the refund is reviewed). "View stay" opens a read-only booking dialog with the receipt.
- Messages search also finds people to start a conversation with, no booking needed: renters find owners by name or property name, owners find renters by name. A conversation without a booking shows in the portal that matches the other person's role (owners in the Renter portal, renters in the Owner portal).
- Money inputs (Task 15): every amount field (acquisition cost, nightly rate, cleaning fee, item price) is read by `MoneyInput`, which accepts `2800` or `2,800` with up to 2 decimals, rejects anything else with a message naming the field, and caps the value at what the database column holds (99,999,999.99; acquisition cost 9,999,999,999.99). The peso sign isn't accepted: the field label already says (₱). Stored amounts are put back into fields as `1,250,000.00` with `Money.plain`; `Money` is the only place that defines the number format. `Formats.parseMoney` and `Formats.plainMoney` were removed.
- Browse (built to fit PR #2's `BrowsePage`): the search loads one `BrowseService.Catalog` per page load (listings, Trie, unit calendars), so typing and clicking pills don't hit the database. A renter never sees their own properties (two-role users can't book themselves). Dates are optional, but both or neither, and use the same rules as booking (`BookingService.checkDates`: from tomorrow, 1–30 nights). Only approved stays block dates; pending requests don't, as in `BookingService.book`.
- Browse categories: the Stitch pills (Villas, Lofts, Cabins, Work-friendly, Oceanfront, Mountain View) need tags the app doesn't store, so the pills are All homes, Whole houses, Multi-unit and Available now (a unit free today). Ratings, favourites and "Refine filters" were dropped for the same reason.
- Property detail is a dialog (`BookDialog`), not a page, because PR #2's Browse opens it with `BookDialog.show(...)`. Left: carousel, location, host, the unit's description, the app-wide check-in and check-out times (after 2:00 PM, before 11:00 AM) and the cancellation policy. It is not called "House rules", because owners can't write house rules. Right: price card with unit picker (multi-unit only), dates, guests (capped at the unit's capacity), special requests and the live price from `BookingService.quote`. Amenities and quiet hours from the design were dropped (no data).
- Booking flow: Reserve → a confirm box with the full amount and the 48-hour rule → `BookingService.book` (pay first, pending) → the payment receipt image is saved → "Payment received" with the reference, stay, payment, View receipt and Message host. The design's confirm text "Free cancellation up to 48 hours before check-in" contradicts the refund tiers, so the dialog shows the real policy box instead.
- The property dialog also has "Message host", which opens a conversation without a booking (allowed since 2026-11-11).
- Filled buttons (`AppButton` PRIMARY / SECONDARY / DANGER) looked greyed out when they were a dialog's default button (the one Enter presses), because FlatLaf paints default buttons with its own colours. `AppButton` now gives default buttons the same colours. This also fixes the Save buttons in the unit and item dialogs.
- The long-stay discount (10% off the nights for 7+ nights, `Price.LongStayDiscount`) stays app-wide; owners can't change it. The unit fields (unit dialog, and Add property for a house) show a note about it under the rates, built from the same constants.
- Cancellations: the reservation dropdown shows each stay on two lines (property and unit; dates, reference, total) and is never wider than the field, so the open list lines up under it instead of spilling over the page.
