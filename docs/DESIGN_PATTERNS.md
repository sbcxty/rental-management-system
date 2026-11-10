# Design patterns in Gudo's Nest

This page lists the design patterns that are actually in the code, with where to find them.
All paths start at the project root. Line numbers match the code as of the commit that added this file.

## Index

| Name | File path | Line numbers |
|---|---|---|
| Singleton | `src/main/java/com/gudosnest/backend/database/DatabaseConnection.java` | 7–26 |
| Singleton | `src/main/java/com/gudosnest/backend/database/SessionManager.java` | 5–25 |
| Factory Method | `src/main/java/com/gudosnest/frontend/portal/Portal.java` | 22–53, 91–97 |
| Factory Method | `src/main/java/com/gudosnest/frontend/owner/OwnerPortal.java` | 29–40 |
| Factory Method | `src/main/java/com/gudosnest/frontend/renter/RenterPortal.java` | 26–34 |
| Template Method | `src/main/java/com/gudosnest/frontend/portal/BasePage.java` | 26–40 |
| Builder | `src/main/java/com/gudosnest/backend/model/Booking.java` | 115–117, 292–434 |
| Builder | `src/main/java/com/gudosnest/backend/model/Property.java` | 55–57, 126–192 |
| Prototype | `src/main/java/com/gudosnest/backend/model/Item.java` | 74–79 |
| Prototype | `src/main/java/com/gudosnest/backend/model/Unit.java` | 29–35 |
| Decorator | `src/main/java/com/gudosnest/backend/service/pricing/Price.java` | 8–125 |
| Strategy | `src/main/java/com/gudosnest/backend/service/refund/RefundPolicy.java` | 9–93 |
| Facade | `src/main/java/com/gudosnest/backend/service/BookingService.java` | 19–25 (whole class) |
| Repository | `src/main/java/com/gudosnest/backend/repository/` (6 classes) | e.g. `BookingRepository.java` 16–46 |
| Partial: Observer-style listener | `src/main/java/com/gudosnest/frontend/components/Sidebar.java` | 59–66 |

---

## 1. Singleton

**What it is:** A Singleton is a class that only ever has one object. The constructor is private, so nobody can call `new`, and everyone gets the same object through a static `getInstance()` method.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/database/DatabaseConnection.java`, lines 7–26 (class, private constructor, `getInstance()`). Used in `src/main/java/com/gudosnest/backend/database/Db.java`, line 39.
- `src/main/java/com/gudosnest/backend/database/SessionManager.java`, lines 5–25. Used in `src/main/java/com/gudosnest/frontend/MainWindow.java`, line 46.


**Code snippet:** Look at the private constructor and the `if (instance == null)` check: the object is made only the first time.

`src/main/java/com/gudosnest/backend/database/DatabaseConnection.java`, lines 10–26:

```java
public final class DatabaseConnection {

    private static DatabaseConnection instance;

    private final AppConfig config;
    private Connection connection;

