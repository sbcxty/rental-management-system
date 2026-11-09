# Gudo's Nest – build progress

Phase 1 (setup, theme, components, schema) and Phase 2 (login / sign up, portal Factory Method) are done.
Phase 4 (owner portal) is in progress, then Phase 3 (renter portal), then Phase 5 (docs and polish).

Each item: write it → `./mvnw clean compile` (fix errors in that file) → commit.
The build only passes once every page that the portals reference exists.

## Phase 4 – Owner portal

- [x] Dashboard (`DashboardPage`, `PropertyFilter`, `Attention`)
- [x] Booking / refund review dialogs
- [x] Needs attention (`NeedsAttentionPage`)
- [x] Properties (`PropertiesPage`, `PropertyDialog`, `CardGrid`)
- [x] Units of a property (`UnitsPage`, `UnitDialog`)
- [ ] Inventory (`InventoryPage`) and its item dialog (`ItemDialog`, copy-from-unit)
- [ ] Bookings (`BookingsPage`)
- [ ] Payments (`PaymentsPage`)
- [ ] Finances (`FinancesPage`) and Add expense dialog
- [ ] Messages (shared `MessagesPage`, used by both portals)
- [ ] Damage reports (`DamageReportsPage`) and File report dialog

## Phase 3 – Renter portal

- [ ] Browse (`BrowsePage`, Trie location suggestions)
- [ ] Property detail + booking (carousel, price card, book dialog)
- [ ] Booking history (`BookingHistoryPage`)
- [ ] Cancellations (`CancellationsPage`)

## Blocked

(none yet)

## Decisions made without asking

- Work is committed on branch `portal-pages`, not `main` (the repo rule is to branch before committing on the default branch). Merge it when you're happy.
- The first commit also contains the earlier uncommitted Phase 2 / Phase 4 work, because `git add -A` was requested.
- Bookings and Payments pages were added to the list: `OwnerPortal` references them, so the app can't run without them.
- Units page follows the Stitch "Unit Picker" screen but without the Maintenance tab/stat (unit status 'maintenance' was removed) and without "Manage calendar" / "Block dates" (no such feature). The fourth stat is "Items needing attention" instead.
- Units have no photos of their own, so each unit card shows one of the property's photos in turn.
- A house's rates are edited in Edit property (and the unit card's pencil); a house can't add, duplicate or delete units.
- Picked photos are copied into `uploads/properties/` only when the property is saved, under a new random name.
- Buttons, not links, are used for actions inside clickable cards, so clicking them doesn't also open the card.
