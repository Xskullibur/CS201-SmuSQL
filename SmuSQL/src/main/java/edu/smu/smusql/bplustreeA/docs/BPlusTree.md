# BPlusTree Class Documentation

## Overview
The `BPlusTree` class implements a B+ tree data structure that maintains sorted data for efficient retrieval, insertion, and deletion operations. It supports generic key types that extend `Number` and any value type.

## Class Structure

```java
public class BPlusTree<K extends Number, V> {
    private final int order;
    private final NumberComparator comparator;
    private Node root;
    private LeafNode firstLeaf;
    private int size;
}
```

### Core Components

1. **Node Classes**
    - `Node`: Abstract base class for tree nodes
    - `InternalNode`: Non-leaf nodes containing routing keys
    - `LeafNode`: Leaf nodes storing actual data

2. **Key Fields**
   ```java
   private final int order;          // Maximum number of keys per node
   private Node root;                // Root node of the tree
   private LeafNode firstLeaf;       // First leaf for sequential access
   private int size;                 // Total number of key-value pairs
   ```

## Key Operations

### 1. Insertion
```java
public void insert(K key, V value)
```
- Traverses to appropriate leaf node
- Handles node splitting if necessary
- Updates tree structure
- Maintains tree balance

Process:
1. Locate target leaf node
2. Insert key-value pair
3. Split node if overflow occurs
4. Propagate changes upward if needed

### 2. Search Operations

#### Single Key Search
```java
public List<V> search(K key)
```
- Returns all values associated with the key
- Supports multiple values per key

#### Range Search
```java
public List<V> rangeSearch(K startKey, K endKey)
```
- Returns values for keys within range
- Utilizes leaf node links for efficiency

#### Multi-Key Search
```java
public Map<K, V> multiKeySearch(List<K> keys)
```
- Optimized batch retrieval
- Returns map of found key-value pairs

### 3. Update Operations

#### Value Update
```java
public void update(K key, V newValue)
```
- Updates all values for a key

#### Specific Value Update
```java
public void updateValue(K key, V oldValue, V newValue)
```
- Updates specific value for a key

#### Key Update
```java
public void updateKey(K oldKey, K newKey)
```
- Moves values from old key to new key

### 4. Deletion Operations

#### Key Deletion
```java
public void removeKey(K key)
```
- Removes key and all associated values
- Handles node underflow

#### Value Deletion
```java
public Node removeValue(K key, V value)
```
- Removes specific value for a key
- Maintains key if other values exist

## Helper Methods

### Data Retrieval
```java
public List<V> getAllValues()
public List<K> getAllKeys()
public Map<K, V> getAllKeyValues()
```

### Tree Information
```java
public int getSize()
```

## Implementation Details

### Node Structure

1. **Internal Node**
```java
private class InternalNode extends Node {
    List<Node> children;
    List<K> keys;
}
```

2. **Leaf Node**
```java
private class LeafNode extends Node {
    List<List<V>> values;
    List<K> keys;
    LeafNode next;
}
```

### Node Operations

1. **Split Operations**
   ```java
   Node split()  // Splits node when overflow occurs
   ```

2. **Merge Operations**
   ```java
   void mergeNodes()  // Merges nodes when underflow occurs
   ```

## Performance Characteristics

### Time Complexity
- Search: O(log n)
- Insert: O(log n)
- Delete: O(log n)
- Range Query: O(log n + m) where m is number of elements in range

### Space Complexity
- O(n) for n key-value pairs
- Additional overhead for internal nodes

## Usage Example

```java
// Create tree with order 4
BPlusTree<Integer, String> tree = new BPlusTree<>(4);

// Insert values
tree.insert(1, "One");
tree.insert(2, "Two");

// Search
List<String> result = tree.search(1);

// Range search
List<String> rangeResult = tree.rangeSearch(1, 5);

// Update
tree.update(1, "Updated One");

// Delete
tree.removeKey(1);
```