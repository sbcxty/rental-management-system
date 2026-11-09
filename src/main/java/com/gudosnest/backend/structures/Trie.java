package com.gudosnest.backend.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// DSA: Trie
// Suggestions under the Browse "Location" box. Every location and property name is stored
// letter by letter in a prefix tree; typing "ta" walks two letters down and lists every word
// below that node (Tagaytay, Cavite, ...). Lookup time depends on the length of what was typed,
// not on how many places are stored. Children are kept in a TreeMap so suggestions come out
// in alphabetical order.
public class Trie {

    private static final class Node {
        final Map<Character, Node> children = new TreeMap<>();
        String word;   // set on the node where a stored word ends (original spelling)
    }

    private final Node root = new Node();

    public void insert(String word) {
        Node node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new Node());
        }
        node.word = word;
    }

    /** Up to {@code limit} stored words starting with {@code prefix} (any case), alphabetical. */
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

    private void collect(Node node, List<String> result, int limit) {
        if (result.size() >= limit) {
            return;
        }
        if (node.word != null) {
            result.add(node.word);
        }
        for (Node child : node.children.values()) {
            collect(child, result, limit);
        }
    }
}
