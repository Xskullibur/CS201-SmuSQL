package edu.smu.smusql.bplustreeA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BPlusTreeMultiRange<K extends Number, V> {

    private final int order;
    private final NumberComparator comparator;
    private Node root;
    private LeafNode firstLeaf;
    private int size;

    public BPlusTreeMultiRange(int order) {
        this.order = order;
        this.root = new LeafNode();
        this.firstLeaf = (LeafNode) root;
        this.size = 0;
        this.comparator = new NumberComparator();
    }

    public List<V> getAllValues() {
        List<V> allValues = new ArrayList<>();
        LeafNode current = firstLeaf;

        while (current != null) {
            for (List<V> valueList : current.values) {
                allValues.addAll(valueList);
            }
            current = current.next;
        }

        return allValues;
    }

    public List<K> getAllKeys() {
        List<K> allKeys = new ArrayList<>();
        LeafNode current = firstLeaf;

        while (current != null) {
            allKeys.addAll(current.keys);
            current = current.next;
        }

        return allKeys;
    }

    /**
     * For Main Tree Retrieval
     */
    public Map<K, V> getAllKeyValues() {
        Map<K, V> allKeys = new HashMap<>();
        LeafNode current = firstLeaf;

        while (current != null) {

            for (int i = 0; i < current.keys.size(); i++) {
                K key = current.keys.get(i);
                V value = current.values.get(i).get(0);
                allKeys.put(key, value);
            }

            current = current.next;
        }
        return allKeys;
    }

    /**
     * Optimized method to retrieve multiple values by their keys
     *
     * @param keys List of keys to search for, must be sorted
     * @return Map of key-value pairs found
     */
//    public Map<K, V> multiKeySearch(List<K> keys) {
//        if (keys == null || keys.isEmpty() || root == null) {
//            return new HashMap<>();
//        }
//
//        Map<K, V> results = new HashMap<>((int) (keys.size() / 0.75f) + 1);
//        LeafNode leaf = getLeafNode(keys);
//        int keyIndex = 0;
//
//        // Traverse through leaf nodes
//        while (leaf != null && keyIndex < keys.size()) {
//            K targetKey = keys.get(keyIndex);
//
//            // Process all matching keys in current leaf
//            for (int i = 0; i < leaf.keys.size() && keyIndex < keys.size(); i++) {
//                K leafKey = leaf.keys.get(i);
//                int comparison = comparator.compare(leafKey, targetKey);
//
//                if (comparison == 0) {
//                    // Found matching key
//                    results.put(targetKey, leaf.values.get(i).get(0));
//                    keyIndex++;
//                    if (keyIndex < keys.size()) {
//                        targetKey = keys.get(keyIndex);
//                    }
//                } else if (comparison > 0) {
//                    // Current leaf key is greater than target
//                    if (comparator.compare(leafKey, keys.get(keys.size() - 1)) > 0) {
//                        // All remaining leaf keys will be too large
//                        return results;
//                    }
//                    while (keyIndex < keys.size() &&
//                        comparator.compare(keys.get(keyIndex), leafKey) < 0) {
//                        keyIndex++;
//                    }
//                    if (keyIndex < keys.size()) {
//                        targetKey = keys.get(keyIndex);
//                    }
//                }
//            }
//            leaf = leaf.next;
//        }
//
//        return results;
//    }
    public Map<K, V> multiKeySearch(List<K> keys) {
        if (keys == null || keys.isEmpty() || root == null) {
            return new HashMap<>();
        }

        Map<K, V> results = new HashMap<>((int) (keys.size() / 0.75f) + 1);
        LeafNode leaf = getLeafNode(keys);
        int keyIndex = 0;

        while (leaf != null && keyIndex < keys.size()) {
            K currentKey = keys.get(keyIndex);
            K nextKey = keyIndex + 1 < keys.size() ? keys.get(keyIndex + 1) : null;

            // Process current leaf...
            for (int i = 0; i < leaf.keys.size() && keyIndex < keys.size(); i++) {
                K leafKey = leaf.keys.get(i);
                int comparison = comparator.compare(leafKey, currentKey);

                if (comparison == 0) {
                    results.put(currentKey, leaf.values.get(i).get(0));
                    keyIndex++;
                    if (keyIndex < keys.size()) {
                        currentKey = keys.get(keyIndex);
                    }
                }
            }

            // Decide how to move to next key
            if (nextKey != null && shouldUseRootTraversal(currentKey, nextKey)) {
                leaf = getLeafNode(Collections.singletonList(nextKey));
            } else {
                leaf = leaf.next;
            }

//            // Traverse from node
//            leaf = getLeafNode(Collections.singletonList(nextKey));

//            // Traverse from leaf
//            leaf = leaf.next;
        }

        return results;
    }

    private LeafNode getLeafNode(List<K> keys) {

        Node current = root;

        // Traverse to first leaf node that might contain our keys
        while (current instanceof InternalNode) {
            InternalNode node = (InternalNode) current;
            int index = 0;
            while (index < node.keys.size()
                && comparator.compare(keys.get(0), node.keys.get(index)) >= 0) {
                index++;
            }
            current = node.children.get(index);
        }

        return (LeafNode) current;
    }

    private boolean shouldUseRootTraversal(K currentKey, K nextKey) {
        // If keys are numbers, we can estimate distance directly
        double currentVal = currentKey.doubleValue();
        double nextVal = nextKey.doubleValue();
        double range = Math.abs(nextVal - currentVal);

        // If keys are very far apart based on their values
        // we can assume they're likely in different parts of the tree
        double threshold = Math.abs(currentVal * 0.1); // 10% of current value as threshold
        return range > threshold;
    }

    public List<V> rangeSearch(K startKey, K endKey) {
        return root.rangeSearch(startKey, endKey);
    }

    /**
     * Updates all values associated with a key Used primarily for the main tree where each key has
     * one value
     */
    public void update(K key, V newValue) {
        List<V> searchResult = search(key);
        if (searchResult == null || searchResult.isEmpty()) {
            throw new IllegalArgumentException("Key not found: " + key);
        }
        root.update(key, newValue);
    }

    // Search for a value by key in the B+ tree
    public List<V> search(K key) {

        return root.search(key);
    }

    /**
     * Updates a specific old value to a new value for a given key Used primarily for index trees
     * where each key can have multiple values
     */
    public void updateValue(K key, V oldValue, V newValue) {
        List<V> searchResult = search(key);
        if (searchResult == null || searchResult.isEmpty()) {
            throw new IllegalArgumentException("Key not found: " + key);
        }
        if (!searchResult.contains(oldValue)) {
            throw new IllegalArgumentException("Old value not found for key: " + key);
        }
        root.updateValue(key, oldValue, newValue);
    }

    /**
     * Updates an old key to a new key, maintaining all associated values Used for updating keys in
     * both main and index trees
     */
    public void updateKey(K oldKey, K newKey) {
        // First get all values associated with the old key
        List<V> values = search(oldKey);
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Key not found: " + oldKey);
        }

        // Check if new key already exists
        List<V> existingValues = search(newKey);
        if (existingValues != null && !existingValues.isEmpty()) {
            throw new IllegalArgumentException("New key already exists: " + newKey);
        }

        // Remove all entries with the old key
        removeKey(oldKey);

        // Insert all values with the new key
        for (V value : values) {
            insert(newKey, value);
        }
    }

    public void removeKey(K key) {
        root = root.removeKey(key);
        size--;

        // If root is an internal node with no keys and only one child, make its child
        // the new root
        if (root instanceof InternalNode && root.keys.isEmpty() &&
            ((InternalNode) root).children.size() == 1) {
            root = ((InternalNode) root).children.get(0);
        }

        // If the root is null or empty, create a new empty root
        if (root == null || root.keys.isEmpty()) {
            root = new LeafNode();
            firstLeaf = (LeafNode) root;
        }
    }

    // Insert a key-value pair into the B+ tree
    public void insert(K key, V value) {
        Node newNode = root.insert(key, value); // Insert into the root node
        if (newNode != null) { // If the root was split
            InternalNode newRoot = new InternalNode(); // Create a new root
            newRoot.keys.add(
                newNode.getFirstLeafKey()); // Add the first key of the new node to the new root
            newRoot.children.add(root); // Add the old root as the first child
            newRoot.children.add(newNode); // Add the new node as the second child
            root = newRoot; // Update the root
        }
        size++;
    }

    public int getSize() {
        return size; // Return the size of the B+ tree
    }

    public Node removeValue(K key, V value) {
        Node result = root.removeValue(key, value);
        size--;

        // If root is an internal node with no keys and only one child, make its child
        // the new root
        if (root instanceof InternalNode && root.keys.isEmpty() &&
            ((InternalNode) root).children.size() == 1) {
            root = ((InternalNode) root).children.get(0);
        }

        // If the root is null or empty, create a new empty root
        if (root == null || root.keys.isEmpty()) {
            root = new LeafNode();
            firstLeaf = (LeafNode) root;
        }

        return result;
    }

    private abstract class Node {

        List<K> keys;

        abstract List<V> search(K key);

        abstract Node insert(K key, V value);

        abstract K getFirstLeafKey();

        abstract List<V> rangeSearch(K startKey, K endKey);

        abstract List<V> getAllChildren();

        abstract List<K> getKeys();

        abstract Node removeKey(K key);

        abstract Node removeValue(K key, V value);

        abstract void update(K key, V newValue);

        abstract void updateValue(K key, V oldValue, V newValue);
    }

    private class InternalNode extends Node {

        List<Node> children;

        InternalNode() {
            this.keys = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        @Override
        List<V> search(K key) {
            int childIndex = findChildIndex(key);
            return children.get(childIndex).search(key);
        }

        @Override
        Node insert(K key, V value) {
            int childIndex = findChildIndex(key);
            Node newNode = children.get(childIndex).insert(key, value);
            if (newNode != null) {
                // Handle node split
                K newKey = newNode.getFirstLeafKey();
                insertKeyAndChild(newKey, newNode);
                if (keys.size() > order - 1) {
                    return split();
                }
            }
            return null;
        }

        private void insertKeyAndChild(K key, Node child) {
            int index = findChildIndex(key);
            keys.add(index, key);
            children.add(index + 1, child);
        }

        Node split() {
            int mid = keys.size() / 2;
            InternalNode newNode = new InternalNode();

            newNode.keys = new ArrayList<>(keys.subList(mid + 1, keys.size()));
            newNode.children = new ArrayList<>(children.subList(mid + 1, children.size()));

            keys = new ArrayList<>(keys.subList(0, mid));
            children = new ArrayList<>(children.subList(0, mid + 1));

            return newNode;
        }

        @Override
        K getFirstLeafKey() {
            return children.get(0).getFirstLeafKey();
        }

        @Override
        List<V> rangeSearch(K startKey, K endKey) {
            int childIndex = findChildIndex(startKey);
            return children.get(childIndex).rangeSearch(startKey, endKey);
        }

        @Override
        List<V> getAllChildren() {
            List<V> allValues = new ArrayList<>();
            for (Node child : children) {
                allValues.addAll(child.getAllChildren());
            }
            return allValues;
        }

        @Override
        List<K> getKeys() {
            List<K> nodeKeys = new ArrayList<>();
            for (Node child : children) {
                nodeKeys.addAll(child.getKeys());
            }
            return nodeKeys;
        }

        @Override
        Node removeKey(K key) {
            int childIndex = findChildIndex(key);
            Node child = children.get(childIndex);
            Node result = child.removeKey(key);

            // Handle case where child becomes empty
            if (result == null) {
                if (children.size() <= 1) {
                    return null;
                }
                keys.remove(childIndex > 0 ? childIndex - 1 : 0);
                children.remove(childIndex);

                // Update the key if we removed a middle child
                if (childIndex > 0 && childIndex < children.size()) {
                    keys.set(childIndex - 1, children.get(childIndex).getFirstLeafKey());
                }
            } else if (childIndex > 0) {
                // Update the separator key
                keys.set(childIndex - 1, result.getFirstLeafKey());
            }

            // Handle underflow
            if (result instanceof LeafNode && !result.keys.isEmpty() &&
                result.keys.size() < (order - 1) / 2) {
                handleLeafUnderflow(childIndex);
            } else if (result instanceof InternalNode && !result.keys.isEmpty() &&
                result.keys.size() < (order - 1) / 2) {
                handleInternalUnderflow(childIndex);
            }

            return this;
        }

        @Override
        Node removeValue(K key, V value) {
            int childIndex = findChildIndex(key);
            Node child = children.get(childIndex);
            Node result = child.removeValue(key, value);

            // Handle case where child becomes empty
            if (result == null) {
                if (children.size() <= 1) {
                    return null;
                }
                keys.remove(childIndex > 0 ? childIndex - 1 : 0);
                children.remove(childIndex);

                // Update the key if we removed a middle child
                if (childIndex > 0 && childIndex < children.size()) {
                    keys.set(childIndex - 1, children.get(childIndex).getFirstLeafKey());
                }
            } else if (childIndex > 0) {
                // Update the separator key
                keys.set(childIndex - 1, result.getFirstLeafKey());
            }

            // Handle underflow
            if (result instanceof LeafNode && !result.keys.isEmpty() &&
                result.keys.size() < (order - 1) / 2) {
                handleLeafUnderflow(childIndex);
            } else if (result instanceof InternalNode && !result.keys.isEmpty() &&
                result.keys.size() < (order - 1) / 2) {
                handleInternalUnderflow(childIndex);
            }

            return this;
        }

        @Override
        void update(K key, V newValue) {
            int childIndex = findChildIndex(key);
            children.get(childIndex).update(key, newValue);
        }

        @Override
        void updateValue(K key, V oldValue, V newValue) {
            int childIndex = findChildIndex(key);
            children.get(childIndex).updateValue(key, oldValue, newValue);
        }

        private void handleLeafUnderflow(int childIndex) {
            LeafNode child = (LeafNode) children.get(childIndex);

            // Only proceed if the child has keys
            if (child.keys.isEmpty()) {
                return;
            }

            LeafNode leftSibling = childIndex > 0 ? (LeafNode) children.get(childIndex - 1) : null;
            LeafNode rightSibling =
                childIndex < children.size() - 1 ? (LeafNode) children.get(childIndex + 1) : null;

            // Try to borrow from siblings first
            if (rightSibling != null && rightSibling.keys.size() > (order - 1) / 2) {
                // Borrow from right
                child.keys.add(rightSibling.keys.remove(0));
                child.values.add(rightSibling.values.remove(0));
                if (childIndex < keys.size()) {
                    keys.set(childIndex, rightSibling.getFirstLeafKey());
                }
            } else if (leftSibling != null && leftSibling.keys.size() > (order - 1) / 2) {
                // Borrow from left
                child.keys.add(0, leftSibling.keys.remove(leftSibling.keys.size() - 1));
                child.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
                keys.set(childIndex - 1, child.getFirstLeafKey());
            } else {
                // Merge if borrowing is not possible
                if (rightSibling != null && !rightSibling.keys.isEmpty()) {
                    mergeLeafNodes(child, rightSibling, childIndex);
                } else if (leftSibling != null && !leftSibling.keys.isEmpty()) {
                    mergeLeafNodes(leftSibling, child, childIndex - 1);
                }
            }
        }

        private void handleInternalUnderflow(int childIndex) {
            InternalNode child = (InternalNode) children.get(childIndex);
            InternalNode leftSibling =
                childIndex > 0 ? (InternalNode) children.get(childIndex - 1) : null;
            InternalNode rightSibling =
                childIndex < children.size() - 1 ? (InternalNode) children.get(childIndex + 1)
                    : null;

            // Try to borrow from right sibling first
            if (rightSibling != null && rightSibling.keys.size() > (order - 1) / 2) {
                // Move parent key down to child
                child.keys.add(keys.get(childIndex));
                // Move first key from right sibling up to parent
                keys.set(childIndex, rightSibling.keys.remove(0));
                // Move first child from right sibling to child's last position
                child.children.add(rightSibling.children.remove(0));
            }
            // Try to borrow from left sibling
            else if (leftSibling != null && leftSibling.keys.size() > (order - 1) / 2) {
                // Move parent key down to child
                child.keys.add(0, keys.get(childIndex - 1));
                // Move last key from left sibling up to parent
                keys.set(childIndex - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
                // Move last child from left sibling to child's first position
                child.children.add(0, leftSibling.children.remove(leftSibling.children.size() - 1));
            }
            // Merge with a sibling
            else if (rightSibling != null) {
                mergeInternalNodes(child, rightSibling, childIndex);
            } else if (leftSibling != null) {
                mergeInternalNodes(leftSibling, child, childIndex - 1);
            }
        }

        private void mergeLeafNodes(LeafNode left, LeafNode right, int keyIndex) {
            // Only merge if both nodes have keys
            if (!left.keys.isEmpty() && !right.keys.isEmpty()) {
                left.keys.addAll(right.keys);
                left.values.addAll(right.values);
                left.next = right.next;

                // Remove the separator key and the right node
                if (keyIndex < keys.size()) {
                    keys.remove(keyIndex);
                }
                children.remove(keyIndex + 1);
            }
        }

        private void mergeInternalNodes(InternalNode left, InternalNode right, int keyIndex) {
            // Add the parent key to the left node
            left.keys.add(keys.remove(keyIndex));
            // Move all keys and children from right to left
            left.keys.addAll(right.keys);
            left.children.addAll(right.children);
            // Remove the right node
            children.remove(keyIndex + 1);
        }

        private int findChildIndex(K key) {
            int index = 0;
            while (index < keys.size() && comparator.compare(key, keys.get(index)) >= 0) {
                index++;
            }
            return index;
        }
    }

    private class LeafNode extends Node {

        List<List<V>> values; // List of lists of values for each key
        private LeafNode next; // Pointer to the next leaf node

        LeafNode() {
            this.keys = new ArrayList<>();
            this.values = new ArrayList<>();
            this.next = null;
        }

        @Override
        List<V> search(K key) {
            int index = keys.indexOf(key);
            if (index != -1) {
                return values.get(index); // Return all values for the given key
            }
            return null; // Return empty list if key not found
        }

        @Override
        Node insert(K key, V value) {
            int index = keys.indexOf(key);
            if (index != -1) {
                // Key exists, add value to the existing list
                values.get(index).add(value);
            } else {
                // Key doesn't exist, insert new key-value pair
                int insertionPoint = findInsertionPoint(key);
                keys.add(insertionPoint, key);
                List<V> valueList = new ArrayList<>();
                valueList.add(value);
                values.add(insertionPoint, valueList);
            }

            if (keys.size() > order - 1) {
                return split();
            }

            return null;
        }

        private int findInsertionPoint(K key) {
            int index = 0;
            while (index < keys.size() && comparator.compare(key, keys.get(index)) > 0) {
                index++;
            }
            return index;
        }

        Node split() {
            int mid = keys.size() / 2;
            LeafNode newNode = new LeafNode();

            // Move half of keys and values to a new node
            newNode.keys = new ArrayList<>(keys.subList(mid, keys.size()));
            newNode.values = new ArrayList<>(values.subList(mid, values.size()));

            // Keep the first half of keys and values
            keys = new ArrayList<>(keys.subList(0, mid));
            values = new ArrayList<>(values.subList(0, mid));

            // Update pointers
            newNode.next = this.next;
            this.next = newNode;
            return newNode;
        }

        @Override
        K getFirstLeafKey() {
            return keys.get(0);
        }

        @Override
        List<V> rangeSearch(K startKey, K endKey) {
            List<V> result = new ArrayList<>();
            LeafNode currentNode = this;
            boolean started = false;

            while (currentNode != null) {
                for (int i = 0; i < currentNode.keys.size(); i++) {
                    K key = currentNode.keys.get(i);
                    if (comparator.compare(key, startKey) >= 0
                        && comparator.compare(key, endKey) <= 0) {
                        result.addAll(currentNode.values.get(i));
                        started = true;
                    } else if (started) {
                        return result;
                    }
                }
                currentNode = currentNode.next;
            }
            return result;
        }

        @Override
        List<V> getAllChildren() {
            List<V> allValues = new ArrayList<>();
            for (List<V> valueList : values) {
                allValues.addAll(valueList);
            }
            return allValues;
        }

        @Override
        List<K> getKeys() {
            return new ArrayList<>(keys);
        }

        @Override
        Node removeKey(K key) {
            int index = keys.indexOf(key);
            if (index != -1) {
                keys.remove(index);
                values.remove(index);

                // Return null if this node becomes empty
                return keys.isEmpty() ? null : this;
            }
            return this;
        }

        @Override
        Node removeValue(K key, V value) {
            int index = keys.indexOf(key);
            if (index != -1) {
                List<V> valueList = values.get(index);
                valueList.remove(value);

                // If no more values for this key, remove the key entirely
                if (valueList.isEmpty()) {
                    keys.remove(index);
                    values.remove(index);
                    return keys.isEmpty() ? null : this;
                }

                // Update the values list
                values.set(index, valueList);
            }
            return this;
        }

        @Override
        void update(K key, V newValue) {
            int index = keys.indexOf(key);
            if (index != -1) {
                // Create new value list with only the new value
                List<V> newValueList = new ArrayList<>();
                newValueList.add(newValue);
                values.set(index, newValueList);
            }
        }

        @Override
        void updateValue(K key, V oldValue, V newValue) {
            int index = keys.indexOf(key);
            if (index != -1) {
                List<V> valueList = values.get(index);
                int valueIndex = valueList.indexOf(oldValue);
                if (valueIndex != -1) {
                    valueList.set(valueIndex, newValue);
                }
            }
        }
    }
}