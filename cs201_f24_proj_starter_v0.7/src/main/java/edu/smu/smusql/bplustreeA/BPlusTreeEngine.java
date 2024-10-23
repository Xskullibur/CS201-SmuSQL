package edu.smu.smusql.bplustreeA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.smu.smusql.Constants;
import edu.smu.smusql.parser.*;
import edu.smu.smusql.parser.nodes.*;

public class BPlusTreeEngine {

    private Map<String, BPlusTreeTable> database;
    private Map<String, BPlusTree<Number, Integer>> indexDatabase;

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
            List<Integer> leftResult = evaluateConditionNode(tableName, (ConditionNode) node.getLeft());
            List<Integer> rightResult = evaluateConditionNode(tableName, (ConditionNode) node.getRight());
            return combineResults(leftResult, rightResult, node.getOperator());

        } else if (node.getLeft() instanceof ExpressionNode && node.getRight() instanceof ExpressionNode) {
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

        // Check if we're searching by primary key
        if (columnName.equals("id")) { // Assuming "id" is your primary key column name
            Integer value = literalNode.getType() == LiteralNode.LiteralNodeType.NUMBER
                    ? literalNode.getIntegerValue()
                    : literalNode.getValue().hashCode();

            // For primary key searches, return a single-element list
            if (operator.equals("=")) {
                return Collections.singletonList(value);
            }

            // For other operators, get all keys and filter
            BPlusTreeTable table = retrieveTable(database, tableName);
            BPlusTree<Integer, Map<String, Object>> mainTree = table.getRows();
            List<Integer> allKeys = mainTree.getAllKeys();

            return filterKeysByOperator(allKeys, value, operator);
        }

        Number value = convertToNumber(getValueFromLiteralNode(literalNode));

        String indexTableName = Constants.getIndexTableName(tableName, columnName);
        BPlusTree<Number, Integer> indexTree = retrieveTable(indexDatabase, indexTableName);

        return evaluateCondition(indexTree, value, operator);
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
                throw new IllegalStateException("Unexpected LiteralNodeType: " + literalNode.getType());
        }
    }

    private Number convertToNumber(Object obj) {
        if (obj instanceof String) {
            return obj.hashCode();
        }

        else if (obj instanceof Integer) {
            return (Integer) obj;
        }

        else if (obj instanceof Float) {
            return (Float) obj;
        }

        else {
            throw new IllegalArgumentException("Received an unsupported object: " + obj.toString());
        }
    }

    private List<Integer> filterKeysByOperator(List<Integer> keys, Integer value, String operator) {
        return keys.stream()
                .filter(key -> {
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
                })
                .collect(Collectors.toList());
    }

    private List<Integer> evaluateCondition(BPlusTree<Number, Integer> indexTree, Number value, String operator) {
        switch (operator) {
            case "=":
                return indexTree.search(value);
            case "!=":
                // TODO: This can be optimized
                // Retrieve all values and filter out the ones equal to the given value
                List<Integer> allValues = indexTree.rangeSearch(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                allValues.removeAll(indexTree.search(value));
                return allValues;
            case "<":
                return indexTree.rangeSearch(Double.NEGATIVE_INFINITY, value.doubleValue() - 1);
            case "<=":
                return indexTree.rangeSearch(Double.NEGATIVE_INFINITY, value.doubleValue());
            case ">":
                return indexTree.rangeSearch(value.doubleValue() + 1, Double.POSITIVE_INFINITY);
            case ">=":
                return indexTree.rangeSearch(value.doubleValue(), Double.POSITIVE_INFINITY);
            default:
                throw new RuntimeException("Unsupported operator: " + operator);
        }
    }

    private List<Integer> combineResults(List<Integer> leftResult, List<Integer> rightResult, String operator) {
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

    private List<BPlusTree.Range<Integer>> groupKeysIntoRanges(List<Integer> keys) {

        List<BPlusTree.Range<Integer>> ranges = new ArrayList<>();

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
                ranges.add(new BPlusTree.Range<>(start, end));
                start = current;
                end = start;
            }
        }
        ranges.add(new BPlusTree.Range<>(start, end));

        return ranges;
    }

    private String formatSelectResults(List<Map<String, Object>> rows, List<String> columns) {
        StringBuilder sb = new StringBuilder();

        // Header (column names)
        sb.append(String.join("\t", columns)).append("\n");

        // Rows data
        for (Map<String, Object> row : rows) {
            if (row == null)
                continue; // Skip null rows
            for (String column : columns) {
                Object value = row.get(column);
                sb.append(value != null ? value.toString() : "NULL").append("\t");
            }
            sb.append("\n");
        }

        return sb.toString().trim(); // Return the formatted string
    }

    private boolean isPrimaryKeySearch(ConditionNode whereClause) {
        if (whereClause == null)
            return false;

        // Check if this is a simple equality condition on the primary key
        if (whereClause.getLeft() instanceof ColumnNode
                && whereClause.getRight() instanceof LiteralNode
                && whereClause.getOperator().equals("=")) {

            String columnName = ((ColumnNode) whereClause.getLeft()).getName();
            return columnName.equals("id"); // Assuming "id" is your primary key column name
        }

        return false;
    }

    public BPlusTreeEngine() {
        this.database = new HashMap<>();
        indexDatabase = new HashMap<>();
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
     * @param node The InsertNode containing the table name and values to be
     *             inserted.
     * @return A message indicating the result of the insertion operation.
     *         Returns "1 row inserted successfully" if the insertion is successful.
     *         Returns an error message if the specified table does not exist.
     */
    public String insert(InsertNode node) {

        // Retrieve Query Information
        String tableName = node.getTableName();
        Integer primaryKey = node.getPrimaryKey().getIntegerValue();
        List<LiteralNode> values = node.getValues().stream()
                .map(LiteralNode.class::cast)
                .collect(Collectors.toList());

        // Retrieve Table Information
        BPlusTreeTable table = retrieveTable(database, tableName);
        List<String> columns = table.getColumns();

        if (values.size() != columns.size()) {
            throw new RuntimeException("ERROR: Column count does not match value count");
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
        BPlusTree<Integer, Map<String, Object>> rows = table.getRows();
        rows.insert(primaryKey, rowData);

        return "1 row inserted successfully";
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
        if (whereClause == null && node.getColumns().get(0) == "*") {
            return formatSelectResults(rows.getAllValues(), columns);
        }

        // Filter indexes based on where clause
        List<Integer> filteredKeys = filterIndexes(tableName, whereClause);

        if (isPrimaryKeySearch(whereClause)) {
            List<Map<String, Object>> result = rows.search(filteredKeys.get(0));
            return formatSelectResults(result, columns);
        }

        // Group keys into ranges
        List<BPlusTree.Range<Integer>> ranges = groupKeysIntoRanges(filteredKeys);

        // Retrieve filtered Rows
        List<Map<String, Object>> fitleredRows = new ArrayList<>();

        for (BPlusTree.Range<Integer> range : ranges) {
            fitleredRows.addAll(rows.rangeSearch(range.getStart(), range.getEnd()));
        }

        return formatSelectResults(fitleredRows, columns);
    }

    public String delete(DeleteNode node) {
        // TODO
        return "not implemented";
    }

    public String update(UpdateNode node) {
        // TODO
        return "not implemented";
    }

}