    private DatabaseConnection() {
        this.config = AppConfig.load();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
```

**Why it fits this project:** It's a desktop app with one user at a time, so one shared database connection and one "who is logged in" record is exactly what we need.

---

## 2. Factory Method

**What it is:** The parent class has an abstract method that creates an object, and each subclass decides which class to create. The shared code calls the method without knowing which exact class comes back.

**Where to find it:**
- `src/main/java/com/gudosnest/frontend/portal/Portal.java`, lines 22–53: `create(...)` picks the portal (lines 47–50) and `createPage(...)` is the factory method (line 53).
- `src/main/java/com/gudosnest/frontend/portal/Portal.java`, lines 91–97: `navigate(...)` calls `createPage(itemId)` (line 93).
- `src/main/java/com/gudosnest/frontend/owner/OwnerPortal.java`, lines 29–40: the owner's version of `createPage`.
- `src/main/java/com/gudosnest/frontend/renter/RenterPortal.java`, lines 26–34: the renter's version of `createPage`.
- `src/main/java/com/gudosnest/frontend/MainWindow.java`, line 52: `Portal.create(this, user, kind)`.


**Code snippet:** Look at the abstract `createPage` in `Portal`, then at how `OwnerPortal` fills it in with its own pages.

`src/main/java/com/gudosnest/frontend/portal/Portal.java`, lines 47–53:

```java
    /** Picks the portal class for the role the person is using. */
    public static Portal create(MainWindow window, User user, SessionManager.Portal kind) {
        return kind == SessionManager.Portal.OWNER ? new OwnerPortal(window, user) : new RenterPortal(window, user);
    }

    /** The factory method: builds the page for one sidebar item. */
    protected abstract BasePage createPage(String itemId);
```

`src/main/java/com/gudosnest/frontend/owner/OwnerPortal.java`, lines 29–40:

```java
    @Override
    protected BasePage createPage(String itemId) {
        return switch (itemId) {
            case "properties" -> new PropertiesPage(this);
            case "bookings" -> new BookingsPage(this);
            case "payments" -> new PaymentsPage(this);
            case "finances" -> new FinancesPage(this);
            case "messages" -> new MessagesPage(this);
            case "damage" -> new DamageReportsPage(this);
            default -> new DashboardPage(this);
        };
    }
```

**Why it fits this project:** Owners and renters see different pages, but the sidebar, navigation and back button work the same. The shared code stays in `Portal`, and each portal only says which pages it has.

---

## 3. Template Method

**What it is:** A parent class has a fixed method that runs steps in order, and leaves one step abstract for subclasses to fill in. The subclass can't change the order, only its own step.

**Where to find it:**
- `src/main/java/com/gudosnest/frontend/portal/BasePage.java`, lines 26–40: `build()` is abstract (line 27) and `reload()` is `final` (line 30) and calls `build()`.
- Every page fills in `build()`, for example `src/main/java/com/gudosnest/frontend/owner/NeedsAttentionPage.java`, line 50.


**Code snippet:** Look at `reload()`: clear, call the page's own `build()`, show an error if it fails, then repaint. Pages only write `build()`.

`src/main/java/com/gudosnest/frontend/portal/BasePage.java`, lines 26–40:

```java
    /** Builds the page content. Called by {@link #reload()}. */
    protected abstract void build();

    /** Clears the page and builds it again from fresh data. */
    public final void reload() {
        removeAll();
        try {
            build();
        } catch (DataException | RuleException e) {
            removeAll();
            add(Ui.emptyState(e.getMessage()), "grow");
        }
        revalidate();
        repaint();
    }
```

**Why it fits this project:** Every page needs the same "clear, rebuild from the database, show errors" steps. Writing them once means no page can forget the error handling.

---

## 4. Builder

**What it is:** A Builder is a helper object that collects the values for a big object one named call at a time, then makes the object with `build()`. `build()` can also check that the required values are there.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/model/Booking.java`, lines 292–434 (the `Builder` class), lines 115–117 (`builder()`), lines 426–433 (`build()`). Used in `src/main/java/com/gudosnest/backend/repository/BookingRepository.java`, lines 104–125.
- `src/main/java/com/gudosnest/backend/model/Property.java`, lines 126–192 (the `Builder` class), lines 55–57 (`builder()`), lines 187–191 (`build()`). Used in `src/main/java/com/gudosnest/backend/repository/PropertyRepository.java`, lines 65–74.


**Code snippet:** Look at how each value is set by name (`.name(...)`, `.location(...)`) instead of a constructor with ten arguments in a row.

`src/main/java/com/gudosnest/backend/repository/PropertyRepository.java`, lines 65–74:

```java
            result.add(Property.builder()
                    .id(r.id())
                    .owner(r.ownerId(), r.ownerName())
                    .name(r.name())
                    .location(r.location())
                    .type(r.type())
                    .acquisition(r.cost(), r.date())
                    .images(images.getOrDefault(r.id(), List.of()))
                    .units(units.getOrDefault(r.id(), List.of()))
                    .build());
```

And `build()` checks the required values before making the object:

`src/main/java/com/gudosnest/backend/model/Booking.java`, lines 426–433:

```java
        public Booking build() {
            Objects.requireNonNull(checkIn, "checkIn");
            Objects.requireNonNull(checkOut, "checkOut");
            if (!checkOut.isAfter(checkIn)) {
                throw new IllegalStateException("Check-out must be after check-in");
            }
            return new Booking(this);
        }
```

**Why it fits this project:** A booking row has more than thirty values and many are optional (refund, cancellation). With a builder you can't mix up two arguments of the same type by accident.

---

## 5. Prototype

**What it is:** Instead of building a new object from scratch, you copy an existing one (the "prototype") and change only what's different.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/model/Item.java`, lines 74–79: `copyTo(targetUnitId)`. Used in `src/main/java/com/gudosnest/backend/service/OwnerService.java`, line 188 ("Copy inventory from another unit").
- `src/main/java/com/gudosnest/backend/model/Unit.java`, lines 29–35: `copy(newName)`. Used in `src/main/java/com/gudosnest/frontend/owner/UnitDialog.java`, line 40 ("Duplicate unit").


**Code snippet:** Look at what changes in the copy: id becomes 0 (a new row), the unit is the target, and the condition resets to `GOOD`.

`src/main/java/com/gudosnest/backend/model/Item.java`, lines 74–79:

```java
    // PATTERN: Prototype
    // "Copy inventory from another unit" clones every item of the source unit into the target
    // unit: same name, type, quantity and price, condition reset to Good, no id (new rows).
    public Item copyTo(int targetUnitId) {
        return new Item(0, propertyId, targetUnitId, name, type, quantity, unitPrice, Condition.GOOD, null);
    }
```

**Why it fits this project:** Owners often have units with the same furniture. Copying items from one unit is faster and less error-prone than typing them all again.

---

## 6. Decorator

**What it is:** A Decorator wraps an object of the same type and adds something on top. You can stack wrappers like layers, and each one only knows about the layer inside it.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/service/pricing/Price.java`, lines 8–125: the `Price` interface, the base `NightlyRate` (lines 44–65), the `PriceDecorator` parent (lines 68–74), and the layers `CleaningFee` (lines 77–96) and `LongStayDiscount` (lines 99–125). `forStay(...)` (lines 33–39) stacks them.
- Used in `src/main/java/com/gudosnest/backend/service/BookingService.java`, line 100; `src/main/java/com/gudosnest/frontend/components/Receipt.java`, line 55; `src/main/java/com/gudosnest/frontend/shared/BookingUi.java`, line 89.


**Code snippet:** Look at `forStay` wrapping one price inside the next, and at `CleaningFee.total()` adding its fee to `inner.total()`.

`src/main/java/com/gudosnest/backend/service/pricing/Price.java`, lines 33–39:

```java
    static Price forStay(BigDecimal nightlyRate, long nights, BigDecimal cleaningFee) {
        Price price = new NightlyRate(nightlyRate, nights);
        if (cleaningFee.signum() > 0) {
            price = new CleaningFee(price, cleaningFee);
        }
        return new LongStayDiscount(price, nightlyRate, nights);
    }
```

`src/main/java/com/gudosnest/backend/service/pricing/Price.java`, lines 77–95:

```java
    final class CleaningFee extends PriceDecorator {
        private final BigDecimal fee;

        public CleaningFee(Price inner, BigDecimal fee) {
            super(inner);
            this.fee = fee;
        }

        @Override
        public BigDecimal total() {
            return inner.total().add(fee);
        }

        @Override
        public List<Line> lines() {
            List<Line> lines = inner.lines();
            lines.add(new Line("Cleaning fee", fee, false));
            return lines;
        }
```

**Why it fits this project:** A price is the nights plus extras minus discounts, and each part also needs its own line on the receipt. Adding a new fee later means adding one new layer, not editing the others.

---

## 7. Strategy

**What it is:** A Strategy is one of several interchangeable classes that do the same job in different ways. The code picks a strategy and then just calls its method, without `if` checks everywhere.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/service/refund/RefundPolicy.java`, lines 9–93: the `RefundPolicy` interface with `quote(...)` (line 44), the chooser `forCancellation(...)` (lines 47–56), and the strategies `FullRefund` (65–70), `PartialRefund` (72–78), `NoRefund` (80–85), `AutomaticRefund` (88–93).
- Used in `src/main/java/com/gudosnest/backend/service/BookingService.java`, lines 160, 168 and 275.


Note: line 275 (automatic full refund) runs today when an owner rejects or cancels a booking. Line 168 picks a tier when a renter cancels, but the renter's Cancellations page is still a placeholder, so no screen reaches it yet.

**Code snippet:** Look at how `forCancellation` returns a different class depending on the days left, and each class answers `quote(total)` its own way.

`src/main/java/com/gudosnest/backend/service/refund/RefundPolicy.java`, lines 47–56:

```java
    static RefundPolicy forCancellation(LocalDate today, LocalDate checkInDate) {
        long days = daysBefore(today, checkInDate);
        if (days >= 7) {
            return new FullRefund();
        }
        if (days >= 3) {
            return new PartialRefund();
        }
        return new NoRefund();
    }
```

`src/main/java/com/gudosnest/backend/service/refund/RefundPolicy.java`, lines 72–78:

```java
    final class PartialRefund implements RefundPolicy {
        @Override
        public Quote quote(BigDecimal total) {
            BigDecimal fee = total.multiply(CANCELLATION_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            return new Quote(PARTIAL, "Partial refund", fee, total.subtract(fee));
        }
    }
```

**Why it fits this project:** The refund rules have clear tiers (7+ days, 3–6 days, under 3 days). Each tier is one small class, so the rules are in one place and easy to test.

---

## 8. Facade

**What it is:** A Facade is one simple class that hides a complicated set of steps behind it. The screens call one method, and the facade does all the work across many other classes.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/service/BookingService.java`, lines 19–25 (the comment and class). For example `reject(...)` at lines 207–216 and `approve(...)` at lines 186–205.
- The screen calls it in `src/main/java/com/gudosnest/frontend/owner/BookingReviewDialog.java`, lines 80, 88, 106 and 121.


**Code snippet:** Look at how one call to `reject` changes the booking status and makes a refund inside one database transaction. The dialog only calls `portal.bookings.reject(...)`.

`src/main/java/com/gudosnest/backend/service/BookingService.java`, lines 207–216:

```java
    /** Rejects a pending booking; the renter gets everything back automatically. */
    public void reject(int bookingId, int ownerId) {
        Booking b = ownedPending(bookingId, ownerId);
        LocalDateTime now = now();
        Db.transaction(() -> {
            bookings.setStatus(bookingId, BookingStatus.REJECTED);
            automaticRefund(b, now);
            return null;
        });
    }
```

**Why it fits this project:** Approving or rejecting a booking touches bookings, payments, refunds and the money ledger together. The facade makes sure all steps happen (or none do) and keeps SQL out of the screens.

---

## 9. Repository

**What it is:** A Repository is a class that hides the database. Other code asks it for objects ("all bookings for this owner") and never writes SQL itself.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/repository/`: `BookingRepository.java`, `MessageRepository.java`, `MoneyRepository.java`, `OwnerOpsRepository.java`, `PropertyRepository.java`, `UserRepository.java`.
- Example: `src/main/java/com/gudosnest/backend/repository/BookingRepository.java`, lines 16–46. Services create them, for example `src/main/java/com/gudosnest/backend/service/BookingService.java`, lines 31–32.


**Code snippet:** Look at the method names: callers get a `List<Booking>` back and never see the SQL.

`src/main/java/com/gudosnest/backend/repository/BookingRepository.java`, lines 36–46:

```java
    public List<Booking> forOwner(int ownerId) {
        return Db.list(SELECT + " WHERE p.owner_id = ? ORDER BY b.booking_id", BookingRepository::map, ownerId);
    }

    public List<Booking> forRenter(int renterId) {
        return Db.list(SELECT + " WHERE b.renter_id = ? ORDER BY b.booking_id", BookingRepository::map, renterId);
    }

    public Optional<Booking> byId(int bookingId) {
        return Db.one(SELECT + " WHERE b.booking_id = ?", BookingRepository::map, bookingId);
    }
```

**Why it fits this project:** The SQL with all its joins lives in one place, so if a table changes, only the repository needs fixing.

---

## Partial or unclear

### Observer-style listener (Sidebar)

**What it is:** In the Observer pattern, an object keeps a list of listeners and tells all of them when something happens.

**Where to find it:** `src/main/java/com/gudosnest/frontend/components/Sidebar.java`, lines 59–66 (the `Listener` interface), and `src/main/java/com/gudosnest/frontend/portal/Portal.java`, line 67 (the portal passes in its listener).


**Code snippet:** Look at the three events the sidebar reports to its portal.

`src/main/java/com/gudosnest/frontend/components/Sidebar.java`, lines 58–66:

```java
    /** What the sidebar asks its portal to do. */
    public interface Listener {
        void onNavigate(String itemId);

        void onSwitchPortal();

        void onLogOut();
    }

```

**Why it's here and not above:** The sidebar has exactly one listener (its portal) passed in the constructor. There's no list of listeners and no way to add more, so it's really a simple callback, not a full Observer.

### Not counted

- `src/main/java/com/gudosnest/frontend/components/Ui.java` has static helper methods that make labels and panels (its comment calls them "factory methods"). These are plain helper methods, not the Factory Method pattern above.
