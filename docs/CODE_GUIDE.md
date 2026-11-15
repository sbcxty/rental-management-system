# Gudo's Nest – code guide

A short map of the code for review. Rules are in `BUSINESS_RULES.md`, patterns in `DESIGN_PATTERNS.md`,
data structures in `DSA.md`.

## Run it

1. Run `db/schema.sql`, then `db/seed.sql`, in MySQL (this resets the `gudos_nest` database).
2. Copy `config.properties.example` to `config.properties` and set your MySQL user and password.
3. `.\mvnw.cmd -q compile exec:java`
4. Demo accounts (password `password123!`): `elena.vance@example.com` (owner + renter),
   `marco.delacruz@example.com` (owner), `miguel.alvarez@example.com` (renter).

Database check only:
`.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=com.gudosnest.backend.database.DbConnectionTest"`

## Layers

A screen never talks to the database. Each layer only calls the one below it.

```
frontend (Swing screens)  →  service (rules)  →  repository (SQL)  →  Db / DatabaseConnection  →  MySQL
```

| Package (`com.gudosnest...`) | What is in it |
|---|---|
| `Main` | Installs the theme, checks the database, opens the window |
| `backend.database` | `DatabaseConnection` (Singleton), `Db` (JDBC helper + transactions), `SessionManager`, `AppConfig` |
| `backend.model` | Data classes: `User`, `Property`, `Unit`, `Booking`, `Item`, and small records in `Records` |
| `backend.repository` | All SQL, one class per area |
| `backend.service` | All business rules. Throws `RuleException` with a message the screen shows as is |
| `backend.structures` | Hand-written data structures (see `DSA.md`) |
| `frontend.theme` | `Colors`, `Fonts`, `Icons`, `Theme` (spacing and FlatLaf settings): the only place style is defined |
| `frontend.components` | Reusable parts: `AppButton`, `AppDialog`, `AppTable`, `Card`, `PageHeader`, `Pager`, `Sidebar`, `Ui` |
| `frontend.portal` | `Portal` (Factory Method) and `BasePage` (Template Method), shared by both portals |
| `frontend.owner`, `frontend.renter` | The pages and dialogs of each portal |
| `frontend.shared` | Used by both portals: `MessagesPage`, `BookingUi`, `DamageUi` |

## What happens when a renter books

1. `BookDialog` calls `BookingService.quote(...)` to show the price (`Price`, Decorator).
2. Reserve calls `BookingService.book(...)`, which checks: no overdue damage report, valid dates
   (tomorrow or later, 1–30 nights), guests fit, dates free (`AvailabilityCalendar`), no duplicate request.
3. In **one transaction** it inserts the booking (pending, expires in 48 hours) and its paid payment.
4. The owner approves (`BookingService.approve`): status approved, income recorded, overlapping
   requests rejected and refunded, again in one transaction.
5. `BookingService.refreshStatuses()` runs when the app opens, when a page opens and every 3 minutes:
   expired requests are refunded, ended stays are completed, unit occupancy is updated.

## How data stays correct

| Protection | Where |
|---|---|
| Transactions: several changes are saved together or not at all | `Db.transaction`, used by every multi-table action in the services |
| Rules are checked in the service, not the screen | `BookingService`, `OwnerService`, `DamageService`, `FinanceService`, `AuthService` |
| Ownership checks: you can only change your own bookings and properties | e.g. `BookingService.ownedPending`, `OwnerService.property` |
| Database constraints: foreign keys, `CHECK`, `UNIQUE`, `ENUM` | `db/schema.sql`: check-out after check-in, one account per email, one refund per payment, one rental payment per booking, no two units with the same name in a property, messages and damage descriptions 1 to 1,000 characters |
| Parameterised SQL (`?`) for everything a person types | `Db.prepare` |
| Passwords stored only as BCrypt hashes | `AuthService` |
| Price snapshot: a booking keeps the rate it was made with | `Bookings.nightly_rate`, `cleaning_fee`, `discount_amount`, `total_amount` |
| Money is `BigDecimal` / `DECIMAL`, never floating point | models and schema |

## UI consistency

- Page padding comes from `BasePage` (32 top, 40 sides). Sections are 24 apart; tabs, table and pager 16.
- Spacing uses 4 / 8 / 12 / 16 / 24. Controls are 40 high (`Theme.CONTROL_HEIGHT`).
- One accent colour (`Colors.PRIMARY`); red only for destructive actions and errors.
- Every input (text, dropdown, date) is white with the same border and corner radius (`Theme`).

## Try by hand before the defense

- Book a 7+ night stay as Miguel (weekly discount line), approve it as Elena.
- Cancel an upcoming stay from Booking history (opens Cancellations with that stay selected).
- Approve a refund from Payments or Needs attention; check Finances shows the refund.
- File a damage report as Elena, pay it as the renter.
