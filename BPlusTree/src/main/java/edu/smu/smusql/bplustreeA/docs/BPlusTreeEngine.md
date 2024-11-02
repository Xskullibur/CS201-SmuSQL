# BPlusTreeEngine Class Documentation

## Overview
`BPlusTreeEngine` implements an in-memory SQL engine using B+ trees for data storage and indexing. It maintains a main tree for primary key-based storage and separate index trees for each column to enable efficient querying.

## Class Structure

```java
public class BPlusTreeEngine implements IEngine {
    private Map<String, BPlusTreeTableHashMap> database;
    private Map<String, BPlusTree<Number, Integer>> indexDatabase;
    private final LRUCache<CacheQueryKey, String> queryCache;
}
```

### Core Components

1. **Storage Components**
    - `database`: Main storage for tables
    - `indexDatabase`: Secondary indexes
    - `queryCache`: Cache for query results

2. **Cache Metrics**
   ```java
   private long cacheHits;
   private long cacheMisses;
   ```

## SQL Operations

### 1. CREATE TABLE
```java
public String create(CreateTableNode node)
```
Process:
1. Creates main table structure
2. Initializes index trees for each column
3. Validates table doesn't exist

Example:
```sql
CREATE TABLE users (name STRING, age INTEGER)
```

### 2. INSERT
```java
public String insert(InsertNode node)
```
Process:
1. Validates primary key uniqueness
2. Updates main table
3. Updates all index trees
4. Invalidates relevant cache entries

Example:
```sql
INSERT INTO users VALUES (1, "John", 25)
```

### 3. SELECT
```java
public String select(SelectNode node)
```
Features:
- Cache utilization
- Condition evaluation
- Index-based filtering
- Result formatting

Example:
```sql
SELECT * FROM users WHERE age > 25
```

### 4. UPDATE
```java
public String update(UpdateNode node)
```
Process:
1. Locates affected rows
2. Updates main table
3. Updates relevant indexes
4. Invalidates cache

Example:
```sql
UPDATE users SET age = 26 WHERE id = 1
```

### 5. DELETE
```java
public String delete(DeleteNode node)
```
Process:
1. Identifies rows to delete
2. Removes from main table
3. Updates indexes
4. Cleans up cache

Example:
```sql
DELETE FROM users WHERE age < 25
```

## Query Processing

### 1. Condition Evaluation
```java
private List<Integer> evaluateConditionNode(String tableName, ConditionNode node)
```
Handles:
- Complex conditions (AND, OR)
- Comparison operators
- Index utilization

### 2. Index Utilization
```java
private List<Integer> evaluateSimpleCondition(String tableName, ConditionNode node)
```
- Determines optimal index usage
- Processes different data types
- Handles range conditions

### 3. Result Processing
```java
private String formatSelectResults(Map<Integer, Map<String, Object>> rows, List<String> columns)
```
- Formats query results
- Handles column selection
- Maintains consistency

## Caching Mechanism

### Implementation
```java
private final LRUCache<CacheQueryKey, String> queryCache;
```

### Operations
1. **Cache Check**
   ```java
   String cachedResult = queryCache.get(queryKey);
   ```

2. **Cache Update**
   ```java
   queryCache.put(queryKey, result);
   ```

3. **Cache Invalidation**
   ```java
   private void invalidateCacheForTable(String tableName)
   ```

### Metrics
```java
public double getCacheHitRate()
public void clearCacheMetrics()
```

## Performance Optimization

### 1. Query Optimization
- Index selection
- Condition evaluation order
- Cache utilization

### 2. Batch Operations
```java
public Map<K, V> multiKeySearch(List<K> keys)
```
- Optimized multi-key retrieval
- Batch updates

### 3. Memory Management
- Cache size control
- Index maintenance
- Resource cleanup

## Usage Example

```java
BPlusTreeEngine engine = new BPlusTreeEngine();

// Create table
engine.executeSQL("CREATE TABLE users (name STRING, age INTEGER)");

// Insert data
engine.executeSQL("INSERT INTO users VALUES (1, 'John', 25)");

// Query data
String result = engine.executeSQL("SELECT * FROM users WHERE age > 20");

// Update data
engine.executeSQL("UPDATE users SET age = 26 WHERE id = 1");

// Delete data
engine.executeSQL("DELETE FROM users WHERE id = 1");
```