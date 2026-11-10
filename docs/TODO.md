# Gudo's Nest – TODO

What is left to build. Keep this in step with `PROGRESS.md` (same folder).

## Placeholder pages (show only a header and "Coming soon")

- **Finances** – `src/main/java/com/gudosnest/frontend/owner/FinancesPage.java`. Show income, expenses and profit per property, with an "Add expense" dialog.
- **Damage reports** – `src/main/java/com/gudosnest/frontend/owner/DamageReportsPage.java`. List the owner's damage reports and add a "File report" dialog; a report goes from open to charged (creates a damage fee payment) or dismissed.
- **Messages** – `src/main/java/com/gudosnest/frontend/shared/MessagesPage.java`. Shared by both portals: a list of renter–owner conversations and a chat view. The second constructor (open a given conversation) currently ignores its arguments.
- **Browse** – `src/main/java/com/gudosnest/frontend/renter/BrowsePage.java`. Search properties by location with Trie suggestions and open a property.
- **Booking history** – `src/main/java/com/gudosnest/frontend/renter/BookingHistoryPage.java`. List the renter's past and upcoming bookings with their status and receipts.
- **Cancellations** – `src/main/java/com/gudosnest/frontend/renter/CancellationsPage.java`. Cancel a booking and request a refund; the `bookingId` argument (preselected booking) is currently ignored.

## Not started

- **Property detail and booking** (renter). Photo carousel, price card and a book dialog; no file exists yet.

## Broken or missing

- The "Message renter" / "Message guest" buttons in the owner dialogs and Units page open the placeholder Messages page instead of a conversation.
- `RenterPortal.openCancellation(...)` has no caller yet; the renter booking dialog that should use it doesn't exist.
- The startup check only confirmed the app runs for 40 seconds without errors; the window and login were not checked by hand.
