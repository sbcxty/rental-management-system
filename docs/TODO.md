# Gudo's Nest – TODO

What is left to build. Keep this in step with `PROGRESS.md` (same folder). The rules every screen must follow are in `BUSINESS_RULES.md`.

## Broken or missing

- `CancellationsPage` (from PR #2) compiles and uses the preselected booking id, but it hasn't been reviewed against the cancellation rules yet.
- Booking history (`RenterBookingDialog`) wasn't clicked through by hand: try Cancel booking (should open Cancellations with that stay selected), Confirm check-out on a staying booking, and the refund receipt on a refunded one.
- The startup check only confirmed the app runs for 40 seconds without errors; the window and login were not checked by hand.
- Browse search and availability were tested against the seed data, but the property dialog (`BookDialog`) wasn't clicked through: try one booking by hand, including a 7+ night stay (weekly discount line).
