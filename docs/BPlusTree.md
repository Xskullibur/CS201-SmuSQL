# B+ Tree Implementation

## BPlusTree Data Structure

### Structure Overview

The B+ Tree is implemented with a distinction between internal (routing) nodes and leaf nodes that
store actual data. This design enables efficient range queries and maintains data sorted at the leaf
level.

- Generic implementation supporting `Number` keys and any value type
- Configurable order for tree balancing (Using 128)
- Linked leaf nodes for efficient range queries
- Support for duplicate values in index trees

### Basic Components

#### 1. Core Classes

```java
public class BPlusTree<K extends Number, V> {

    private final int order;              // Maximum children per node
    private final NumberComparator comparator;
    private Node root;                    // Root node
    private LeafNode firstLeaf;           // First leaf for range queries
    private int size;                     // Total key-value pairs
}
```

- `K extends Number`: Enables numeric comparison for keys
- `V`: Any value type supported for flexibility in storage

#### 2. Node Types

```
- Abstract Node Base Class
  ├── Internal Node (routing)
  │   ├── Keys List
  │   └── Children List
  └── Leaf Node (data storage)
      ├── Keys List
      ├── Values List
      └── Next Leaf Pointer
```


##### Internal Node
- Internal nodes don't store values, only routing information

##### Leaf Node
- Leaf nodes store both keys and values in parallel lists
- `List<List<V>>` structure supports multiple values per key (for index trees)

### 1. Insertion Process

```mermaid
graph TD
    %% Initial Process
    A[Start Insert] --> B[Find Target Leaf]
    B --> C{Key Exists?}
    
    %% Existing Key Case
    C -->|Yes| D{Is Index Tree?}
    D -->|Yes| E[Add Value to<br>Existing List]
    D -->|No| F[Return:<br>Key Exists Error]
    
    %% New Key Case
    C -->|No| G[Find Insertion Point]
    G --> H[Insert Key-Value Pair]
    H --> I{Node Full?}
    I -->|No| J[Return null]
    
    %% Split Process
    I -->|Yes| K[Split Node]
    K --> L[Create New Node]
    L --> M[Move Half Keys/Values<br>to New Node]
    M --> N[Update Next Pointers]
    
    %% Parent Update Process
    N --> O{Is Root?}
    O -->|Yes| P[Create New Root]
    P --> Q[Set Old Node as<br>Left Child]
    Q --> R[Set New Node as<br>Right Child]
    
    O -->|No| S[Get Parent Node]
    S --> T[Insert Split Key<br>in Parent]
    T --> U{Parent Full?}
    U -->|Yes| V[Split Parent Node]
    U -->|No| W[Update Parent<br>Pointers]
    
    %% Completion
    E --> X[Return]
    J --> X
    R --> X
    V --> W
    W --> X

    %% Style Definitions
    classDef start fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;
    classDef process fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
    classDef decision fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
    classDef terminal fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;
    
    %% Apply Styles
    class A start;
    class B,G,H,K,L,M,N,P,Q,R,S,T,W process;
    class C,D,I,O,U decision;
    class E,F,J,X terminal;
```

#### Steps:
1. Key Existence Check 

2. Insertion Phase
   - Find correct insertion point
   - Insert key-value pair
   - Check node capacity
   - Handle overflow if necessary

3. Split Process (Overflow)
   - Create new node
   - Calculate split point (size/2)
   - Move half of keys/values to new node
   - Update leaf node links
   - Handle parent references

### 2. Search Operations

#### Single Key Search

```mermaid
graph TD
    A[Start] --> B[Get Root Node]
    B --> C{Is Internal Node?}
    C -->|Yes| D[Compare Key with Node Keys]
    D --> E[Select Child Node]
    E --> C
    C -->|No| F[Found Leaf Node]
    F --> G[Search for Key]
    G --> H{Key Found?}
    H -->|Yes| I[Return Values List]
    H -->|No| J[Return null]

    %% Style for nodes
    classDef process fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
    classDef decision fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
    classDef terminator fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;

    class A,B,D,E,F,G terminator;
    class C,H decision;
    class I,J process;
```

1. Input Validation
   - Check if key is not null
   - Verify root exists

2. Tree Traversal
   - Start at root node
   - Compare key with node's keys
   - Select appropriate child node
   - Repeat until leaf node reached

3. Key Search
   - Search for key in leaf node
   - Return list of values if found
   - Return null if not found

#### Range Search

```mermaid
graph TD
    A[Start] --> B[Initialize Result List]
    B --> C[Find Start Leaf Node]
    C --> D[Current = Start Node]
    D --> E{Current != null?}
    E -->|Yes| F[Check Keys in Node]
    F --> G{Key in Range?}
    G -->|Yes| H[Add Values to Result]
    G -->|No| I{Past End Key?}
    I -->|No| K[Continue]
    I -->|Yes| J[Return Result]
    H --> K
    K --> L[Move to Next Key]
    L --> M{More Keys<br>in Node?}
    M -->|Yes| F
    M -->|No| N[Current = Next Leaf]
    N --> E
    E -->|No| J

    %% Style for nodes
    classDef process fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
    classDef decision fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
    classDef terminator fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;

    class A,B,C,D,H,K,L,N terminator;
    class E,G,I,M decision;
    class F,J process;
```

