package edu.smu.smusql.bplustreeA;

import edu.smu.smusql.Constants;
import edu.smu.smusql.IEngine;
import edu.smu.smusql.bplustreeA.helper.Range;
import edu.smu.smusql.parser.AstParser;
import edu.smu.smusql.parser.Token;
import edu.smu.smusql.parser.Tokenizer;
import edu.smu.smusql.parser.nodes.ASTNode;
import edu.smu.smusql.parser.nodes.AssignmentNode;
import edu.smu.smusql.parser.nodes.ColumnNode;
import edu.smu.smusql.parser.nodes.ConditionNode;
import edu.smu.smusql.parser.nodes.CreateTableNode;
import edu.smu.smusql.parser.nodes.DeleteNode;
import edu.smu.smusql.parser.nodes.ExpressionNode;
import edu.smu.smusql.parser.nodes.InsertNode;
import edu.smu.smusql.parser.nodes.LiteralNode;
import edu.smu.smusql.parser.nodes.SelectNode;
import edu.smu.smusql.parser.nodes.UpdateNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BPlusTreeEngineArray implements IEngine {

    private Map<String, BPlusTreeTableArray> database;
    private Map<String, BPlusTree<Number, Integer>> indexDatabase;

    public BPlusTreeEngineArray() {
        this.database = new HashMap<>();
        indexDatabase = new HashMap<>();
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

        // Evaluate the condition node
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

    private List<Range<Integer>> groupKeysIntoRanges(List<Integer> keys) {

        List<Range<Integer>> ranges = new ArrayList<>();

        if (keys.isEmpty()) {
            return ranges;
        }

        Collections.sort(keys);
        Integer start = keys.get(0);
        Integer end = start;

        for (int i = 1; i < keys.size(); i++) {
            Integer current = keys.get(i);
            if (current.equals(end + 1)) {
                end = current;
            } else {
                ranges.add(new Range<>(start, end));
                start = current;
                end = start;
            }
        }
        ranges.add(new Range<>(start, end));

        return ranges;
    }

    private String formatSelectResults(Map<Integer, Object[]> rows, List<String> columns) {
        if (rows == null || rows.isEmpty()) {
            return "id\t" + String.join("\t", columns);
        }

        // Pre-calculate buffer size
        int estimatedRowSize = (columns.size() + 1) * 12; // Average 12 chars per cell including tabs
        int totalSize = (rows.size() + 1) * estimatedRowSize; // +1 for header
        StringBuilder sb = new StringBuilder(totalSize);

        // Append header
        sb.append("id");
        for (String column : columns) {
            sb.append('\t').append(column);
        }
        sb.append('\n');

        // Append rows - using direct array access
        for (Map.Entry<Integer, Object[]> entry : rows.entrySet()) {
            Object[] row = entry.getValue();
            if (row == null) {
                continue;
            }

            sb.append(entry.getKey()); // Append ID

            // Direct array access
            for (Object value : row) {
                sb.append('\t');
                if (value != null) {
                    if (value instanceof String) {
                        sb.append((String) value);
                    } else if (value instanceof Number) {
                        sb.append(value.toString());
                    } else {
                        sb.append(value);
                    }
                } else {
                    sb.append("NULL");
                }
            }
            sb.append('\n');
        }

        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
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
     * Clears all data from the database and index database. Used primarily for testing purposes.
     */
    public void clearDatabase() {
        // Clear both main database and index database
        database.clear();
        indexDatabase.clear();
    }

    /**
     * Clears all data from a specific table and its indexes. Used primarily for testing purposes.
     *
     * @param tableName the name of the table to clear
     * @return String indicating the cleanup was successful
     * @throws RuntimeException if the table doesn't exist
     */
    public String clearTable(String tableName) {
        // Check if table exists
        if (!database.containsKey(tableName)) {
            throw new RuntimeException("ERROR: Table " + tableName + " does not exist");
        }

        // Get table columns to identify indexes to clear
        BPlusTreeTableArray table = database.get(tableName);
        List<String> columns = table.getColumns();

        // Clear all index trees for this table
        for (String column : columns) {
            String indexTableName = Constants.getIndexTableName(tableName, column);
            indexDatabase.remove(indexTableName);

            // Recreate empty index tree
            BPlusTree<Number, Integer> indexTree = new BPlusTree<>(Constants.B_PLUS_TREE_ORDER);
            indexDatabase.put(indexTableName, indexTree);
        }

        // Clear and recreate main table
        BPlusTreeTableArray newTable = new BPlusTreeTableArray(columns);
        database.put(tableName, newTable);

        return "Table " + tableName + " cleared successfully";
    }

    public String create(CreateTableNode node) {
        String tableName = node.getTableName();
        List<String> columns = node.getColumns();

        if (database.containsKey(tableName)) {
            throw new RuntimeException("Table " + tableName + " already exist");
        }

        BPlusTreeTableArray table = new BPlusTreeTableArray(columns);
        database.put(tableName, table);

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
     * @param node The InsertNode containing the table name and values to be inserted.
     * @return A message indicating the result of the insertion operation. Returns "1 row inserted
     * successfully" if the insertion is successful. Returns an error message if the specified table
     * does not exist.
     */
    public String insert(InsertNode node) {
        String tableName = node.getTableName();
        Integer primaryKey = node.getPrimaryKey().getIntegerValue();
        List<LiteralNode> values = node.getValues().stream()
            .map(LiteralNode.class::cast)
            .toList();

        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();
        List<String> columns = table.getColumns();

        if (values.size() != columns.size()) {
            throw new RuntimeException("ERROR: Column count does not match value count");
        }

        if (rows.search(primaryKey) != null) {
            return "0 row inserted, primary key already exists";
        }

        // Create array instead of HashMap
        Object[] rowData = new Object[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            LiteralNode literalNode = values.get(i);
            Object value = getValueFromLiteralNode(literalNode);

            // Store in array
            rowData[i] = value;

            // Update index
            String indexTableName = Constants.getIndexTableName(tableName, column);
            BPlusTree<Number, Integer> tree = indexDatabase.get(indexTableName);
            tree.insert(convertToNumber(value), primaryKey);
        }

        rows.insert(primaryKey, rowData);
        return "1 row inserted successfully";
    }

    private Map<Integer, Object[]> retrieveFilteredRows(List<Integer> filteredKeys,
        BPlusTree<Integer, Object[]> rows) {
        List<Range<Integer>> ranges = groupKeysIntoRanges(filteredKeys);
        Map<Integer, Object[]> filteredRows = new HashMap<>();

        for (Range<Integer> range : ranges) {
            List<Object[]> rangeRows = rows.rangeSearch(range.getStart(), range.getEnd());
            for (int i = 0; i < rangeRows.size(); i++) {
                Integer key = filteredKeys.get(filteredKeys.indexOf(range.getStart()) + i);
                filteredRows.put(key, rangeRows.get(i));
            }
        }
        return filteredRows;
    }

    public String select(SelectNode node) {
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();

        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();
        List<String> columns = table.getColumns();

        if (whereClause == null && Objects.equals(node.getColumns().get(0), "*")) {
            return formatSelectResults(rows.getAllKeyValues(), columns);
        }

        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        Map<Integer, Object[]> filteredRows = retrieveFilteredRows(filteredKeys, rows);

        return formatSelectResults(filteredRows, columns);
    }

    public String delete(DeleteNode node) {
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();

        BPlusTreeTableArray table = retrieveTable(database, tableName);
        BPlusTree<Integer, Object[]> rows = table.getRows();
        List<String> columns = table.getColumns();

        if (rows.getSize() == 0) {
            return "0 row(s) deleted, no rows found";
        }

        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        Map<Integer, Object[]> filteredRows = retrieveFilteredRows(filteredKeys, rows);

        if (filteredRows.isEmpty()) {
            return "0 row(s) deleted, not found";
        }

        for (Map.Entry<Integer, Object[]> entry : filteredRows.entrySet()) {
            Integer rowValue = entry.getKey();
            Object[] rowData = entry.getValue();

            for (int i = 0; i < columns.size(); i++) {
                String columnName = columns.get(i);
                Object value = rowData[i];

                String indexTableName = Constants.getIndexTableName(tableName, columnName);
                removeIndexEntry(indexTableName, value, rowValue);
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

        for (Map.Entry<Integer, Object[]> entry : filteredRows.entrySet()) {
            Integer primaryKey = entry.getKey();
            Object[] oldRow = entry.getValue();
            Object[] newRow = Arrays.copyOf(oldRow, oldRow.length);

            for (AssignmentNode assignment : assignments) {
                String columnName = assignment.getColumn();
                int columnIndex = columns.indexOf(columnName);

                LiteralNode newValueNode = (LiteralNode) assignment.getValue();
                Object newValue = getValueFromLiteralNode(newValueNode);
                Object oldValue = oldRow[columnIndex];

                String indexTableName = Constants.getIndexTableName(tableName, columnName);
                BPlusTree<Number, Integer> indexTree = indexDatabase.get(indexTableName);

                indexTree.removeValue(convertToNumber(oldValue), primaryKey);
                indexTree.insert(convertToNumber(newValue), primaryKey);

                newRow[columnIndex] = newValue;
            }

            rows.update(primaryKey, newRow);
        }

        return filteredKeys.size() + " row(s) updated successfully";
    }

}