
package edu.smu.smusql;

import java.util.*;

public class Engine {
    // stores the contents of database tables in-memory using ArrayList
    private ArrayList<Table> dataList = new ArrayList<>();

    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\s+");
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "CREATE":
                return create(tokens);
            case "INSERT":
                return insert(tokens);
            case "SELECT":
                return select(tokens);
            case "UPDATE":
                return update(tokens);
            case "DELETE":
                return delete(tokens);
            default:
                return "ERROR: Unknown command";
        }
    }

    // Implementation of the CREATE TABLE command
    public String create(String[] tokens) {
        String tableName = tokens[2]; // assumes syntax: CREATE TABLE table_name (col1, col2, ...)
        String columnList = queryBetweenParentheses(tokens, 3); // Get columns list between parentheses
        List<String> columns = Arrays.asList(columnList.split(","));

        // Trim each column to remove spaces around them
        columns.replaceAll(String::trim);

        // Create a new table and add to dataList
        Table newTable = new Table(tableName, columns);
        dataList.add(newTable);
        return "Table " + tableName + " created.";
    }

    // Implementation of the INSERT INTO command
    public String insert(String[] tokens) {
        if (!tokens[1].toUpperCase().equals("INTO")) {
            return "ERROR: Invalid INSERT INTO syntax";
        }

        String tableName = tokens[2];
        Table tbl = getTableByName(tableName);
        if (tbl == null) {
            return "ERROR: No such table: " + tableName;
        }

        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.asList(valueList.split(","));

        // Trim each value to avoid spaces around them
        values.replaceAll(String::trim);

        List<String> columns = tbl.getColumns();

        if (values.size() != columns.size()) {
            return "ERROR: Column count doesn't match value count";
        }

        // Insert the row into the table
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            row.put(columns.get(i), values.get(i));
        }
        tbl.addRow(row);
        return "Row inserted into " + tableName;
    }

    // Updated implementation of the SELECT command to skip null rows
    public String select(String[] tokens) {
        String tableName = tokens[3];
        Table tbl = getTableByName(tableName);
        if (tbl == null) {
            return "ERROR: No such table: " + tableName;
        }

        // Simple select all (assuming SELECT * FROM table_name for now)
        if (tokens.length == 4) {
            List<Map<String, String>> allRows = tbl.getDataList();
            List<Map<String, String>> nonNullRows = new ArrayList<>();

            // Skip null rows
            for (Map<String, String> row : allRows) {
                if (row != null) {
                    nonNullRows.add(row);
                }
            }

            return nonNullRows.toString();
        }

        // Handle conditional selects (assuming WHERE clause present)
        String column = tokens[5];  // assumes syntax: SELECT * FROM table_name WHERE column = value
        String value = tokens[7];

        // Fetch matching rows using the index in Table
        List<Map<String, String>> results = tbl.select(column, value);
        return results.toString();
    }

    // Updated implementation of the UPDATE command to skip null rows
    public String update(String[] tokens) {
        String tableName = tokens[1];
        Table tbl = getTableByName(tableName);
        if (tbl == null) {
            return "ERROR: No such table: " + tableName;
        }

        // assumes syntax: UPDATE table_name SET column = value WHERE condition_column = condition_value
        String updateColumn = tokens[3];
        String updateValue = tokens[5];
        String conditionColumn = tokens[7];
        String conditionValue = tokens[9];

        Map<String, String> updates = new HashMap<>();
        updates.put(updateColumn, updateValue);

        // Update matching rows (skipping null rows)
        int updatedCount = tbl.update(conditionColumn, conditionValue, updates);
        return updatedCount + " row(s) updated.";
    }

    // Implementation of the DELETE command
    public String delete(String[] tokens) {
        String tableName = tokens[2];
        Table tbl = getTableByName(tableName);
        if (tbl == null) {
            return "ERROR: No such table: " + tableName;
        }

        // assumes syntax: DELETE FROM table_name WHERE condition_column = condition_value
        String conditionColumn = tokens[4];
        String conditionValue = tokens[6];

        // Delete matching rows
        int deletedCount = tbl.delete(conditionColumn, conditionValue);
        return deletedCount + " row(s) deleted.";
    }

    // Helper method to get table by name
    private Table getTableByName(String tableName) {
        for (Table tbl : dataList) {
            if (tbl.getName().equals(tableName)) {
                return tbl;
            }
        }
        return null;
    }

    // Helper method to extract content within parentheses
    private String queryBetweenParentheses(String[] tokens, int start) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < tokens.length; i++) {
            result.append(tokens[i]).append(" ");
        }
        return result.toString().replaceAll("[()]", "").trim();
    }
}
