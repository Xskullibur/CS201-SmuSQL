package edu.smu.smusql.bplustreeA;

import java.util.ArrayList;
import java.util.List;

public class BPlusTree<K extends Number, V> {

    public static class Range<K> {
        private K start;
        private K end;

        public Range(K start, K end) {
            this.start = start;
            this.end = end;
        }

        public K getStart() {
            return start;
        }

        public K getEnd() {
            return end;
        }
    }

    private Node root;
    private LeafNode firstLeaf;
    private int order;
    private int size;
    private NumberComparator comparator;

    public BPlusTree(int order) {
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

    // Insert a key-value pair into the B+ tree
    public void insert(K key, V value) {
        Node newNode = root.insert(key, value); // Insert into the root node
        if (newNode != null) { // If the root was split
            InternalNode newRoot = new InternalNode(); // Create a new root
            newRoot.keys.add(newNode.getFirstLeafKey()); // Add the first key of the new node to the new root
            newRoot.children.add(root); // Add the old root as the first child
            newRoot.children.add(newNode); // Add the new node as the second child
            root = newRoot; // Update the root
        }
        size++;
    }

    // Search for a value by key in the B+ tree
    public List<V> search(K key) {

        return root.search(key);
    }

    public List<V> rangeSearch(K startKey, K endKey) {
        return root.rangeSearch(startKey, endKey);
    }

    public void update(K key, V newValue) {
        root.update(key, newValue);
    }

    public int getSize() {
        return size; // Return the size of the B+ tree
    }

    public void updateKey(K oldKey, K newKey) {
        // First get all values associated with the old key
        List<V> values = search(oldKey);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Key not found: " + oldKey);
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

        // If root is an internal node with no keys, make its only child the new root
        if (root instanceof InternalNode && root.keys.isEmpty() && ((InternalNode) root).children.size() == 1) {
            root = ((InternalNode) root).children.get(0);
        }

        // If the root is null, recreate the default root
        if (root == null) {
            root = new LeafNode();
            firstLeaf = (LeafNode) root;
        }

    }

    private abstract class Node {
        List<K> keys;

        abstract List<V> search(K key);

        abstract Node insert(K key, V value);

        abstract K getFirstLeafKey();

        abstract List<V> rangeSearch(K startKey, K endKey);

        abstract void update(K key, V newValue);

        abstract List<V> getAllChildren();

        abstract List<K> getKeys();

        abstract Node removeKey(K key);
    }

    private class InternalNode extends Node {
        List<Node> children;

        InternalNode() {
            this.keys = new ArrayList<>();
            this.children = new ArrayList<>();
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

        private int findChildIndex(K key) {
            int index = 0;
            while (index < keys.size() && comparator.compare(key, keys.get(index)) >= 0) {
                index++;
            }
            return index;
        }

        private void insertKeyAndChild(K key, Node child) {
            int index = findChildIndex(key);
            keys.add(index, key);
            children.add(index + 1, child);
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
        void update(K key, V newValue) {
            int childIndex = findChildIndex(key);
            children.get(childIndex).update(key, newValue);
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

            // If child was deleted and this node has only one child
            if (result == null && children.size() <= 1) {
                return null;
            }

            if (childIndex > 0 && childIndex < children.size() && child == children.get(childIndex)) {
                // Only update the key if the child node still has keys
                if (!child.keys.isEmpty()) {
                    keys.set(childIndex - 1, child.getFirstLeafKey());
                } else {
                    // If the child has no keys, remove the corresponding key and child
                    keys.remove(childIndex - 1);
                    children.remove(childIndex);
                }
            }
        
            // Handle underflow by merging or redistributing
            if (child instanceof LeafNode && !child.keys.isEmpty() && child.keys.size() < (order - 1) / 2) {
                handleLeafUnderflow(childIndex);
            } else if (child instanceof InternalNode && !child.keys.isEmpty() && child.keys.size() < (order - 1) / 2) {
                handleInternalUnderflow(childIndex);
            }
            
            return this;
        }

        private void handleLeafUnderflow(int childIndex) {
            LeafNode child = (LeafNode) children.get(childIndex);
            LeafNode leftSibling = childIndex > 0 ? (LeafNode) children.get(childIndex - 1) : null;
            LeafNode rightSibling = childIndex < children.size() - 1 ? (LeafNode) children.get(childIndex + 1) : null;

            // Try to borrow from right sibling first
            if (rightSibling != null && rightSibling.keys.size() > (order - 1) / 2) {
                // Move first key-value pair from right sibling to child
                child.keys.add(rightSibling.keys.remove(0));
                child.values.add(rightSibling.values.remove(0));
                keys.set(childIndex, rightSibling.getFirstLeafKey());
            }
            // Try to borrow from left sibling
            else if (leftSibling != null && leftSibling.keys.size() > (order - 1) / 2) {
                // Move last key-value pair from left sibling to child
                child.keys.add(0, leftSibling.keys.remove(leftSibling.keys.size() - 1));
                child.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
                keys.set(childIndex - 1, child.getFirstLeafKey());
            }
            // Merge with a sibling
            else if (rightSibling != null) {
                mergeLeafNodes(child, rightSibling, childIndex);
            } else if (leftSibling != null) {
                mergeLeafNodes(leftSibling, child, childIndex - 1);
            }
        }

        private void handleInternalUnderflow(int childIndex) {
            InternalNode child = (InternalNode) children.get(childIndex);
            InternalNode leftSibling = childIndex > 0 ? (InternalNode) children.get(childIndex - 1) : null;
            InternalNode rightSibling = childIndex < children.size() - 1 ? (InternalNode) children.get(childIndex + 1)
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
            // Move all keys and values from right to left
            left.keys.addAll(right.keys);
            left.values.addAll(right.values);
            // Update next pointers
            left.next = right.next;
            // Remove the separated key and right node
            keys.remove(keyIndex);
            children.remove(keyIndex + 1);
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

        Node split() {
            int mid = keys.size() / 2;
            InternalNode newNode = new InternalNode();

            newNode.keys = new ArrayList<>(keys.subList(mid + 1, keys.size()));
            newNode.children = new ArrayList<>(children.subList(mid + 1, children.size()));

            keys = new ArrayList<>(keys.subList(0, mid));
            children = new ArrayList<>(children.subList(0, mid + 1));

            return newNode;
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
        List<V> getAllChildren() {
            List<V> allValues = new ArrayList<>();
            for (List<V> valueList : values) {
                allValues.addAll(valueList);
            }
            return allValues;
        }

        @Override
        List<V> search(K key) {
            int index = keys.indexOf(key);
            if (index != -1) {
                return values.get(index); // Return all values for the given key
            }
            return new ArrayList<>(); // Return empty list if key not found
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

        @Override
        Node removeKey(K key) {
            int index = keys.indexOf(key);
            if (index != -1) {
                keys.remove(index);
                values.remove(index);
                return keys.isEmpty() ? null : this;
            }
            return this;
        }

        private int findInsertionPoint(K key) {
            int index = 0;
            while (index < keys.size() && comparator.compare(key, keys.get(index)) > 0) {
                index++;
            }
            return index;
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
                    if (comparator.compare(key, startKey) >= 0 && comparator.compare(key, endKey) <= 0) {
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
        void update(K key, V newValue) {
            int index = keys.indexOf(key);
            if (index != -1) {
                values.get(index).set(0, newValue); // Replace first value for simplicity
            }
        }

        @Override
        List<K> getKeys() {
            return new ArrayList<>(keys);
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
    }
}