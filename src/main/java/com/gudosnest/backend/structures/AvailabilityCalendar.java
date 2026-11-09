package com.gudosnest.backend.structures;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// DSA: TreeMap
// One unit's bookings keyed by check-in time. A TreeMap keeps the keys sorted (red-black tree),
// so finding bookings that overlap a new stay does not need to scan every booking:
//   - floorEntry(start) is the last stay that began at or before the new check-in; it overlaps
//     if it ends after the new check-in.
//   - subMap(start, end) are the stays that begin during the new stay; they all overlap.
// Each lookup is O(log n) plus the overlaps found.
// Invariant: the stays in one calendar never overlap each other (they are a unit's approved
// bookings, and approving a booking rejects anything overlapping it). That is why only the
// floor entry can reach into the new stay from before.
public class AvailabilityCalendar<T> {

    /** A booked stay: [start, end) plus whatever the caller wants back (a booking id, a booking). */
    public record Stay<T>(LocalDateTime start, LocalDateTime end, T value) {
    }

    private final TreeMap<LocalDateTime, List<Stay<T>>> byStart = new TreeMap<>();

    public void add(LocalDateTime start, LocalDateTime end, T value) {
        byStart.computeIfAbsent(start, k -> new ArrayList<>()).add(new Stay<>(start, end, value));
    }

    /** Every stay that shares at least one moment with [start, end). Checkout day = next check-in day is fine. */
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

    public boolean isFree(LocalDateTime start, LocalDateTime end) {
        return overlapping(start, end).isEmpty();
    }
}
