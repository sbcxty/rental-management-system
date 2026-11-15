# Gudo's Nest – TODO

What is left to build. Keep this in step with `PROGRESS.md` (same folder). The rules every screen must follow are in `BUSINESS_RULES.md`.

## Placeholder pages (show only a header and "Coming soon")

- **Finances** – `src/main/java/com/gudosnest/frontend/owner/FinancesPage.java`. Show income, expenses and profit per property, with an "Add expense" dialog. Read the amount with `MoneyInput.parse`.
- **Booking history** – `src/main/java/com/gudosnest/frontend/renter/BookingHistoryPage.java`. List the renter's past and upcoming bookings with their status and receipts.

## Broken or missing

- PR #2's title lists Booking history, but `BookingHistoryPage` is still a placeholder.
- `CancellationsPage` (from PR #2) compiles and uses the preselected booking id, but it hasn't been reviewed against the cancellation rules yet.
- Add a "Message host" button (calls `portal.openConversation(b.getOwnerId(), b.getOwnerName(), b)`) to the renter booking dialog when Booking history is built, so the chat is linked to that booking. (The property dialog and the "Payment received" screen after booking already have one.)
- The startup check only confirmed the app runs for 40 seconds without errors; the window and login were not checked by hand.
- Browse search and availability were tested against the seed data, but the property dialog (`BookDialog`) wasn't clicked through: try one booking by hand, including a 7+ night stay (weekly discount line).
