# B+ Tree Architecture and Design Decisions

## Design Decisions and Tradeoffs

### 1. Dual-Tree Architecture

The engine uses two types of B+ trees:

- **Main Tree**: `primaryKey -> rowData (HashMap)`
- **Index Trees**: `columnValue -> List<primaryKey>`

#### Why This Approach?

```plaintext
Advantages:
- Efficient point queries on any indexed column (O(log n))
- Fast range queries without full table scans
- Memory-efficient storage of row data (stored once in main tree)
- Natural support for non-unique indexes
- Simplified update operations (main data in one place)

Tradeoffs:
- Additional memory overhead for index trees
- Write operations need to update multiple trees
- Potential for inconsistency between main and index trees
- Index maintenance overhead during updates/deletes
```

### 2. Index Structure Choices

```plaintext
Alternative Approaches Considered:
1. Hash-based Indexes
   + O(1) point lookups
   - No range query support
   - More memory overhead
   
2. Single Tree with Duplicate Keys
   + Simpler implementation
   + Less memory overhead
   - Complex update logic
   - Duplicate data storage
```

## Performance Characteristics

### 1. Time Complexity

| Operation            | Average Case | Worst Case   | Notes                   |
|----------------------|--------------|--------------|-------------------------|
| Search (Point Query) | O(log n)     | O(log n)     | Using appropriate index |
| Insert               | O(log n)     | O(log n)     | Updates all indexes     |
| Delete               | O(log n)     | O(log n)     | Updates all indexes     |
| Range Query          | O(log n + m) | O(log n + m) | m = number of results   |
| Update               | O(log n)     | O(log n)     | Per index update        |
| Index Creation       | O(n log n)   | O(n log n)   | Full table scan         |

> n = number of records in table

### 2. Space Complexity

| Component            | Space Usage                                |
|----------------------|--------------------------------------------|
| Main Tree            | O(n) where n = number of records           |
| Each Index Tree      | O(n) per indexed column                    |
| Total with k indexes | O(n * (1 + k)) where k = number of indexes |

```plaintext
Memory Overhead Breakdown:
1. Main Tree Node: 
   - Keys: 8 bytes per primary key (long)
   - Values: ~32 bytes per HashMap overhead + field storage
   - Node Pointers: 8 bytes per child reference

2. Index Tree Node:
   - Keys: Variable (depends on indexed column type)
   - Values: 8 bytes per primary key reference
   - Node Pointers: 8 bytes per child reference

3. Additional Overheads:
   - B+ Tree Order: Affects node capacity and tree height
   - Index Fan-out: Impacts tree height and query performance
```

### 3. Optimization Techniques Implemented

#### a. Query Processing Optimizations

```plaintext
1. Efficient Multi-Key Lookup
   - Batch processing of multiple key searches
   - Reduces tree traversals
   - Optimized leaf node traversal

2. Smart Index Selection
   - Automatic selection of most selective index
   - Avoids unnecessary index scans
   - Prioritizes equality conditions over ranges

3. Query Result Caching
   - LRU cache for frequently accessed queries
   - Cache invalidation on table updates
   - Configurable cache size
```

#### b. Memory Efficiency Techniques

```plaintext
1. Shared Primary Key References
   - Index trees store only key references
   - Actual data stored once in main tree

2. Node Utilization
   - Minimum 50% node utilization maintained
   - Reduces space waste and improves cache efficiency

3. Lazy Index Updates
   - Could be implemented to batch index updates
   - Tradeoff: Consistency vs Performance
```

### 4. Performance Tuning Parameters

| Parameter       | Impact                          | Recommended Values                                                                                            |
|-----------------|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| B+ Tree Order   | Node size, tree height          | 32                                                                                                            |
| Cache Size      | Memory usage, query performance | 10-20% of dataset                                                                                             |
| Index Selection | Query performance, memory usage | Index columns with:      &nbsp;<br>- High selectivity&nbsp;<br>- Frequent queries&nbsp;<br>- Range operations |
