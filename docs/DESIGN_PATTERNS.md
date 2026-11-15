# Design patterns in Gudo's Nest

Nine patterns. To find one in the code, search for `// PATTERN:` (each class is marked).
All paths are under `src/main/java/com/gudosnest/`.

| # | Pattern | Class | One-line reason |
|---|---|---|---|
| 1 | Singleton | `DatabaseConnection`, `SessionManager` | Only one connection and one logged-in session exist |
| 2 | Factory Method | `Portal` → `OwnerPortal`, `RenterPortal` | The app opens a portal without knowing which kind |
| 3 | Template Method | `BasePage.reload()` | Every page loads the same way; only `build()` differs |
| 4 | Builder | `Booking.Builder`, `Property.Builder` | Objects with many optional values, set by name |
| 5 | Prototype | `Unit.copy()`, `Item.copyTo()` | "Duplicate unit" and "Copy inventory" clone an existing object |
| 6 | Decorator | `Price` | A price is built in layers: nights + cleaning fee − discount |
| 7 | Strategy | `RefundPolicy`, `BrowseService.Category`, `BookingHistoryPage.Tab` | Swappable rules behind one method |
| 8 | Facade | `BookingService` | One method call hides changes to five tables |
| 9 | Repository | `backend/repository/*` | All SQL lives in one layer |

## 1. Singleton

**Idea:** a class that can only ever have one object; everyone asks the class for it.

**Where:** `backend/database/DatabaseConnection.java`, `backend/database/SessionManager.java`.

```java
private static DatabaseConnection instance;
private DatabaseConnection() { ... }                    // nobody else can call new
public static synchronized DatabaseConnection getInstance() {
    if (instance == null) instance = new DatabaseConnection();
    return instance;
}
```

**Why here:** a desktop app needs one database connection, not one per screen. `SessionManager` holds the
logged-in user and portal: `MainWindow` writes it at log in, portal switch and log out, and
`Portal.create()` reads it to open the right portal.

## 2. Factory Method

**Idea:** a parent class declares "make me an object" and subclasses decide which class to make.

**Where:** `frontend/portal/Portal.java` (`create`, `createPage`), `frontend/owner/OwnerPortal.java`,
`frontend/renter/RenterPortal.java`.

```java
protected abstract BasePage createPage(String itemId);   // Portal

protected BasePage createPage(String itemId) {            // OwnerPortal
    return switch (itemId) {
        case "properties" -> new PropertiesPage(this);
        case "bookings"   -> new BookingsPage(this);
        ...
```

**Why here:** the shared code (sidebar, navigation, back button, unread dots) is written once in `Portal`.
It calls `createPage("messages")` and each portal returns its own page. A new portal is a new subclass.

## 3. Template Method

**Idea:** the parent fixes the order of the steps; subclasses fill in one step.

**Where:** `frontend/portal/BasePage.java`.

```java
public final void reload() {          // final: pages can't change the steps
    removeAll();
    try { build(); }                  // the one step each page writes
    catch (DataException | RuleException e) { add(Ui.emptyState(e.getMessage())); }
    revalidate(); repaint();
}
protected abstract void build();
```

**Why here:** every page clears itself, loads from the database and shows errors the same way, so no page
can forget the error handling.

## 4. Builder

**Idea:** build a big object step by step with named methods, then call `build()`.

**Where:** `backend/model/Booking.java` (35 values), `backend/model/Property.java`. Used in
`BookingRepository.map`.

```java
Booking.builder()
    .id(rs.getInt("booking_id"))
    .stay(checkIn, checkOut, guests)
    .price(rate, cleaningFee, discount, total)
    .build();                         // checks check-out is after check-in
```

**Why here:** a constructor with 35 positional arguments is easy to get wrong, and many values are optional
(cancellation, refund). The built object can't be changed afterwards.

## 5. Prototype

**Idea:** make a new object by copying an existing one.

**Where:** `backend/model/Unit.java` (`copy`), `backend/model/Item.java` (`copyTo`). Used by "Duplicate
unit" (`UnitsPage`) and "Copy inventory" (`OwnerService.copyInventory`).

```java
public Unit copy(String newName) {
    return new Unit(0, propertyId, newName, description, capacity, dailyRate, cleaningFee, false);
}
```

**Why here:** the copy keeps the rates and capacity but gets a new name, no id (not saved yet) and is not
occupied. The object itself knows how to copy itself correctly.

## 6. Decorator

**Idea:** wrap an object in another object with the same interface to add behaviour.

**Where:** `backend/service/pricing/Price.java`.

```java
Price price = new NightlyRate(rate, nights);          // base
price = new CleaningFee(price, fee);                  // wraps it: total + fee
price = new LongStayDiscount(price, rate, nights);    // wraps again: 10% off for 7+ nights
price.total();   price.lines();                       // every layer adds its part
```

**Why here:** each charge is its own small class that adds one line to the receipt and adjusts the total.
A new fee or discount is a new decorator; the existing classes don't change.

## 7. Strategy

**Idea:** several interchangeable rules with the same method; the caller picks one and just calls it.

**Where:**
- `backend/service/refund/RefundPolicy.java`: `FullRefund`, `PartialRefund`, `NoRefund`, `AutomaticRefund`.
- `backend/service/BrowseService.java` (`Category`): each Browse pill is a filter with `matches(property)`.
- `frontend/renter/BookingHistoryPage.java` (`Tab`): each tab decides which bookings it shows.

```java
static RefundPolicy forCancellation(LocalDate today, LocalDate checkIn) {
    long days = daysBefore(today, checkIn);
    if (days >= 7) return new FullRefund();
    if (days >= 3) return new PartialRefund();        // minus the 20% fee
    return new NoRefund();
}
// caller:  RefundPolicy.forCancellation(today, checkIn).quote(total)
```

**Why here:** the refund tiers live in exactly one place, so the Cancellations page, the booking dialog
and `BookingService` can never disagree.

## 8. Facade

**Idea:** one simple class in front of a complicated group of classes.

**Where:** `backend/service/BookingService.java`.

```java
// The screen calls one method...
bookings.approve(bookingId, ownerId);
// ...and the facade does all of this in one transaction:
//   Bookings: status = approved            FinancialTransactions: + rental income
//   Bookings: overlapping requests = rejected          Refunds: full refund for each
```

**Why here:** booking, approving, cancelling and checking out each touch several tables. The UI never needs
to know which tables or in what order, and all the steps succeed or fail together.

## 9. Repository

**Idea:** one layer owns all database access; the rest of the app works with objects.

**Where:** `backend/repository/` (`BookingRepository`, `PropertyRepository`, `MoneyRepository`,
`OwnerOpsRepository`, `MessageRepository`, `UserRepository`), on top of the small JDBC helper
`backend/database/Db.java`.

```java
public List<Booking> forOwner(int ownerId) {
    return Db.list(SELECT + " WHERE p.owner_id = ? ORDER BY b.booking_id", BookingRepository::map, ownerId);
}
```

**Why here:** services contain rules, repositories contain SQL. Values typed by a person always go in as
`?` parameters (PreparedStatement), so they can never change the SQL (no SQL injection).

## Not counted as patterns

- `Sidebar.Listener` is a single callback (the portal), not a full Observer with a list of listeners.
- `Ui` has helper methods that create labels and fields; these are plain helpers, not Factory Method.
