# B+ Tree Engine Implementation

## Core Architecture

### 1. Main Components

- **BPlusTree**: Core data structure implementation
- **BPlusTreeEngine**: SQL query processing engine
- **BPlusTreeTableHashMap**: Table representation using B+ trees

### 2. Storage Structure

- **Main Tree**:
    - Key: Primary Key (Integer)
    - Value: Row Data (HashMap)
- **Index Trees**:
    - Key: Column Values
    - Value: Primary Keys
    - One index tree per column for efficient querying

## BPlusTree Engine

### Operations Overview

#### Insert Operations
```mermaid
graph TD
    A[Start] --> B[Parse Insert Node]
    B --> C[Extract Table Info/Primary Key/Values]
    C --> D[Validate Table Exists]
    D --> E[Validate Column Count]
    E --> F{Check Primary Key<br>Exists?}
    F -->|Yes| G[Return Error:<br>Key Exists]
    F -->|No| H[Invalidate Cache]
    H --> I[Create Row Data HashMap]
    I --> J[Loop Through Columns]
    J --> K[Convert Values<br>to Appropriate Type]
    K --> L[Update Index Trees]
    L --> M[Insert Into Main Tree]
    M --> N[Return Success]
```

#### Delete Operations
```mermaid
graph TD
    A[Start] --> B[Parse Delete Node]
    B --> C[Extract Table/Conditions]
    C --> D[Validate Table Exists]
    D --> E{Table Empty?}
    E -->|Yes| F[Return: No Rows]
    E -->|No| G[Evaluate Where Clause]
    G --> H[Get Filtered Keys]
    H --> I{Keys Found?}
    I -->|No| J[Return: Not Found]
    I -->|Yes| K[Invalidate Cache]
    K --> L[Loop Through Rows]
    L --> M[Remove from Index Trees]
    M --> N[Remove from Main Tree]
    N --> O[Return Deleted Count]
```

#### Select Operations
```mermaid
graph TD
%% Main Select Flow
  A[Start] --> B[Parse Select Node]
  B --> C[Extract Query Components]
  C --> D{Cache Enabled?}
  D -->|Yes| E{Cache Hit?}
  E -->|Yes| F[Return Cached Result]
  E -->|No| G[Record Cache Miss]
  D -->|No| G
  G --> H[Validate Table Exists]
  H --> I{Where Clause<br>Exists?}
  I -->|No| J{Select *?}
  J -->|Yes| K[Get All Rows]
  J -->|No| L[Get Specified Columns]

%% Condition Evaluation Flow
  I -->|Yes| M[evaluateConditionNode]
  M --> N{Node Types?}

%% Complex Condition Path
  N -->|Both ConditionNodes| O[Process Left Node]
  O --> P[Process Right Node]
  P --> Q{Operator Type?}
  Q -->|AND| R[Optimize AND:<br>Use HashSet]
  Q -->|OR| S[Optimize OR:<br>Union Sets]

%% Simple Condition Path
  N -->|Both ExpressionNodes| T[evaluateSimpleCondition]
  T --> U[Extract Column & Value]
  U --> V{Is Primary Key?}

%% Primary Key Path
  V -->|Yes| W{Operator = '='?}
  W -->|Yes| X[Direct Key Search]
  W -->|No| Y[Filter All Keys]

%% Index Tree Path
  V -->|No| Z[Get Index Tree]
  Z --> AA[Convert Value Type]
  AA --> AB{Operator?}

%% Index Operations
  AB -->|=| AC[Exact Match]
  AB -->|!=| AD[Not Equal:<br>Filter All Values]
  AB -->|<| AE[Less Than:<br>Range Search]
  AB -->|<=| AF[Less Equal:<br>Range Search]
  AB -->|>| AG[Greater Than:<br>Range Search]
  AB -->|>=| AH[Greater Equal:<br>Range Search]

%% Results Combination
  R --> AI[Combine Results]
  S --> AI
  X --> AI
  Y --> AI
  AC --> AI
  AD --> AI
  AE --> AI
  AF --> AI
  AG --> AI
  AH --> AI

%% Final Processing
  AI --> AJ[Get Filtered Rows]
  K --> AK[Format Results]
  L --> AK
  AJ --> AK
  AK --> AL{Cacheable?}
  AL -->|Yes| AM[Update Cache]
  AL -->|No| AN[Return Results]
  AM --> AN

%% Style Definitions
  classDef cacheOps fill:#d4a276,stroke:#333,stroke-width:2px,color:#000;
  classDef evaluation fill:#7ca5b8,stroke:#333,stroke-width:2px,color:#000;
  classDef indexOps fill:#98b08d,stroke:#333,stroke-width:2px,color:#000;

%% Apply Styles
  class E,F,G,AM cacheOps;
  class M,N,O,P,Q,R,S,T cacheOps;
  class Z,AA,AB,AC,AD,AE,AF,AG,AH indexOps;
```

#### Update Operations
```mermaid
graph TD
    A[Start] --> B[Parse Update Node]
    B --> C[Extract Components]
    C --> D[Validate Table Exists]
    D --> E{Table Empty?}
    E -->|Yes| F[Return: No Rows]
    E -->|No| G[Evaluate Where Clause]
    G --> H[Get Filtered Keys]
    H --> I{Keys Found?}
    I -->|No| J[Return: Not Found]
    I -->|Yes| K[Invalidate Cache]
    K --> L[Loop Through Rows]
    L --> M[For Each Column Update]
    M --> N[Remove Old Index]
    N --> O[Insert New Index]
    O --> P[Update Main Tree]
    P --> Q[Return Updated Count]
```