1. Range Traversal
   - Start at leaf containing startKey
   - Process keys in current node
   - Follow next leaf pointer
   - Continue until endKey reached

2. Result Collection
   - Check each key against range
   - Collect matching values
   - Handle duplicate values
   - Early termination if past endKey

#### Multi-Key Search

```mermaid
graph TD
    A[Start] --> B{Input Valid?}
    B -->|No| C[Return Empty Map]
    B -->|Yes| D[Initialize Results Map]
    D --> E[Find First Leaf Node]
    E --> F[Set KeyIndex = 0]
    F --> G{Leaf != null AND<br>KeyIndex < keys.size?}
    G -->|Yes| H[Get Target Key]
    H --> I[Process Node Keys]
    I --> J{Key Found?}
    J -->|Yes| K[Add to Results]
    K --> L[Increment KeyIndex]
    J -->|No| M{Key > Target?}
    M -->|Yes| N[Skip Smaller Keys]
    M -->|No| O[Continue]
    N --> P[Update Target Key]
    O --> Q[Next Key in Node]
    P --> Q
    Q --> R{More Keys<br>in Node?}
    R -->|Yes| I
    R -->|No| S[Move to Next Leaf]
    S --> G
    L --> T{More Keys<br>to Find?}
    T -->|Yes| U[Update Target]
    T -->|No| V[Continue]
    U --> Q
    V --> Q
    G -->|No| W[Return Results]

    %% Style for nodes
    classDef process fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
    classDef decision fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
    classDef terminator fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;

    class A,D,E,F,H,K,L,N,O,P,Q,S,U,V terminator;
    class B,G,J,M,R,T decision;
    class C,I,W process;
```

1. Initial Setup
   - Find first leaf node
   - Get first target key

2. Search Process
   - Process keys in current leaf
   - Compare with target key
   - Handle matches and mismatches
   - Track progress through key list

3. Optimization Steps
   - Skip keys if target is larger
   - Early termination if all keys found
   - Handle boundary conditions

4. Result Management
   - Store matches in result map
   - Track found keys
   - Maintain key ordering
   - Handle multiple values per key

### 3. Deletion Operation

```mermaid
graph TD
    %% Initial Process
    A[Start Delete] --> B[Find Target Leaf]
    B --> C{Key Found?}
    C -->|No| D[Return null]
    
    %% Delete Process
    C -->|Yes| E[Remove Key and Value]
    E --> F{Is Root?}
    
    %% Root Handling
    F -->|Yes| G{Empty Root?}
    G -->|Yes| H[Create New Empty Root]
    G -->|No| I[Continue]
    
    %% Underflow Handling
    F -->|No| J{Underflow?}
    J -->|No| K[Return Node]
    
    %% Rebalancing Process
    J -->|Yes| L{Can Borrow<br>from Right?}
    L -->|Yes| M[Borrow from<br>Right Sibling]
    L -->|No| N{Can Borrow<br>from Left?}
    N -->|Yes| O[Borrow from<br>Left Sibling]
    N -->|No| P{Has Right<br>Sibling?}
    
    %% Merge Process
    P -->|Yes| Q[Merge with<br>Right Sibling]
    P -->|No| R[Merge with<br>Left Sibling]
    
    %% Post Merge
    Q --> S[Update Parent<br>Separator Key]
    R --> S
    S --> T[Remove Empty Node]
    T --> U{Parent<br>Underflow?}
    U -->|Yes| V[Handle Parent<br>Underflow]
    U -->|No| W[Update Tree<br>Structure]
    
    %% Final Steps
    M --> X[Return Updated Node]
    O --> X
    V --> X
    W --> X
    
    %% Style Definitions
    classDef start fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;
    classDef process fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
    classDef decision fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
    classDef terminal fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;
    
    %% Apply Styles
    class A,H start;
    class B,E,M,O,Q,R,S,T,W process;
    class C,F,G,J,L,N,P,U decision;
    class D,K,X terminal;
```

#### Steps:

1. Locate the key
2. Remove key and value
3. Handle underflow if necessary

### 4. Update Operations

#### Full Value Update (Used primarily in main tree operations)
1. Search for key in tree
2. Validate key exists
3. Create new single-value list
4. Replace all values for key
5. Return

#### Specific Value Update (Used in index tree operations) 

1. Search for key in tree 
2. Validate key exists 
3. Verify old value exists in value list 
4. Replace specific value while maintaining others 
5. Return

#### Key Update
- Moves all values from old key to new key
- Maintains value associations
- Handles key conflicts

```mermaid
graph TD
    A[Start updateKey] --> B[Search for oldKey]
    B --> C{oldKey exists?}
    C -->|No| D[Throw Exception:<br>Key Not Found]
    C -->|Yes| E[Get all values<br>for oldKey]
    E --> F[Search for newKey]
    F --> G{newKey exists?}
    G -->|Yes| H[Throw Exception:<br>New Key Exists]
    G -->|No| I[Remove oldKey<br>and values]
    I --> J[Start Value Insertion]
    J --> K[Insert each value<br>with newKey]
    K --> L{More values?}
    L -->|Yes| K
    L -->|No| M[Return]

    %% Style Definitions
    classDef process fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
    classDef decision fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
    classDef terminal fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;

    %% Apply Styles
    class A,M terminal;
    class B,E,I,J,K process;
    class C,G,L decision;
    class D,H process;
```