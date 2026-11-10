# Gudo's Nest – build progress

Phase 1 (setup, theme, components, schema) and Phase 2 (login / sign up, portal Factory Method) are done.
Phase 4 (owner portal) is in progress, then Phase 3 (renter portal), then Phase 5 (docs and polish).

Each item: write it → `./mvnw clean compile` (fix errors in that file) → commit.
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
- [ ] Messages (shared `MessagesPage`, placeholder, used by both portals)
- [ ] Damage reports (`DamageReportsPage`, placeholder) and File report dialog

## Phase 3 – Renter portal

- [ ] Browse (`BrowsePage`, placeholder, Trie location suggestions)
- [ ] Property detail + booking (carousel, price card, book dialog) – not started
- [ ] Booking history (`BookingHistoryPage`, placeholder)
- [ ] Cancellations (`CancellationsPage`, placeholder)

## Blocked

(none)

## Broken or missing

- "Message renter" / "Message guest" buttons open the placeholder Messages page, not a conversation.
- `RenterPortal.openCancellation(...)` has no caller yet.

## Decisions made without asking

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
