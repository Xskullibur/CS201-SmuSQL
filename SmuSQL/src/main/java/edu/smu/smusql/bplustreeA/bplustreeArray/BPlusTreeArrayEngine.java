package edu.smu.smusql.bplustreeA.bplustreeArray;

import edu.smu.smusql.Constants;
import edu.smu.smusql.IEngine;
import edu.smu.smusql.bplustreeA.BPlusTree;
import edu.smu.smusql.bplustreeA.lruCache.LRUCache;
import edu.smu.smusql.bplustreeA.lruCache.CacheQueryKey;
import edu.smu.smusql.bplustreeA.AstParser.AstParser;
import edu.smu.smusql.bplustreeA.AstParser.Token;
import edu.smu.smusql.bplustreeA.AstParser.Tokenizer;
import edu.smu.smusql.bplustreeA.AstParser.nodes.ASTNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.AssignmentNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.ColumnNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.ConditionNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.CreateTableNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.DeleteNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.ExpressionNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.InsertNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.LiteralNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.SelectNode;
import edu.smu.smusql.bplustreeA.AstParser.nodes.UpdateNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BPlusTreeArrayEngine implements IEngine {

    private final LRUCache<CacheQueryKey, String> queryCache; // Only implemented for "SELECT *" statements
    private final boolean useCaching;
    private Map<String, BPlusTreeTableArray> database;
    private Map<String, BPlusTree<Number, Integer>> indexDatabase;
    private long cacheHits = 0;
    private long cacheMisses = 0;

    public BPlusTreeArrayEngine() {
        this(true);
    }

    public BPlusTreeArrayEngine(boolean useCaching) {
        this.database = new HashMap<>();
        this.indexDatabase = new HashMap<>();
        this.useCaching = useCaching;
        this.queryCache = useCaching ? new LRUCache<>(Constants.CACHE_SIZE) : null;
    }

    private static String buildHeaderString(List<String> columns) {
        StringBuilder header = new StringBuilder("id");
        for (String column : columns) {
            header.append('\t').append(column);
        }
        header.append('\n');
        return header.toString();
    }

    private <T> T retrieveTable(Map<String, T> database, String tableName) {

        if (!database.containsKey(tableName)) {
            throw new RuntimeException("ERROR: Table " + tableName + " does not exist");
        }

        return database.get(tableName);
    }

    private List<Integer> filterIndexes(String tableName, ConditionNode node) {

        if (node == null) {
            return null;
        }

        return evaluateConditionNode(tableName, node);
    }

    private List<Integer> evaluateConditionNode(String tableName, ConditionNode node) {

        if (node.getLeft() instanceof ConditionNode && node.getRight() instanceof ConditionNode) {

            // Both left and right are ConditionNodes
            List<Integer> leftResult = evaluateConditionNode(tableName,
                (ConditionNode) node.getLeft());
            List<Integer> rightResult = evaluateConditionNode(tableName,
                (ConditionNode) node.getRight());
            return combineResults(leftResult, rightResult, node.getOperator());

        } else if (node.getLeft() instanceof ExpressionNode
            && node.getRight() instanceof ExpressionNode) {
            // Both left and right are ExpressionNodes
            return evaluateSimpleCondition(tableName, node);
        } else {
            throw new RuntimeException("Unsupported condition node structure");
        }
    }

    private List<Integer> evaluateSimpleCondition(String tableName, ConditionNode node) {

        ExpressionNode left = (ExpressionNode) node.getLeft();
        ExpressionNode right = (ExpressionNode) node.getRight();
        String operator = node.getOperator();

        if (!(left instanceof ColumnNode) || !(right instanceof LiteralNode)) {
            throw new RuntimeException("Unsupported simple condition structure");
        }

        String columnName = ((ColumnNode) left).getName();
        LiteralNode literalNode = (LiteralNode) right;
        Object value = getValueFromLiteralNode(literalNode);

        // Check if we're searching by primary key
        if (columnName.equals("id")) {
            Integer intValue = literalNode.getType() == LiteralNode.LiteralNodeType.NUMBER
                ? literalNode.getIntegerValue()
                : literalNode.getValue().hashCode();

            // For primary key searches, return a single-element list
            if (operator.equals("=")) {
                return Collections.singletonList(intValue);
            }

            // For other operators, get all keys and filter
            BPlusTreeTableArray table = retrieveTable(database, tableName);
            BPlusTree<Integer, Object[]> mainTree = table.getRows();
            List<Integer> allKeys = mainTree.getAllKeys();

            return filterKeysByOperator(allKeys, intValue, operator);
        }

        String indexTableName = Constants.getIndexTableName(tableName, columnName);
        BPlusTree<Number, Integer> indexTree = retrieveTable(indexDatabase, indexTableName);

        // Convert value to appropriate type for comparison
        Number searchValue;
        if (value instanceof String) {
            searchValue = value.hashCode();
        } else if (value instanceof Number) {
            searchValue = (Number) value;
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }

        return evaluateCondition(indexTree, searchValue, operator);
    }

    private Object getValueFromLiteralNode(LiteralNode literalNode) {
        switch (literalNode.getType()) {
            case STRING:
                return literalNode.getStringValue();
            case NUMBER:
                return literalNode.getIntegerValue();
            case FLOAT:
                return literalNode.getFloatValue();
            default:
                throw new IllegalStateException(
                    "Unexpected LiteralNodeType: " + literalNode.getType());
        }
    }

    private Number convertToNumber(Object obj) {
        if (obj instanceof String) {
            return obj.hashCode();
        } else if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Float) {
            return (Float) obj;
        } else {
            throw new IllegalArgumentException("Received an unsupported object: " + obj.toString());
        }
    }

    private List<Integer> filterKeysByOperator(List<Integer> keys, Integer value, String operator) {
        return keys.stream().filter(key -> {
            switch (operator) {
                case "!=":
                    return !key.equals(value);
                case "<":
                    return key < value;
                case "<=":
                    return key <= value;
                case ">":
                    return key > value;
                case ">=":
                    return key >= value;
                default:
                    return false;
            }
        }).collect(Collectors.toList());
    }

    private List<Integer> evaluateCondition(BPlusTree<Number, Integer> indexTree, Number value,
        String operator) {

        // If the index tree is empty, return an empty list
        if (indexTree.getSize() == 0) {
            return new ArrayList<>();
        }

        switch (operator) {
            case "=":
                return indexTree.search(value);

            case "!=":
                // Get all values
                List<Integer> allValues = indexTree.getAllValues();
                if (allValues.isEmpty()) {
                    return allValues;
                }

                // Remove matching values using a Set
                Set<Integer> equalValues = new HashSet<>(
                    indexTree.search(value) != null ? indexTree.search(value)
                        : Collections.emptyList());

                return allValues.stream().filter(v -> !equalValues.contains(v))
                    .collect(Collectors.toList());

            case "<":
                List<Integer> lessThanResult = indexTree.rangeSearch(Double.NEGATIVE_INFINITY,
                    value.doubleValue());
                if (lessThanResult == null || lessThanResult.isEmpty()) {
                    return new ArrayList<>();
                }

                // Remove boundary values using Set
                Set<Integer> boundaryValues = new HashSet<>(
                    indexTree.search(value) != null ? indexTree.search(value)
                        : Collections.emptyList());
                return lessThanResult.stream().filter(v -> !boundaryValues.contains(v))
                    .collect(Collectors.toList());

            case "<=":
                return indexTree.rangeSearch(Double.NEGATIVE_INFINITY, value.doubleValue());

            case ">":
                List<Integer> greaterThanResult = indexTree.rangeSearch(value.doubleValue(),
                    Double.POSITIVE_INFINITY);
                if (greaterThanResult == null || greaterThanResult.isEmpty()) {
                    return new ArrayList<>();
                }

                // Remove boundary values using Set
                Set<Integer> upperBoundaryValues = new HashSet<>(
                    indexTree.search(value) != null ? indexTree.search(value)
                        : Collections.emptyList());
                return greaterThanResult.stream().filter(v -> !upperBoundaryValues.contains(v))
                    .collect(Collectors.toList());

            case ">=":
                return indexTree.rangeSearch(value.doubleValue(), Double.POSITIVE_INFINITY);

            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private List<Integer> combineResults(List<Integer> leftResult, List<Integer> rightResult,
        String operator) {

        if (leftResult == null || leftResult.isEmpty()) {
            return rightResult != null ? new ArrayList<>(rightResult) : new ArrayList<>();
        }
        if (rightResult == null || rightResult.isEmpty()) {
            return new ArrayList<>(leftResult);
        }

        if (operator.equals("AND")) {
            // Use HashSet for better performance with large datasets
            if (leftResult.size() > rightResult.size()) {
                // Swap to ensure we use the smaller list for the HashSet
                List<Integer> temp = leftResult;
                leftResult = rightResult;
                rightResult = temp;
            }

            // Create HashSet from the smaller list
            Set<Integer> rightSet = new HashSet<>(rightResult);
            List<Integer> result = new ArrayList<>();

            // Iterate through the smaller list
            for (Integer value : leftResult) {
                if (rightSet.contains(value)) {
                    result.add(value);
                }
            }
            return result;
        } else if (operator.equals("OR")) {
            // For OR operations, use HashSet for deduplication
            Set<Integer> uniqueResults = new HashSet<>(leftResult);
            uniqueResults.addAll(rightResult);
            List<Integer> result = new ArrayList<>(uniqueResults);
            Collections.sort(result); // Maintain sorted order
            return result;
        } else {
            throw new RuntimeException("Unsupported logical operator: " + operator);
        }
    }

    private String formatSelectResults(Map<Integer, Object[]> rows, List<String> columns) {
        String header = buildHeaderString(columns);

        if (rows == null || rows.isEmpty()) {
            return header;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(header);

        // Rows data
        for (Map.Entry<Integer, Object[]> entry : rows.entrySet()) {
            Integer id = entry.getKey();
            Object[] row = entry.getValue();

            if (row == null) {
                continue;
            }

            sb.append(id).append("\t");

            for (int i = 0; i < columns.size(); i++) {
                Object value = row[i];
                sb.append(value != null ? value.toString() : "NULL").append('\t');
            }
            sb.append('\n');
        }

        return sb.toString().trim();
    }

    public String executeSQL(String query) {

        List<Token> tokens = Tokenizer.tokenize(query);
        AstParser parser = new AstParser(tokens);
        ASTNode statement = parser.parse();

        if (statement instanceof InsertNode) {
            return insert((InsertNode) statement);
        } else if (statement instanceof DeleteNode) {
            return delete((DeleteNode) statement);
        } else if (statement instanceof SelectNode) {
            return select((SelectNode) statement);
        } else if (statement instanceof UpdateNode) {
            return update((UpdateNode) statement);
        } else if (statement instanceof CreateTableNode) {
            return create((CreateTableNode) statement);
        } else {
            return "Unsupported SQL statement";
        }
    }

    /**
     * Clears all data from the database and index database. Used primarily for
     * testing purposes.
     */
    public void clearDatabase() {
        // Clear both main database and index database
        database.clear();
        indexDatabase.clear();
    }

    public String create(CreateTableNode node) {

        String tableName = node.getTableName();
        List<String> columns = node.getColumns();

        // Add table into database
        if (database.containsKey(tableName)) {
            throw new RuntimeException("Table " + tableName + " already exist");
        }

        BPlusTreeTableArray table = new BPlusTreeTableArray(columns);
        database.put(tableName, table);

        /**
         * Create Indexing Trees with column fields as Key and PrimaryKey as Value
         */
        for (String col : columns) {
            BPlusTree<Number, Integer> indexTree = new BPlusTree<>(Constants.B_PLUS_TREE_ORDER);
            String indexTableName = Constants.getIndexTableName(tableName, col);
            indexDatabase.put(indexTableName, indexTree);

            if (Constants.LOGGING) {
                System.out.println("Indexing Table " + indexTableName + " created successfully");
            }

        }

        return "Table " + tableName + " created successfully";
    }

    /**
     * Inserts a new row into the specified table.
     *
     * @param node The InsertNode containing the table name and values to be
     *             inserted.
     * @return A message indicating the result of the insertion operation. Returns
     *         "1 row inserted
     *         successfully" if the insertion is successful. Returns an error
     *         message if the specified table
     *         does not exist.
     */
    public String insert(InsertNode node) {
        String tableName = node.getTableName();
        Integer primaryKey = node.getPrimaryKey().getIntegerValue();
        List<LiteralNode> values = node.getValues().stream().map(LiteralNode.class::cast).toList();

        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();
        List<String> columns = table.getColumns();

        if (values.size() != columns.size()) {
            throw new RuntimeException("ERROR: Column count does not match value count");
        }

        if (rows.search(primaryKey) != null) {
            return "0 row inserted, primary key already exists";
        }

        invalidateCacheForTable(node.getTableName());

        // Create array for row data
        Object[] rowData = new Object[table.getColumnCount()];

        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            String indexTableName = Constants.getIndexTableName(tableName, column);
            LiteralNode literalNode = values.get(i);

            Object value = getValueFromLiteralNode(literalNode);
            rowData[i] = value;

            BPlusTree<Number, Integer> tree = indexDatabase.get(indexTableName);
            tree.insert(convertToNumber(value), primaryKey);
        }

        rows.insert(primaryKey, rowData);
        return "1 row inserted successfully";
    }

    private void invalidateCacheForTable(String tableName) {

        if (useCaching) {
            queryCache.entrySet().removeIf(entry -> entry.getKey().tableName.equals(tableName));
        }
    }

    private Map<Integer, Object[]> retrieveFilteredRows(List<Integer> filteredKeys,
        BPlusTree<Integer, Object[]> rows) {

        if (filteredKeys == null || filteredKeys.isEmpty()) {
            return new HashMap<>();
        }

        // Pre-sort filtered keys if not already sorted
        Collections.sort(filteredKeys);

        // Use the optimized multi-key search
        return rows.multiKeySearch(filteredKeys);
    }

    public String select(SelectNode node) {

        // Retrieve query information
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();
        CacheQueryKey queryKey = null;

        // Check if caching is enabled
        if (useCaching) {

            // Create cache key
            queryKey = new CacheQueryKey(node.getTableName(), node.getWhereClause(),
                node.getColumns());

            // Check cache first
            String cachedResult = queryCache.get(queryKey);

            if (cachedResult != null) {
                recordCacheHit();
                return cachedResult;
            }

            recordCacheMiss();
        }

        // If not in cache, execute query
        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();
        List<String> columns = table.getColumns();

        String result;

        // Handle SELECT * Query
        if (whereClause == null && Objects.equals(node.getColumns().get(0), "*")) {
            result = formatSelectResults(rows.getAllKeyValues(), columns);

            if (useCaching) {
                queryCache.put(queryKey, result);
            }

            return result;
        }

        // Get primary keys based on whereClause
        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        // Get rows using filteredKeys
        Map<Integer, Object[]> fitleredRows = retrieveFilteredRows(filteredKeys, rows);

        // Format results
        result = formatSelectResults(fitleredRows, columns);
        return result;
    }

    public String delete(DeleteNode node) {
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();

        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();

        if (rows.getSize() == 0) {
            return "0 row(s) deleted, no rows found";
        }

        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        Map<Integer, Object[]> filteredRows = retrieveFilteredRows(filteredKeys, rows);

        if (filteredRows.isEmpty()) {
            return "0 row(s) deleted, not found";
        }

        invalidateCacheForTable(node.getTableName());

        for (Map.Entry<Integer, Object[]> row : filteredRows.entrySet()) {
            Integer rowKey = row.getKey();
            Object[] rowData = row.getValue();

            for (int i = 0; i < table.getColumnCount(); i++) {
                String columnName = table.getColumns().get(i);
                Object columnValue = rowData[i];

                String indexTableName = Constants.getIndexTableName(tableName, columnName);
                removeIndexEntry(indexTableName, columnValue, rowKey);
            }
        }

        for (Integer key : filteredKeys) {
            rows.removeKey(key);
        }

        return filteredKeys.size() + " row(s) deleted successfully";
    }

    private void removeIndexEntry(String indexTableName, Object value, Integer key) {
        if (value instanceof String) {
            indexDatabase.get(indexTableName).removeValue(((String) value).hashCode(), key);
        } else if (value instanceof Integer) {
            indexDatabase.get(indexTableName).removeValue((Integer) value, key);
        } else if (value instanceof Float) {
            indexDatabase.get(indexTableName).removeValue((Float) value, key);
        } else {
            throw new IllegalStateException(
                "Unexpected value type for removal: " + value.getClass());
        }
    }

    public String update(UpdateNode node) {
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();
        List<AssignmentNode> assignments = node.getAssignments();

        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();
        List<String> columns = table.getColumns();

        if (rows.getSize() == 0) {
            return "0 row(s) updated, no rows found";
        }

        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        Map<Integer, Object[]> filteredRows = retrieveFilteredRows(filteredKeys, rows);

        if (filteredRows.isEmpty()) {
            return "0 row(s) updated, not found";
        }

        invalidateCacheForTable(node.getTableName());

        for (Map.Entry<Integer, Object[]> row : filteredRows.entrySet()) {
            Integer primaryKey = row.getKey();
            Object[] rowData = row.getValue();
            Object[] updatedRowData = rowData.clone();

            for (AssignmentNode assignment : assignments) {
                String columnName = assignment.getColumn();
                int columnIndex = table.getColumnIndex(columnName);
                LiteralNode newValueNode = (LiteralNode) assignment.getValue();
                Object newValue = getValueFromLiteralNode(newValueNode);
                Object oldValue = rowData[columnIndex];

                String indexTableName = Constants.getIndexTableName(tableName, columnName);
                BPlusTree<Number, Integer> indexTree = indexDatabase.get(indexTableName);

                indexTree.removeValue(convertToNumber(oldValue), primaryKey);
                indexTree.insert(convertToNumber(newValue), primaryKey);

                updatedRowData[columnIndex] = newValue;
            }

            rows.update(primaryKey, updatedRowData);
        }

        return filteredKeys.size() + " row(s) updated successfully";
    }

    public void recordCacheHit() {
        cacheHits++;
    }

    public void recordCacheMiss() {
        cacheMisses++;
    }

    public double getCacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0 ? 0.0 : (double) cacheHits / total;
    }

    public void clearCacheMetrics() {
        cacheHits = 0;
        cacheMisses = 0;
    }

    public boolean isCachingEnabled() {
        return useCaching;
    }

}