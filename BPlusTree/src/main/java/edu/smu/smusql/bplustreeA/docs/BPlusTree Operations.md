# B+ Tree Operations Guide

## Basic Structure (Order = 4)
Each node can have a maximum of 3 keys (order-1) and minimum of 1 key ((order-1)/2, rounded up).

### Initial Empty Tree and Basic Insertions

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    subgraph "After Insertions: 5, 3, 7"
        B["Leaf [3 | 5 | 7]"]:::leafNode
    end

    subgraph "Initial Empty Tree"
        A["Empty Leaf [ ]"]:::leafNode
    end
```

## 1. Node Splitting Operations

### Basic Split (Insert 2 into full node)

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    subgraph "After Split"
        C["Root [5]"]:::rootNode
        D["Leaf [2 | 3]"]:::leafNode
        E["Leaf [5 | 7]"]:::leafNode
        C --> D
        C --> E
        D -.- E
    end

    subgraph "Before Split"
        B["Leaf [3 | 5 | 7]"]:::leafNode
    end
```

### Multi-Level Split Example (After inserting 1,4,6,8,9)

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef internalNode fill:#fff5e6,stroke:#ffb366,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    F["Root [5]"]:::rootNode
    G["Internal [7]"]:::internalNode
    H["Leaf [2 | 3 | 4]"]:::leafNode
    I["Leaf [5 | 6]"]:::leafNode
    J["Leaf [7 | 8 | 9]"]:::leafNode
    F --> H
    F --> G
    G --> I
    G --> J
    H -.- I
    I -.- J
```

## 2. Complex Operations Examples

### Double Split Example (Insert 10)

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    A["Root [5 | 8]"]:::rootNode
    B["Leaf [2 | 3 | 4]"]:::leafNode
    C["Leaf [5 | 7]"]:::leafNode
    D["Leaf [8 | 9 | 10]"]:::leafNode
    A --> B
    A --> C
    A --> D
    B -.- C
    C -.- D
```

### Multi-Value Keys Example
When handling duplicate values (e.g., for index trees), values are stored as lists:

```mermaid
graph TD
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;

    A["Leaf [age:25 → [user1, user2, user4] | age:30 → [user3]]"]:::leafNode
```

## 3. Deletion Operations

### Simple Deletion (No Underflow)

```mermaid
graph TD
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;

    subgraph "After Delete 5"
        B["Leaf [3 | 7]"]:::leafNode
    end

    subgraph "Before Delete 5"
        A["Leaf [3 | 5 | 7]"]:::leafNode
    end
```

### Deletion with Redistribution

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    subgraph "After Delete 2"
        D["Root [6]"]:::rootNode
        E["Leaf [3 | 5]"]:::leafNode
        F["Leaf [6 | 7]"]:::leafNode
        D --> E
        D --> F
        E -.- F
    end

    subgraph "Before Delete 2"
        A["Root [5]"]:::rootNode
        B["Leaf [2 | 3]"]:::leafNode
        C["Leaf [5 | 6 | 7]"]:::leafNode
        A --> B
        A --> C
        B -.- C
    end
```

## 4. Underflow Handling

### Case 1: Borrow from Left Sibling

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    subgraph "After Borrowing"
        D["Root [4]"]:::rootNode
        E["Leaf [2 | 3]"]:::leafNode
        F["Leaf [4 | 5]"]:::leafNode
        D --> E
        D --> F
        E -.- F
    end

    subgraph "Before (Delete 6)"
        A["Root [5]"]:::rootNode
        B["Leaf [2 | 3 | 4]"]:::leafNode
        C["Leaf [5 | 6]"]:::leafNode
        A --> B
        A --> C
        B -.- C
    end
```

### Case 2: Merge Nodes

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    subgraph "After Merge"
        D["Root/Leaf [2 | 3 | 5]"]:::rootNode
    end

    subgraph "Before Merge"
        A["Root [5]"]:::rootNode
        B["Leaf [2 | 3]"]:::leafNode
        C["Leaf [5 | 6]"]:::leafNode
        A --> B
        A --> C
        B -.- C
    end
```

## 5. Complex Scenarios

### Multiple Level Operations

```mermaid
graph TD
    %% Style definitions
    classDef leafNode fill:#e6f3ff,stroke:#4d94ff,stroke-width:2px;
    classDef rootNode fill:#f0f5e6,stroke:#8bc34a,stroke-width:2px;

    subgraph "After Deleting 7,8"
        E["Root [5]"]:::rootNode
        F["Leaf [2 | 3 | 4]"]:::leafNode
        G["Leaf [5 | 10 | 12 | 14]"]:::leafNode
        E --> F
        E --> G
        F -.- G
    end

    subgraph "Before Deletions"
        A["Root [5 | 10]"]:::rootNode
        B["Leaf [2 | 3 | 4]"]:::leafNode
        C["Leaf [5 | 7 | 8]"]:::leafNode
        D["Leaf [10 | 12 | 14]"]:::leafNode
        A --> B
        A --> C
        A --> D
        B -.- C
        C -.- D
    end
```

## Implementation Notes

### Node Split Algorithm
```java
Node split() {
    int mid = keys.size() / 2;
    LeafNode newNode = new LeafNode();
    
    // Move half of keys and values to new node
    newNode.keys = new ArrayList<>(keys.subList(mid, keys.size()));
    newNode.values = new ArrayList<>(values.subList(mid, values.size()));
    
    // Update current node
    keys = new ArrayList<>(keys.subList(0, mid));
    values = new ArrayList<>(values.subList(0, mid));
    
    // Update links
    newNode.next = this.next;
    this.next = newNode;
    
    return newNode;
}
```

### Underflow Handling Algorithm
```java
private void handleUnderflow(int childIndex) {
    Node child = children.get(childIndex);
    
    // Try borrow from left
    if (childIndex > 0 && children.get(childIndex - 1).canLend()) {
        borrowFromLeft(childIndex);
    }
    // Try borrow from right
    else if (childIndex < children.size() - 1 && children.get(childIndex + 1).canLend()) {
        borrowFromRight(childIndex);
    }
    // Must merge
    else {
        mergeNodes(childIndex);
    }
}
```