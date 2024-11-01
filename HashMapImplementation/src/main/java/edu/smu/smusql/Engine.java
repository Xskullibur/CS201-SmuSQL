package edu.smu.smusql;

import java.util.*;

public class Engine {
    // stores the contents of database tables in-memory using ArrayList
    private ArrayList<Table> dataList = new ArrayList<>();

    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\\s+");
        
        // Capitalize all tokens in the query
        tokens = capitalizeTokens(tokens);
        
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
        if (!tokens[1].equals("INTO")) {
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

    // Updated select method with proper AND/OR logic and inequality handling
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
            for (Map<String, String> row : allRows) {
                if (row != null) {
                    nonNullRows.add(row);
                }
            }
            return formatSelectResults(tbl.getColumns(), nonNullRows);
        }

        // Handle WHERE clause with AND/OR logic
        Set<Map<String, String>> resultSet = new HashSet<>();
        boolean isAnd = Arrays.asList(tokens).contains("AND");
        boolean isOr = Arrays.asList(tokens).contains("OR");

        String condition1Column = tokens[5];
        String condition1Operator = tokens[6];
        String condition1Value = tokens[7];

        String condition2Column = null;
        String condition2Operator = null;
        String condition2Value = null;

        if (tokens.length > 8) {
            condition2Column = tokens[9];
            condition2Operator = tokens[10];
            condition2Value = tokens[11];
        }

        // Fetch rows matching condition1 with inequality handling
        List<Map<String, String>> rowsMatchingCondition1 = tbl.selectWithCondition(condition1Column, condition1Operator, condition1Value);

        if (isAnd && condition2Column != null) {
            // Apply AND logic: Return only rows that satisfy both conditions
            for (Map<String, String> row : rowsMatchingCondition1) {
                // Ensure row satisfies both conditions
                if (compare(row.get(condition2Column), condition2Operator, condition2Value)) {
                    resultSet.add(row);  // Add row to result set only if both conditions are met
                }
            }
        } else if (isOr && condition2Column != null) {
            // Apply OR logic: Return rows that satisfy either of the conditions
            List<Map<String, String>> rowsMatchingCondition2 = tbl.selectWithCondition(condition2Column, condition2Operator, condition2Value);
            resultSet.addAll(rowsMatchingCondition1);  // Add condition1 rows
            resultSet.addAll(rowsMatchingCondition2);  // Add condition2 rows (duplicates automatically avoided by Set)
        } else {
            // If no AND/OR, just return rows that match condition1
            resultSet.addAll(rowsMatchingCondition1);
        }

