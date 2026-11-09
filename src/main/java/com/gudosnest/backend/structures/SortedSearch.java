package com.gudosnest.backend.structures;

import java.util.List;
import java.util.function.ToIntFunction;

// DSA: Binary search
// Bookings are listed sorted by booking id. When someone types a reference like
// "GN-2026-8815" in a search box, the number at the end is the booking id, and binary search
// finds it by halving the list each step: O(log n) instead of checking every row.
public final class SortedSearch {

    private SortedSearch() {
    }

    /**
     * Finds the element whose key equals {@code target} in a list sorted by that key (ascending).
     * Returns its index, or -1 if it isn't there.
     */
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

    /**
     * Reads a booking id out of what the user typed: "8815", "#GN-2026-8815", "Ref #GN-2026-8815".
     * Returns -1 when the text doesn't look like a reference.
     */
    public static int bookingIdFromReference(String text) {
        String trimmed = text.trim().toUpperCase().replace("REF", "").replace("#", "").trim();
        if (trimmed.startsWith("GN-")) {
            int lastDash = trimmed.lastIndexOf('-');
            trimmed = trimmed.substring(lastDash + 1);
        }
        if (trimmed.isEmpty() || !trimmed.chars().allMatch(Character::isDigit) || trimmed.length() > 9) {
            return -1;
        }
        return Integer.parseInt(trimmed);
    }
}
