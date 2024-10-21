package edu.smu.smusql.bplustreeA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.smu.smusql.Constants;
import edu.smu.smusql.parser.*;
import edu.smu.smusql.parser.nodes.*;

public class BPlusTreeEngine {

    private Map<String, BPlusTreeTable> database;
    private Map<String, BPlusTree<Integer, Integer>> indexDatabase;

    public BPlusTreeEngine() {
        this.database = new HashMap<>();

        if (Constants.INDEXING_ENABLED) {
            indexDatabase = new HashMap<>();
        }

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

        if (database.containsKey(tableName)) {
            return "ERROR: Table " + tableName + " already exists";
        }

        // Add table into database
        BPlusTreeTable table = new BPlusTreeTable(columns);
        database.put(tableName, table);

        /**
         * Create Indexing Trees with column fields as Key and PrimaryKey as Value
         */
        if (Constants.INDEXING_ENABLED) {
            for (String col : columns) {
                BPlusTree<Integer, Integer> indexTree = new BPlusTree<>(Constants.B_PLUS_TREE_ORDER);
                String indexTableName = Constants.getIndexTableName(tableName, col);
                indexDatabase.put(indexTableName, indexTree);

                if (Constants.LOGGING) {
                    System.out.println("Indexing Table " + indexTableName + " created successfully");
                }

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
    
        if (!database.containsKey(tableName)) {
            throw new RuntimeException("ERROR: Table " + tableName + " does not exist");
        }
    
        // Retrieve Table Information
        BPlusTreeTable table = database.get(tableName);
        List<String> columns = table.getColumns();
    
        if (values.size() != columns.size()) {
            throw new RuntimeException("ERROR: Column count does not match value count");
        }
    
        // Populate inserted table values
        Map<String, Object> rowData = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {

            LiteralNode literalNode = values.get(i);
            Object value = literalNode.getType() == LiteralNode.LiteralNodeType.STRING 
                    ? literalNode.getStringValue() 
                    : literalNode.getIntegerValue();

            rowData.put(columns.get(i), value);

            if (Constants.INDEXING_ENABLED) {
                String indexTableName = Constants.getIndexTableName(tableName, columns.get(i));
                insertIndexTable(indexTableName, value.hashCode(), primaryKey);
            }
        }
    
        // Get the table
        BPlusTree<Integer, Map<String, Object>> rows = table.getRows();
        rows.insert(primaryKey, rowData);

        return "1 row inserted successfully";
    }

    private void insertIndexTable(String tableName, Integer index, Integer primaryKey) {

        if (!indexDatabase.containsKey(tableName)) {
            throw new RuntimeException("ERROR: Table " + tableName + " does not exist");
        }

        BPlusTree<Integer, Integer> indexTree = indexDatabase.get(tableName);
        indexTree.insert(index, primaryKey);
    }

    public String delete(DeleteNode node) {
        // TODO
        return "not implemented";
    }

    public String select(SelectNode node) {
        // TODO
        return "not implemented";
    }

    public String update(UpdateNode node) {
        // TODO
        return "not implemented";
    }

}