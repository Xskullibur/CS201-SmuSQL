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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BPlusTreeEngine implements IEngine {

    private Map<String, BPlusTreeTable> database;
    private Map<String, BPlusTree<Number, Integer>> indexDatabase;

    public BPlusTreeEngine() {
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
            BPlusTreeTable table = retrieveTable(database, tableName);
            BPlusTree<Integer, Map<String, Object>> mainTree = table.getRows();
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

    private String formatSelectResults(Map<Integer, Map<String, Object>> rows,
        List<String> columns) {
        StringBuilder sb = new StringBuilder();

        // Header (column names)
        sb.append("id\t").append(String.join("\t", columns)).append("\n");

        // Rows data
        for (Map.Entry<Integer, Map<String, Object>> entry : rows.entrySet()) {

            Integer id = entry.getKey();
            Map<String, Object> row = entry.getValue();

            if (row == null) {
                continue; // Skip null rows
            }

            sb.append(id).append("\t");

            for (String column : columns) {
                Object value = row.get(column);
                sb.append(value != null ? value.toString() : "NULL").append("\t");
            }
            sb.append("\n");
        }

        return sb.toString().trim(); // Return the formatted string
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
        BPlusTreeTable table = database.get(tableName);
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
        BPlusTreeTable newTable = new BPlusTreeTable(columns);
        database.put(tableName, newTable);

        return "Table " + tableName + " cleared successfully";
    }

    public String create(CreateTableNode node) {

        String tableName = node.getTableName();
        List<String> columns = node.getColumns();

        // Add table into database
        if (database.containsKey(tableName)) {
            throw new RuntimeException("Table " + tableName + " already exist");
        }

        BPlusTreeTable table = new BPlusTreeTable(columns);
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
     * @param node The InsertNode containing the table name and values to be inserted.
     * @return A message indicating the result of the insertion operation. Returns "1 row inserted
     * successfully" if the insertion is successful. Returns an error message if the specified table
     * does not exist.
     */
    public String insert(InsertNode node) {

        // Retrieve Query Information
        String tableName = node.getTableName();
        Integer primaryKey = node.getPrimaryKey().getIntegerValue();
        List<LiteralNode> values = node.getValues().stream().map(LiteralNode.class::cast).toList();

        // Retrieve Table Information
        BPlusTreeTable table = retrieveTable(database, tableName);
        BPlusTree<Integer, Map<String, Object>> rows = table.getRows();
        List<String> columns = table.getColumns();

        if (values.size() != columns.size()) {
            throw new RuntimeException("ERROR: Column count does not match value count");
        }

        // Search database for existing primary key
        if (rows.search(primaryKey) != null) {
            return "0 row inserted, primary key already exists";
        }

        // Populate inserted table values
        Map<String, Object> rowData = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            String indexTableName = Constants.getIndexTableName(tableName, column);
            LiteralNode literalNode = values.get(i);

            Object value = getValueFromLiteralNode(literalNode);
            rowData.put(column, value);
            BPlusTree<Number, Integer> tree = indexDatabase.get(indexTableName);
            tree.insert(convertToNumber(value), primaryKey);
        }

        // Get the table
        rows.insert(primaryKey, rowData);

        return "1 row inserted successfully";
    }

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

    public String select(SelectNode node) {

        // Retrieve query information
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();

        // Retrieve table
        BPlusTreeTable table = retrieveTable(database, tableName);
        BPlusTree<Integer, Map<String, Object>> rows = table.getRows();
        List<String> columns = table.getColumns();

        // Handle SELECT * Query
        if (whereClause == null && Objects.equals(node.getColumns().get(0), "*")) {
            return formatSelectResults(rows.getAllKeyValues(), columns);
        }

        // Get primary keys based on whereClause
        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        // Get rows using filteredKeys
        Map<Integer, Map<String, Object>> fitleredRows = retrieveFilteredRows(filteredKeys, rows);

        // Format and return output
        return formatSelectResults(fitleredRows, columns);
    }

    public String delete(DeleteNode node) {

        // Retrieve query info
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();

        // Retrieve table
        BPlusTreeTable table = retrieveTable(database, tableName);
        BPlusTree<Integer, Map<String, Object>> rows = table.getRows();

        if (rows.getSize() == 0) {
            return "0 row(s) deleted, no rows found";
        }

        // Get primary keys based on whereClause
        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        // Get rows using filteredKeys
        Map<Integer, Map<String, Object>> fitleredRows = retrieveFilteredRows(filteredKeys, rows);

        if (fitleredRows.isEmpty()) {
            return "0 row(s) deleted, not found";
        }

        // Remove entries from the index database
        /**
         * IndexDatabase:
         * - Key: Columns
         * - Value: PrimaryKey
         */
        for (Map.Entry<Integer, Map<String, Object>> row : fitleredRows.entrySet()) {

            Integer rowValue = row.getKey();
            Map<String, Object> rowData = row.getValue();

            for (Map.Entry<String, Object> column : rowData.entrySet()) {

                String columnName = column.getKey();
                Number columnValue = convertToNumber(column.getValue());

                String indexTableName = Constants.getIndexTableName(tableName, columnName);
                removeIndexEntry(indexTableName, columnValue, rowValue);
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

        // Retrieve query information
        String tableName = node.getTableName();
        ConditionNode whereClause = node.getWhereClause();
        List<AssignmentNode> assignments = node.getAssignments();

        // Retrieve table
        BPlusTreeTable table = retrieveTable(database, tableName);
        BPlusTree<Integer, Map<String, Object>> rows = table.getRows();

        if (rows.getSize() == 0) {
            return "0 row(s) updated, no rows found";
        }

        // Get primary keys based on whereClause
        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);
        // Get rows using filteredKeys
        Map<Integer, Map<String, Object>> filteredRows = retrieveFilteredRows(filteredKeys, rows);

        if (filteredRows.isEmpty()) {
            return "0 row(s) updated, not found";
        }

        // For each row that matches the where clause
        for (Map.Entry<Integer, Map<String, Object>> row : filteredRows.entrySet()) {
            Integer primaryKey = row.getKey();
            Map<String, Object> rowData = row.getValue();

            // Create a new row data with updated values
            Map<String, Object> updatedRowData = new HashMap<>(rowData);

            // For each assignment node
            for (AssignmentNode assignment : assignments) {

                String columnName = assignment.getColumn();
                LiteralNode newValueNode = (LiteralNode) assignment.getValue();
                Object newValue = getValueFromLiteralNode(newValueNode);
                Object oldValue = rowData.get(columnName);

                // Update the index tree
                String indexTableName = Constants.getIndexTableName(tableName, columnName);
                BPlusTree<Number, Integer> indexTree = indexDatabase.get(indexTableName);

                // Remove old index entry
                indexTree.removeValue(convertToNumber(oldValue), primaryKey);

                // Insert new index entry
                indexTree.insert(convertToNumber(newValue), primaryKey);

                // Update the row data
                updatedRowData.put(columnName, newValue);
            }

            // Update the main tree
            rows.update(primaryKey, updatedRowData);
        }

        return filteredKeys.size() + " row(s) updated successfully";
    }

}