        return formatSelectResults(tbl.getColumns(), new ArrayList<>(resultSet));
    }

    public String update(String[] tokens) {
        String tableName = tokens[1];
        Table tbl = getTableByName(tableName);
        if (tbl == null) {
            return "ERROR: No such table: " + tableName;
        }
    
        String updateColumn = tokens[3];
        String updateValue = tokens[5];
    
        String condition1Column = tokens[7];
        String condition1Operator = tokens[8];
        String condition1Value = tokens[9];
    
        String condition2Column = null;
        String condition2Operator = null;
        String condition2Value = null;
    
        boolean isAnd = Arrays.asList(tokens).contains("AND");
        boolean isOr = Arrays.asList(tokens).contains("OR");
    
        if (isAnd || isOr) {
            if (tokens.length > 10) {
                condition2Column = tokens[11];
                condition2Operator = tokens[12];
                condition2Value = tokens[13];
            } else {
                return "ERROR: Invalid query, missing second condition for AND/OR.";
            }
        }
    
        Map<String, String> updates = new HashMap<>();
        updates.put(updateColumn, updateValue);
    
        // Fetch rows matching condition1 with inequality handling
        List<Map<String, String>> rowsMatchingCondition1 = tbl.selectWithCondition(condition1Column, condition1Operator, condition1Value);
        int updatedCount = 0;
    
        if (isAnd && condition2Column != null) {
            for (Map<String, String> row : rowsMatchingCondition1) {
                if (compare(row.get(condition2Column), condition2Operator, condition2Value)) {
                    tbl.updateRow(row, updates); // Update the row only if both conditions are met
                    updatedCount++;
                }
            }
        } else if (isOr && condition2Column != null) {
            // Fetch rows matching condition2 with inequality handling
            List<Map<String, String>> rowsMatchingCondition2 = tbl.selectWithCondition(condition2Column, condition2Operator, condition2Value);
            for (Map<String, String> row : rowsMatchingCondition1) {
                tbl.updateRow(row, updates);
                updatedCount++;
            }
            for (Map<String, String> row : rowsMatchingCondition2) {
                if (!rowsMatchingCondition1.contains(row)) {
                    tbl.updateRow(row, updates);
                    updatedCount++;
                }
            }
        } else {
            updatedCount = tbl.updateWithCondition(condition1Column, condition1Operator, condition1Value, updates);
        }
    
        return updatedCount + " row(s) updated.";
    }

    public String delete(String[] tokens) {
        String tableName = tokens[2];
        Table tbl = getTableByName(tableName);
        if (tbl == null) {
            return "ERROR: No such table: " + tableName;
        }

        String condition1Column = tokens[4];
        String condition1Operator = tokens[5];
        String condition1Value = tokens[6];

        String condition2Column = null;
        String condition2Operator = null;
        String condition2Value = null;

        boolean isAnd = Arrays.asList(tokens).contains("AND");
        boolean isOr = Arrays.asList(tokens).contains("OR");

        if (tokens.length > 7) {
            condition2Column = tokens[8];
            condition2Operator = tokens[9];
            condition2Value = tokens[10];
        }

        List<Map<String, String>> rowsMatchingCondition1 = tbl.selectWithCondition(condition1Column, condition1Operator, condition1Value);
        int deletedCount = 0;

        if (isAnd && condition2Column != null) {
            for (Map<String, String> row : rowsMatchingCondition1) {
                if (compare(row.get(condition2Column), condition2Operator, condition2Value)) {
                    tbl.deleteRow(row); // Perform delete only if both conditions are satisfied
                    deletedCount++;
                }
            }
        } else if (isOr && condition2Column != null) {
            // Fetch rows matching condition2
            List<Map<String, String>> rowsMatchingCondition2 = tbl.selectWithCondition(condition2Column, condition2Operator, condition2Value);
            for (Map<String, String> row : rowsMatchingCondition1) {
                tbl.deleteRow(row); // Delete rows matching condition1
                deletedCount++;
            }
            for (Map<String, String> row : rowsMatchingCondition2) {
                if (!rowsMatchingCondition1.contains(row)) {
                    tbl.deleteRow(row); // Avoid deleting the same row twice
                    deletedCount++;
                }
            }
        } else {
            deletedCount = tbl.deleteWithCondition(condition1Column, condition1Operator, condition1Value);
        }

        return deletedCount + " row(s) deleted.";
    }

    // Comparison helper for inequality conditions
    private boolean compare(String value1, String operator, String value2) {
        try {
            // Try to parse both values as numbers for numeric comparison
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);

            switch (operator) {
                case "=":
                    return num1 == num2;
                case "<":
                    return num1 < num2;
                case ">":
                    return num1 > num2;
                case "<=":
                    return num1 <= num2;
                case ">=":
                    return num1 >= num2;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            // If values are not numeric, fallback to lexicographical comparison
            int comparison = value1.compareTo(value2);

            switch (operator) {
                case "=":
                    return comparison == 0;
                case "<":
                    return comparison < 0;
                case ">":
                    return comparison > 0;
                case "<=":
                    return comparison <= 0;
                case ">=":
                    return comparison >= 0;
                default:
                    return false;
            }
        }
    }

    // Helper method to get a table by its name
    private Table getTableByName(String tableName) {
        for (Table tbl : dataList) {
            if (tbl.getName().equalsIgnoreCase(tableName)) {
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

    // Helper method to capitalize all tokens in the input query
    private String[] capitalizeTokens(String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].toUpperCase();
        }
        return tokens;
    }

    // Format the SELECT results with aligned columns
    private String formatSelectResults(List<String> columns, List<Map<String, String>> rows) {
        StringBuilder result = new StringBuilder();

        int[] columnWidths = new int[columns.size()]; // Width of each column

        int numberOfColumns = columns.size();
        for (int i = 0; i < numberOfColumns; i++) {
            columnWidths[i] = columns.get(i).length();
        }

        // Measure the longest column value to determine spacing
        for (Map<String, String> row : rows) {
                for (int i = 0; i < numberOfColumns; i++) {
                    String columnValue = row.get(columns.get(i));
                    if (columnValue != null) {
                        columnWidths[i] = Math.max(columnWidths[i], columnValue.length()); // Take the highest length to ensure correct spacing
                    }
                }
            }

        // Print column names
        for (int i = 0; i < numberOfColumns; i++) {
            result.append(String.format("%-" + columnWidths[i] + "s", columns.get(i))); // "%-" is for left aligning and the "s" indicates its a String
            if (i < numberOfColumns - 1) result.append(" | "); // stop adding the line for last column
        }
        result.append("\n");
        result.append("-".repeat(result.length()) + "\n"); // Creating line to separate column heads and column values

        // Print selected rows
        for (Map<String, String> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                String value = row.get(columns.get(i));
                result.append(String.format("%-" + columnWidths[i] + "s", value != null ? value : "NULL"));
            if (i < numberOfColumns - 1) result.append(" | ");
            }
            result.append("\n");
        }

        return result.toString();
    }
}
