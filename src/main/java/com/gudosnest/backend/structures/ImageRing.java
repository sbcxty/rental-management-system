package com.gudosnest.backend.structures;

import java.util.List;

// DSA: Circular doubly linked list
// Each node points to the next and the previous photo, and the last node links back to the
// first. "Next" on the last photo wraps to the first with no index math or bounds checks,
// and next/previous are always O(1).
public class ImageRing<T> {

    private static final class Node<T> {
        final T value;
        Node<T> next;
        Node<T> prev;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> current;
    private Node<T> head;
    private int size;
    private int position;   // 1-based position of current, for the "2 / 5" counter

    public ImageRing(List<T> items) {
        for (T item : items) {
            add(item);
        }
    }

    /** Adds an item at the end of the ring (just before head). */
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

    public T current() {
        return current == null ? null : current.value;
    }

    public T next() {
        if (current != null) {
            current = current.next;
            position = position == size ? 1 : position + 1;
        }
        return current();
    }

    public T previous() {
        if (current != null) {
            current = current.prev;
            position = position == 1 ? size : position - 1;
        }
        return current();
    }

    /** Jumps to a 1-based position (used when a thumbnail is clicked). Walks the ring: O(n). */
    public T moveTo(int target) {
        if (target < 1 || target > size) {
            return current();
        }
        while (position != target) {
            next();
        }
        return current();
    }

    public int size() {
        return size;
    }

    public int position() {
        return position;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
