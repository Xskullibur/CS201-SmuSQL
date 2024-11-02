package edu.smu.smusql;

import java.util.*;
import java.util.stream.*;

public class Engine {

    Map<String, Table> data = new HashMap<>();

    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\\s+");
        String command = tokens[0].toUpperCase(); // Commands are always case-insensitive

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

    public String create(String[] tokens) {
        // Example: CREATE TABLE table_name (id, name, age, gpa)
        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive
    
        // Extract column names from the parentheses
        String columnList = queryBetweenParentheses(tokens, 3);
        List<String> columns = Arrays.stream(columnList.split(","))
                                     .map(String::trim)
                                     .collect(Collectors.toList());
    
        if (columns.isEmpty()) {
            return "ERROR: No columns specified for table " + tableName;
        }
    
        // Create a new Table instance and add it to the data map
        Table newTable = new Table(tableName, columns);
        data.put(tableName, newTable);
        return "Table " + tableName + " created successfully with columns: " + String.join(", ", columns);
    }
    

    public String insert(String[] tokens) {
        // `INSERT INTO table_name VALUES (value1, value2, ...)`
        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive
        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.asList(valueList.split(","));
        Table table = data.get(tableName);

        if (table == null) {
            return "Table " + tableName + " does not exist";
        }

        List<String> columns = table.getColumns();
        if (columns.size() != values.size()) {
            return "Invalid insert statement: number of values does not match number of columns";
        }

        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            rowData.put(columns.get(i), values.get(i).trim());
        }

