# Data structures and algorithms in Gudo's Nest

To find one in the code, search for `// DSA:`. The hand-written structures are in
`src/main/java/com/gudosnest/backend/structures/`. `n` = number of items stored.

| # | Structure / algorithm | Class | Used for | Speed |
|---|---|---|---|---|
| 1 | Priority queue (heap) | `AttentionQueue` | Owner's "Needs attention": most urgent first | add O(log n) |
| 2 | TreeMap (sorted map) | `AvailabilityCalendar` | Is a unit free on these dates? | O(log n) |
| 3 | TreeMap by month | `FinanceService.monthly` | Dashboard chart, months in order | O(log n) per line |
| 4 | Tree + recursion | `InventoryTree` | Inventory totals: property → units → items | O(n) |
| 5 | Binary search | `SortedSearch` | Find a booking by its reference number | O(log n) |
| 6 | Stack | `PortalShell.history` | The Back link | O(1) |
| 7 | HashMap | `ImageUtil` cache, `BrowseService.Catalog` | Photos loaded once; one calendar per unit | O(1) |
| 8 | Trie (prefix tree) | `Trie` | Location suggestions while typing | O(length typed) |
| 9 | Circular doubly linked list | `ImageRing` | Photo carousel: next / previous wrap around | O(1) |

## 1. Priority queue (`AttentionQueue`)

A heap always keeps the smallest item at the top. Here "smallest" = earliest deadline.

```java
private final PriorityQueue<Entry<T>> heap = new PriorityQueue<>(Comparator.comparing(Entry::dueAt));
public void add(LocalDateTime dueAt, T value) { heap.add(new Entry<>(dueAt, value)); }   // O(log n)
```

**Why:** the list mixes pending bookings (due when their 48 hours end) and refund requests (due by how long
the renter has waited). The heap gives them back most urgent first without sorting the whole list each
time. Used by `frontend/owner/Attention.java`.

## 2. TreeMap for overlapping dates (`AvailabilityCalendar`)

A TreeMap keeps its keys sorted. Each unit's approved stays are stored by check-in time.

```java
Map.Entry<...> before = byStart.floorEntry(start);     // last stay starting at or before the new check-in
// overlaps if that stay ends after the new check-in
byStart.subMap(start, false, end, false)               // stays that start during the new stay: all overlap
```

**Why:** to check new dates we only look at the neighbours of the new stay, not at every booking:
O(log n) instead of O(n). It works because approved stays of one unit never overlap each other.
Used by `BookingService.book`, `approve` and Browse.

## 3. TreeMap for months (`FinanceService.monthly`)

```java
TreeMap<YearMonth, BigDecimal[]> months = new TreeMap<>();    // month -> {income, expenses}
for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) months.put(m, zeros);
for (Transaction t : ledger) months.get(YearMonth.from(t.date()))...add(t.amount());
```

**Why:** the keys stay in calendar order whatever order the ledger lines arrive in, and months with no
activity still appear as zero. This feeds the Dashboard bar chart.

## 4. Tree with recursion (`InventoryTree`)

A property node has unit nodes as children; each node holds its items.

```java
public BigDecimal totalValue() {
    BigDecimal total = BigDecimal.ZERO;
    for (Item item : items) total = total.add(item.getTotalValue());           // this node
    for (InventoryTree child : children) total = total.add(child.totalValue()); // recursive step
    return total;
}
```

**Why:** the same method gives the value of one unit or of the whole property. The base case is a node
with no children. Built in `OwnerService.inventory`, shown on the Inventory page.

## 5. Binary search (`SortedSearch`)

Bookings are loaded sorted by id (`ORDER BY b.booking_id`). Typing `GN-2026-8815` searches for id 8815.

```java
while (low <= high) {
    int mid = (low + high) >>> 1;
    int value = key.applyAsInt(sorted.get(mid));
    if (value == target) return mid;
    else if (value < target) low = mid + 1;       // throw away the lower half
    else high = mid - 1;                          // throw away the upper half
}
```

**Why:** each step halves the list: 1,000 bookings need about 10 steps, not 1,000. The list must be
sorted. `equalRange` runs two binary searches to find every row with the same key (all damage reports of
one booking). Used by owner Bookings, renter Booking history and Damage reports.

## 6. Stack (`PortalShell`)

```java
private final Deque<BasePage> history = new ArrayDeque<>();
history.push(current);      // opening Units from Properties
history.pop();              // Back: returns the page we came from
```

**Why:** Back must return pages in reverse order (last in, first out): Inventory → Units → Properties.
Choosing a sidebar item clears the stack.

## 7. HashMap

A HashMap finds a value by its key in about one step.

- `frontend/components/ImageUtil.java`: `path → image` cache, so each photo is read from disk once.
- `BrowseService.catalog`: `unit id → AvailabilityCalendar`. One query loads every approved stay; each
  search is then a map lookup plus a calendar lookup, with no database query per unit.

## 8. Trie (`Trie`)

Words are stored letter by letter; words with the same beginning share the same path.

```java
for (char c : prefix.toLowerCase().toCharArray()) {
    node = node.children.get(c);          // walk down one letter at a time
    if (node == null) return List.of();   // nothing starts with this
}
collect(node, result, limit);             // every word below this node (recursion)
```

**Why:** typing "ta" walks two letters and lists Tagaytay, ... The time depends on how many letters were
typed, not on how many places exist. Children are in a TreeMap, so suggestions are alphabetical.
Built in `BrowseService.catalog`, used by the Location box on Browse.

## 9. Circular doubly linked list (`ImageRing`)

Each node knows its `next` and `prev`; the last node's `next` is the first node.

```java
public T next()     { current = current.next; ... }    // from the last photo this lands on the first
public T previous() { current = current.prev; ... }
```

**Why:** the carousel never checks "am I at the end?"; next and previous are always one step, O(1).
Used by `frontend/components/Carousel.java`.
