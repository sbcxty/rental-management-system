# Data structures and algorithms in Gudo's Nest

This page lists the data structures and algorithms (DSA) that are actually used in the code.
All paths start at the project root. Line numbers match the code as of the commit that added this file.
In the time complexities, **n** means the number of items in the structure unless the entry says otherwise.

## Index

| Name | File path | Line numbers |
|---|---|---|
| Priority queue (binary heap) | `src/main/java/com/gudosnest/backend/structures/AttentionQueue.java` | 9–43 |
| TreeMap for overlapping dates | `src/main/java/com/gudosnest/backend/structures/AvailabilityCalendar.java` | 9–53 |
| TreeMap for months in order | `src/main/java/com/gudosnest/backend/service/FinanceService.java` | 89–111 |
| Tree with recursion | `src/main/java/com/gudosnest/backend/structures/InventoryTree.java` | 9–73 |
| Binary search | `src/main/java/com/gudosnest/backend/structures/SortedSearch.java` | 6–34 |
| Stack (back navigation) | `src/main/java/com/gudosnest/frontend/components/PortalShell.java` | 22–61 |
| HashMap cache | `src/main/java/com/gudosnest/frontend/components/ImageUtil.java` | 31–62 |
| Partial: Trie | `src/main/java/com/gudosnest/backend/structures/Trie.java` | 8–55 |
| Partial: Circular doubly linked list | `src/main/java/com/gudosnest/backend/structures/ImageRing.java` | 5–92 |

---

## 1. Priority queue (binary heap)

**What it is:** A priority queue always gives you the "smallest" item first, no matter what order items went in. Java's `PriorityQueue` keeps the items in a binary heap, a tree stored in an array where every parent is smaller than its children.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/structures/AttentionQueue.java`, lines 9–43. The heap is made at line 21, ordered by `dueAt`.
- Filled and read in `src/main/java/com/gudosnest/frontend/owner/Attention.java`, lines 39–56 (`add` at lines 45 and 52, `inOrder()` at line 56).


**Code snippet:** Look at the comparator (earliest `dueAt` first) and at `inOrder()`, which polls a copy so the original queue isn't emptied.

`src/main/java/com/gudosnest/backend/structures/AttentionQueue.java`, lines 21–25:

```java
    private final PriorityQueue<Entry<T>> heap = new PriorityQueue<>(Comparator.comparing(Entry::dueAt));

    public void add(LocalDateTime dueAt, T value) {
        heap.add(new Entry<>(dueAt, value));
    }
```

`src/main/java/com/gudosnest/backend/structures/AttentionQueue.java`, lines 35–43:

```java
    /** All items, most urgent first. Uses a copy, so the queue itself is left as it was. */
    public List<T> inOrder() {
        PriorityQueue<Entry<T>> copy = new PriorityQueue<>(heap);
        List<T> result = new ArrayList<>(copy.size());
        while (!copy.isEmpty()) {
            result.add(copy.poll().value());
        }
        return result;
    }
```

**What it's used for in the app:** The owner's "Needs attention" list puts the most urgent item on top: pending bookings by their 48-hour deadline and refund requests by how long the renter has waited.

**Time complexity:**
- `add`: O(log n), because the new item moves up at most the height of the heap.
- `inOrder()`: O(n log n), because it copies the heap and then polls n times at O(log n) each.

---

## 2. TreeMap for overlapping dates