### Performance Characteristics

#### Time Complexity

| Operation            | Average Case | Worst Case   | Notes                   |
|----------------------|--------------|--------------|-------------------------|
| Search (Point Query) | O(log n)     | O(log n)     | Using appropriate index |
| Insert               | O(log n)     | O(log n)     | Updates all indexes     |
| Delete               | O(log n)     | O(log n)     | Updates all indexes     |
| Range Query          | O(log n + m) | O(log n + m) | m = number of results   |
| Update               | O(log n)     | O(log n)     | Per index update        |
| Index Creation       | O(n log n)   | O(n log n)   | Full table scan         |

> n = number of records in table

#### Space Complexity

| Component            | Space Usage                                |
|----------------------|--------------------------------------------|
| Main Tree            | O(n) where n = number of records           |
| Each Index Tree      | O(n) per indexed column                    |
| Total with k indexes | O(n * (1 + k)) where k = number of indexes |

### Caching System

- LRU cache implementation for SELECT queries
- Cache invalidation on data modifications

# Design Decision Highlights

### Optimizing B+ Tree Order

In order to identify the best order for the B+ tree, we conducted a series of benchmarks to
evaluate.

#### Experiment Setup

- Tested orders: 16, 32, 64, 128
- Dataset sizes: 1K, 10K, 100K, 1M records
- Measured metrics:
    - Insertion time
    - Search time
- Each test repeated multiple times to ensure consistent results

#### Benchmark Results

![img.png](src/BPlusTree_OrderAnalysis_Results.png)

- Smaller datasets (1K) perform well with order 16
- Medium datasets (10K) benefit from larger order (64)
- Large datasets (100K-1M) perform best with order 32
- Very large orders (128+) showed diminishing returns

Based on these findings, we decided to use a default order of 32 as it provides consistent
performance across different dataset sizes

### Optimizing Result Combination (combineResults)

#### Original Implementation Issues

![alt text](/BPlusTree/src/main/java/edu/smu/smusql/bplustreeA/src/combineResults/combineResults_before.png)

```java
private List<Integer> combineResults(List<Integer> leftResult, List<Integer> rightResult,
    String operator) {
    if (operator.equals("AND")) {
        leftResult.retainAll(rightResult);
        return leftResult;
    } else if (operator.equals("OR")) {
        leftResult.addAll(rightResult);
        return leftResult.stream().distinct().collect(Collectors.toList());
    } else {
        throw new RuntimeException("Unsupported logical operator: " + operator);
    }
}
```

- **AND Operations**: Uses `retainAll()` which has `O(n * m)` complexity
- **OR Operations**: Uses streams with `distinct()` which has `O(n log n)` complexity
- Very inefficient for large lists
- Performance bottleneck: Taking longer than tree traversal

#### Optimized Implementation

![alt text](/SmuSQL/src/main/java/edu/smu/smusql/bplustreeA/src/combineResults/combineResults_after.png)

Improvements:

- **AND Operations**: Use the smaller list as the base hashset to reduce comparisons
- **OR Operations**: Deduplicate results with HashSet
- Utilized HashSet for O(1) lookups
- Reduced AND complexity to `O(n + m)`

### Optimizing Row Retrieval (retrieveFilteredRows)

#### Original Implementation Issues

![alt text](/SmuSQL/src/main/java/edu/smu/smusql/bplustreeA/src/retrieveFilteredRows/retrieveFilteredRows_before.png)

```java
private Map<Integer, Map<String, Object>> retrieveFilteredRows(List<Integer> filteredKeys,
    BPlusTree<Integer, Map<String, Object>> rows) {

    // Group keys into ranges
    List<Range<Integer>> ranges = groupKeysIntoRanges(filteredKeys);

    // Retrieve filtered Rows
    Map<Integer, Map<String, Object>> filteredRows = new HashMap<>();

    for (Range<Integer> range : ranges) {
        List<Map<String, Object>> rangeRows = rows.rangeSearch(range.getStart(),
            range.getEnd());
        for (int i = 0; i < rangeRows.size(); i++) {
            Integer key = filteredKeys.get(filteredKeys.indexOf(range.getStart()) + i);
            filteredRows.put(key, rangeRows.get(i));
        }
    }
    return filteredRows;
}
```

- Expensive `indexOf()` operations
- `O(n²)` time complexity
- Inefficient position tracking

#### Optimized Implementation

![alt text](/SmuSQL/src/main/java/edu/smu/smusql/bplustreeA/src/retrieveFilteredRows/retrieveFilteredRows_before.png)

Key Improvements:

- Eliminated repeated `indexOf()` calls
- Added HashMap for `O(1)` position lookups
- Reduced complexity to `O(n log n)`
- Maintained running index counter

### Expensive String Formatting (formatSelectResults)

#### Original Benchmark

![alt text](/SmuSQL/src/main/java/edu/smu/smusql/bplustreeA/src/benchmarks/benchmark3.png)

#### Optimized Benchmark

![alt text](/SmuSQL/src/main/java/edu/smu/smusql/bplustreeA/src/benchmarks/benchmark4.png)

Implemented a LRU cache for expensive SELECT operations:

- Cache Hits: O(1) lookup
- Cache Misses: Original cost + O(1) insertion
- Eliminates:
    - Row retrieval operations
    - String formatting overhead
    - StringBuilder operations
    - Complex condition evaluations