        table.insertRow(values.get(0).trim(), rowData); // Insert using the ID as the first value
        return "Inserted into " + tableName + " successfully";
    }

    public String update(String[] tokens) {
        // Example: UPDATE table_name SET column = value WHERE condition
        String tableName = tokens[1].toLowerCase(); // Ensure table names are case-insensitive
        Table table = data.get(tableName);
    
        if (table == null) {
            return "ERROR: Table " + tableName + " does not exist.";
        }
    
        // Extract the column to update and the new value
        String setColumn = tokens[3].toLowerCase(); // Handle column name case-insensitively
        String newValue = tokens[5];
    
        // Parse WHERE clause if present
        List<String[]> whereConditions = new ArrayList<>();
        if (tokens.length > 6 && tokens[6].equalsIgnoreCase("WHERE")) {
            whereConditions = parseWhereClause(tokens, 6);
        }
    
        // Apply update logic
        List<String> keysToUpdate = whereConditions.isEmpty()
                ? new ArrayList<>(table.getData().keySet()) // Update all rows if no WHERE clause
                : table.returnKeysByRequirementsOnIndex(whereConditions.get(0)[1], whereConditions.get(0)[2], whereConditions.get(0)[3]);
    
        int updatedCount = 0;
        for (String id : keysToUpdate) {
            Row row = table.getRow(id);
            if (row != null) {
                row.getData().put(setColumn, newValue);
                table.updateRow(id, row.getData()); // Update the row in the table
                updatedCount++;
            }
        }
    
        return updatedCount + " rows updated in table " + tableName + ".";
    }
    

    public String delete(String[] tokens) {
        // `DELETE FROM table_name WHERE condition1 AND/OR condition2`
        String tableName = tokens[2].toLowerCase();
        Table table = data.get(tableName);

        if (table == null) {
            return "Table " + tableName + " does not exist";
        }

        List<String[]> whereConditions = parseWhereClause(tokens, 3);
        int deletedCount = applyDeleteLogic(table, whereConditions);
        return deletedCount + " rows deleted successfully";
    }

    public String select(String[] tokens) {
        // `SELECT * FROM table_name [WHERE condition1 AND/OR condition2]`
        String tableName = tokens[3].toLowerCase();
        Table table = data.get(tableName);

        if (table == null) {
            return "Table " + tableName + " does not exist";
        }

        List<String[]> whereConditions = parseWhereClause(tokens, 4);
        List<Row> selectedRows = applySelectLogic(table, whereConditions);
        return formatSelectResults(selectedRows, table.getColumns());
    }

    // Helper methods for parsing, condition handling, and formatting...
    private String queryBetweenParentheses(String[] tokens, int startIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < tokens.length; i++) {
            result.append(tokens[i]).append(" ");
        }
        return result.toString().trim().replaceAll("\\(", "").replaceAll("\\)", "");
    }


    private List<String[]> parseWhereClause(String[] tokens, int startIndex) {
        List<String[]> conditions = new ArrayList<>();
        for (int i = startIndex; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("AND") || tokens[i].equalsIgnoreCase("OR")) {
                conditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
            } else if (isOperator(tokens[i])) {
                String column = tokens[i - 1].toLowerCase();
                String operator = tokens[i];
                String value = tokens[i + 1];
                conditions.add(new String[] { null, column, operator, value });
                i += 1; // Skip the value since it's already processed
            }
        }
        return conditions;
    }

    // Apply delete logic with simplified indexing
    public int applyDeleteLogic(Table table, List<String[]> whereConditions) {
        int deletedCount = 0;
        if (whereConditions.size() == 1) {
            String[] condition = whereConditions.get(0);
            List<String> keysToDelete = table.returnKeysByRequirementsOnIndex(condition[1], condition[2], condition[3]);

            for (String id : keysToDelete) {
                table.deleteRow(id);
                deletedCount++;
            }
        } else {
            // Handle complex conditions with AND/OR logic...
            String logic = whereConditions.get(1)[0]; // AND/OR
            String[] reqs1 = whereConditions.get(0);
            String[] reqs2 = whereConditions.get(2);

            List<String> keys1 = table.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]);
            List<String> keys2 = table.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]);

            Set<String> set1 = new HashSet<>(keys1);
            Set<String> set2 = new HashSet<>(keys2);

            if (logic.equalsIgnoreCase("OR")) {
                set1.addAll(set2); // Union
            } else {
                set1.retainAll(set2); // Intersection
            }

            for (String id : set1) {
                table.deleteRow(id);
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private List<Row> applySelectLogic(Table table, List<String[]> whereConditions) {
        List<Row> selectedRows = new ArrayList<>();
        if (whereConditions.isEmpty()) {
            selectedRows.addAll(table.getData().values());
            return selectedRows;
        }
        
        if (whereConditions.size() == 1) {
            String[] condition = whereConditions.get(0);
            List<String> keysToSelect = table.returnKeysByRequirementsOnIndex(condition[1], condition[2], condition[3]);

            for (String id : keysToSelect) {
                Row row = table.getRow(id);
                if (row != null) {
                    selectedRows.add(row);
                }
            }
        } else {
            // Handle complex conditions with AND/OR logic...
            String logic = whereConditions.get(1)[0]; // AND/OR
            String[] reqs1 = whereConditions.get(0);
            String[] reqs2 = whereConditions.get(2);

            List<String> keys1 = table.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]);
            List<String> keys2 = table.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]);

            Set<String> set1 = new HashSet<>(keys1);
            Set<String> set2 = new HashSet<>(keys2);

            if (logic.equalsIgnoreCase("OR")) {
                set1.addAll(set2); // Union
            } else {
                set1.retainAll(set2); // Intersection
            }

            for (String id : set1) {
                Row row = table.getRow(id);
                if (row != null) {
                    selectedRows.add(row);
                }
            }
        }
        return selectedRows;
    }

    private String formatSelectResults(List<Row> rows, List<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\t", columns)).append("\n");

        for (Row row : rows) {
            for (String column : columns) {
                sb.append(row.getData().getOrDefault(column, "NULL")).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private boolean isOperator(String token) {
        return token.equals("=") || token.equals(">") || token.equals("<") || token.equals(">=") || token.equals("<=");
    }
}