**What it is:** A `TreeMap` is a map that keeps its keys sorted (inside it's a red-black tree, a self-balancing binary search tree). Because the keys are sorted, it can quickly find "the closest key before this one" (`floorEntry`) or "all keys between these two" (`subMap`).

**Where to find it:**
- `src/main/java/com/gudosnest/backend/structures/AvailabilityCalendar.java`, lines 9–53. The map is at line 25, `overlapping(...)` at lines 32–49.
- Built in `src/main/java/com/gudosnest/backend/service/BookingService.java`, lines 62–69 (`calendarFor`), and used for checks at lines 72, 111 and 190.


**Code snippet:** Look at `floorEntry(start)` (the last stay that began before the new one) and `subMap(start, end)` (the stays that begin during the new one).

`src/main/java/com/gudosnest/backend/structures/AvailabilityCalendar.java`, lines 32–49:

```java
    public List<T> overlapping(LocalDateTime start, LocalDateTime end) {
        List<T> result = new ArrayList<>();
        Map.Entry<LocalDateTime, List<Stay<T>>> before = byStart.floorEntry(start);
        if (before != null) {
            for (Stay<T> stay : before.getValue()) {
                if (stay.end().isAfter(start)) {
                    result.add(stay.value());
                }
            }
        }
        // Stays that start strictly inside the new stay (the floor key itself was handled above)
        for (List<Stay<T>> stays : byStart.subMap(start, false, end, false).values()) {
            for (Stay<T> stay : stays) {
                result.add(stay.value());
            }
        }
        return result;
    }
```

**What it's used for in the app:** Before a booking is approved, the app checks that the unit's dates are still free, so two approved stays never overlap. Line 190 runs when an owner presses Approve; lines 72 and 111 are for the renter booking flow, whose screen isn't built yet.

**Time complexity:** O(log n + k) per check, where k is the number of overlapping stays found: `floorEntry` and `subMap` each take O(log n) to find their place in the tree. (Building the calendar first takes O(n log n) for n approved stays.)

---

## 3. TreeMap for months in order

**What it is:** The same sorted map as above, used here so the months always come out in calendar order.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/service/FinanceService.java`, lines 89–111 (`monthly(...)`), map made at line 95.
- Used by the dashboard chart in `src/main/java/com/gudosnest/frontend/owner/DashboardPage.java`, line 133.


**Code snippet:** Look at how every month gets a zero slot first, then each ledger line adds to its month. Reading `entrySet()` gives the months in order.

`src/main/java/com/gudosnest/backend/service/FinanceService.java`, lines 94–111:

```java
    public List<Month> monthly(List<Transaction> ledger, YearMonth last, int count) {
        TreeMap<YearMonth, BigDecimal[]> months = new TreeMap<>();
        YearMonth first = last.minusMonths(count - 1L);
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            months.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Transaction t : ledger) {
            BigDecimal[] slot = months.get(YearMonth.from(t.date()));
            if (slot == null) {
                continue;
            }
            slot[0] = slot[0].add(incomeEffect(t));
            if (t.isExpense()) {
                slot[1] = slot[1].add(t.amount());
            }
        }
        return months.entrySet().stream().map(e -> new Month(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }
```

**What it's used for in the app:** The "Income per month" bar chart on the owner dashboard, with empty months still shown as zero.

**Time complexity:** O(t log m), where t is the number of ledger lines and m the number of months: each line does one `get` on a tree of m months.

---

## 4. Tree with recursion

**What it is:** A tree is a structure where each node can have child nodes. Recursion means a method calls itself: to total a node, it adds its own items and then asks each child for its total.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/structures/InventoryTree.java`, lines 9–73: `totalValue()` (lines 46–55), `itemCount()` (lines 58–64), `needsAttentionCount()` (lines 67–73).
- Built in `src/main/java/com/gudosnest/backend/service/OwnerService.java`, lines 140–150 (property → units → items).
- Used in `src/main/java/com/gudosnest/frontend/owner/UnitsPage.java`, lines 86, 256 and 257, and `src/main/java/com/gudosnest/frontend/owner/InventoryPage.java`, line 67.


**Code snippet:** Look at the line marked `// recursive step`: the method calls itself on each child.

`src/main/java/com/gudosnest/backend/structures/InventoryTree.java`, lines 45–55:

```java
    /** Value of everything in this node and below: quantity × unit price, summed recursively. */
    public BigDecimal totalValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : items) {
            total = total.add(item.getTotalValue());
        }
        for (InventoryTree child : children) {
            total = total.add(child.totalValue());   // recursive step
        }
        return total;
    }
```

**What it's used for in the app:** On the Units page, the "Items needing attention" stat and each unit card's item count. The same code works for one unit or a whole property.

**Time complexity:** O(n), where n is the number of nodes plus items: the recursion visits each node and each item once.

Note: `totalValue()` is written and works the same way, but no screen calls it yet.

---

## 5. Binary search

**What it is:** Binary search finds a value in a **sorted** list by checking the middle, then throwing away the half that can't contain it, and repeating. Each step halves what's left.

**Where to find it:**
- `src/main/java/com/gudosnest/backend/structures/SortedSearch.java`, lines 6–34 (`binarySearch` at lines 19–34).
- Used in `src/main/java/com/gudosnest/frontend/owner/BookingsPage.java`, line 130. The list is sorted by id first at lines 49–52.


**Code snippet:** Look at `low`, `high` and `mid`: if the middle value is too small, search the right half (`low = mid + 1`), otherwise the left half.

`src/main/java/com/gudosnest/backend/structures/SortedSearch.java`, lines 19–34:

```java
    public static <T> int binarySearch(List<T> sorted, int target, ToIntFunction<T> key) {
        int low = 0;
        int high = sorted.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;      // unsigned shift avoids int overflow on huge lists
            int value = key.applyAsInt(sorted.get(mid));
            if (value == target) {
                return mid;
            } else if (value < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }
```

**What it's used for in the app:** On the owner's Bookings page, typing a reference like "GN-2026-8815" finds that booking by its id.

**Time complexity:** O(log n), because each step halves the part of the list still being searched. (Sorting the list when the page loads is O(n log n), done once per load, not once per search.)

---

## 6. Stack (back navigation)

**What it is:** A stack is "last in, first out", like a pile of plates: you only add or take from the top. Here it's an `ArrayDeque` used with `push` and `pop`.

**Where to find it:**
- `src/main/java/com/gudosnest/frontend/components/PortalShell.java`, lines 22–61: the stack at line 26, `push` at lines 46–51, `back` (pop) at lines 54–61, and `history.clear()` at line 40.
- Called from `src/main/java/com/gudosnest/frontend/portal/Portal.java`, lines 106–113 (`open` and `back`). Pages use it, for example `src/main/java/com/gudosnest/frontend/owner/PropertiesPage.java`, line 99 (open a property's units) and `src/main/java/com/gudosnest/frontend/owner/InventoryPage.java`, line 81 (Back link).


**Code snippet:** Look at `history.push(current)` when going deeper and `history.pop()` when pressing Back.

`src/main/java/com/gudosnest/frontend/components/PortalShell.java`, lines 45–61:

```java
    /** Opens a page on top of the current one; {@link #back()} returns to the current one. */
    public void push(BasePage page) {
        if (current != null) {
            history.push(current);
        }
        display(page);
    }

    /** Returns to the previous page and reloads it so it shows any changes made meanwhile. */
    public void back() {
        if (history.isEmpty()) {
            return;
        }
        BasePage previous = history.pop();
        previous.reload();
        display(previous);
    }
```

**What it's used for in the app:** Drilling down Properties → Units → Inventory, then pressing "Back" returns to exactly the page you came from, in reverse order.

**Time complexity:** O(1) for push and pop, because `ArrayDeque` only touches the top end.

---

## 7. HashMap cache

**What it is:** A `HashMap` stores key → value pairs and finds a key in about one step by turning it into a number (its hash). A cache keeps results you already worked out so you don't do the slow work again.

**Where to find it:**
- `src/main/java/com/gudosnest/frontend/components/ImageUtil.java`, lines 31–62: the map at line 32, the lookup at lines 39–41, and storing at line 60.
- Used by `src/main/java/com/gudosnest/frontend/components/Widgets.java`, line 54 (property photos on cards).


**Code snippet:** Look at the `containsKey` check: if the photo was loaded before, it's returned at once without reading the disk.

`src/main/java/com/gudosnest/frontend/components/ImageUtil.java`, lines 31–41:

```java
    // DSA: HashMap cache - each photo is read from disk once, then found by its path in O(1).
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Loads an image, or returns null if the file does not exist or cannot be read. */
    public static BufferedImage load(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }
```

**What it's used for in the app:** Property photos appear on many cards and pages. Each photo file is read from disk only once.

**Time complexity:** O(1) on average per lookup, because the hash points straight to the right bucket. (The first load of a photo still costs the disk read.)

---

## Partial or unclear

These are written and complete as classes, but **no screen uses them yet**, because the pages that need them are still placeholders.

### Trie (prefix tree)

**What it is:** A trie stores words letter by letter in a tree, so all words that start with the same letters share the same path. To find words starting with "ta", you walk two letters down and collect everything below.

**Where to find it:** `src/main/java/com/gudosnest/backend/structures/Trie.java`, lines 8–55 (`insert` at lines 23–29, `startingWith` at lines 32–43, recursive `collect` at lines 45–55). Nothing calls it yet: it's meant for the renter Browse page (`src/main/java/com/gudosnest/frontend/renter/BrowsePage.java`), which is a placeholder.


**Code snippet:** Look at the loop that walks one child per typed letter, then `collect` gathers every word under that node.

`src/main/java/com/gudosnest/backend/structures/Trie.java`, lines 32–43:

```java
    public List<String> startingWith(String prefix, int limit) {
        Node node = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            node = node.children.get(c);
            if (node == null) {
                return List.of();
            }
        }
        List<String> result = new ArrayList<>();
        collect(node, result, limit);
        return result;
    }
```

**What it's for (planned):** Location suggestions under the Browse search box.

**Time complexity:** O(p + r) for `startingWith`, where p is the length of the typed prefix and r the number of nodes visited while collecting: walking down takes one step per letter.

### Circular doubly linked list (ImageRing)

**What it is:** A linked list where each node points to both the next and previous node, and the last node points back to the first. "Next" on the last photo wraps around to the first with no index math.

**Where to find it:** `src/main/java/com/gudosnest/backend/structures/ImageRing.java`, lines 5–92 (`add` at lines 33–49, `next` at lines 55–61, `previous` at lines 63–69). It's used by `src/main/java/com/gudosnest/frontend/components/Carousel.java` (lines 38 and 52), but no page creates a `Carousel` yet: it's meant for the renter property detail page, which isn't built.


**Code snippet:** Look at how a new node is linked between the old tail and the head, which keeps the ring closed.

`src/main/java/com/gudosnest/backend/structures/ImageRing.java`, lines 33–49:

```java
    public void add(T value) {
        Node<T> node = new Node<>(value);
        if (head == null) {
            node.next = node;
            node.prev = node;
            head = node;
            current = node;
            position = 1;
        } else {
            Node<T> tail = head.prev;
            tail.next = node;
            node.prev = tail;
            node.next = head;
            head.prev = node;
        }
        size++;
    }
```

**What it's for (planned):** The photo carousel's next and previous buttons on the property detail page.

**Time complexity:** O(1) for `add`, `next` and `previous` (just pointer changes). `moveTo` (lines 72–80) is O(n) because it walks around the ring.

