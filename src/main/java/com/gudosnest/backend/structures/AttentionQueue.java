package com.gudosnest.backend.structures;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// DSA: PriorityQueue
// The owner's "Needs attention" list mixes pending bookings and refund requests. The most urgent
// item must come first: a pending booking is urgent by its 48-hour deadline (it is refunded
// automatically when that passes); a refund request by how long the renter has been waiting.
// A PriorityQueue (binary heap) keeps the most urgent item at the head: add is O(log n),
// and taking items out in order is O(log n) each.
public class AttentionQueue<T> {

    /** One item and the moment it should be acted on by. */
    public record Entry<T>(LocalDateTime dueAt, T value) {
    }

    private final PriorityQueue<Entry<T>> heap = new PriorityQueue<>(Comparator.comparing(Entry::dueAt));

    public void add(LocalDateTime dueAt, T value) {
        heap.add(new Entry<>(dueAt, value));
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /** All items, most urgent first. Uses a copy, so the queue itself is left as it was. */
    public List<T> inOrder() {
        PriorityQueue<Entry<T>> copy = new PriorityQueue<>(heap);
        List<T> result = new ArrayList<>(copy.size());
        while (!copy.isEmpty()) {
            result.add(copy.poll().value());
        }
        return result;
    }
}
