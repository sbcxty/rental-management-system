package com.gudosnest.backend.structures;

import com.gudosnest.backend.model.Item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// DSA: Tree with recursion
// Inventory is a tree: a property has units, and each unit has items (items with no unit belong
// to the property itself, for example shared kitchen equipment). Totals are computed by recursion:
// a node's value is its own items plus the value of every child node. The same function works for
// one unit, one property, or the whole portfolio, however deep the tree is.
public class InventoryTree {

    private final String name;
    private final List<Item> items = new ArrayList<>();
    private final List<InventoryTree> children = new ArrayList<>();

    public InventoryTree(String name) {
        this.name = name;
    }

    public InventoryTree addChild(InventoryTree child) {
        children.add(child);
        return child;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public String getName() {
        return name;
    }

    public List<InventoryTree> getChildren() {
        return children;
    }

    public List<Item> getItems() {
        return items;
    }

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

    /** Number of item lines in this node and below. */
    public int itemCount() {
        int count = items.size();
        for (InventoryTree child : children) {
            count += child.itemCount();
        }
        return count;
    }

    /** Items that are damaged, missing or in maintenance, in this node and below. */
    public int needsAttentionCount() {
        int count = (int) items.stream().filter(i -> i.getCondition().needsAttention()).count();
        for (InventoryTree child : children) {
            count += child.needsAttentionCount();
        }
        return count;
    }
}
