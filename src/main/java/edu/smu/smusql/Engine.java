package edu.smu.smusql;

import java.util.*;

public class Engine {

    Map<String, Table> data = new HashMap<>();

    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\\s+");
        String command = tokens[0].toUpperCase();  // Commands are always case-insensitive

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

    public String insert(String[] tokens) {
        // `INSERT INTO table_name VALUES (value1, value2, ...)`

        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive
        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.asList(valueList.split(",")); // These are the values in the row to be inserted.
        Table t = data.get(tableName);
        List<String> cols = t.getColumns();
        Map<String, String> colsVals = new HashMap<>();
        // id is handled separately
        if (cols.size() != values.size() - 1) {
            return "invalid insert statement";
        }

        for (int i = 0; i < cols.size(); i++) {
            colsVals.put(cols.get(i).toLowerCase(), values.get(i + 1));  // Handle column names case-insensitively
        }

        t.insertRow(tokens[3], colsVals);

        return "inserted into " + tableName + " successfully";
    }

    public String delete(String[] tokens) {

        // `DELETE FROM table_name WHERE condition1 AND/OR condition2`

        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive

        List<String[]> whereClauseConditions = new ArrayList<>(); // Array for storing conditions from the where clause.

        // Parse WHERE clause conditions
        if (tokens.length > 3 && tokens[3].equalsIgnoreCase("WHERE")) {
            for (int i = 4; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("AND") || tokens[i].equalsIgnoreCase("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1].toLowerCase();  // Handle column name case-insensitively
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] { null, column, operator, value });
                    i += 1; // Skip the value since it has been processed
                }
            }
        }
        Table t = data.get(tableName);
        int rowCount = 0;
        if (whereClauseConditions.size() == 1) {
            String[] requirements = whereClauseConditions.get(0);
            List<String> keysToDelete = t.returnKeysByRequirementsOnIndex(requirements[1], requirements[2],
                    requirements[3]);
            for (String id : keysToDelete) {
                rowCount++;
                t.deleteRow(id);
            }
        } else {
            String logic = whereClauseConditions.get(1)[0];
            String[] reqs1 = whereClauseConditions.get(0);
            String[] reqs2 = whereClauseConditions.get(2);

            List<String> keys1 = new ArrayList<>();

            if(reqs1[1].equalsIgnoreCase("ID")){
                keys1.addAll(t.returnKeysByRequirementsOnId(reqs1[2], reqs1[3]));
            } else{
                keys1.addAll(t.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]));
            }

            List<String> keys2 = new ArrayList<>();

            if(reqs2[1].equalsIgnoreCase("ID")){
                keys2.addAll(t.returnKeysByRequirementsOnId(reqs2[2], reqs2[3]));
            } else{
                keys2.addAll(t.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]));
            }

            Set<String> set1 = new HashSet<>(keys1);
            Set<String> set2 = new HashSet<>(keys2);

            if (logic.equals("OR")) {
                set1.addAll(set2);
            } else {
                set1.retainAll(set2);
            }

            for(String id : set1){
                rowCount++;
                t.deleteRow(id);
            }
        }
        return rowCount + " rows deleted successfully";
    }

    public String select(String[] tokens) {
        // SELECT * FROM table_name
        // SELECT * FROM table_name WHERE condition1 AND/OR condition2

        String tableName = tokens[3].toLowerCase(); // Ensure table names are case-insensitive
        Table t = data.get(tableName); // Get the table from the map

        List<Row> resultRows = new ArrayList<>(); // To store result rows

        // Check if there is a WHERE clause
        if (tokens.length > 4 && tokens[4].equalsIgnoreCase("WHERE")) {
            List<String[]> whereClauseConditions = new ArrayList<>();

            // Parse WHERE clause conditions
            for (int i = 5; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("AND") || tokens[i].equalsIgnoreCase("OR")) {
                    whereClauseConditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1].toLowerCase();  // Handle column name case-insensitively
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] { null, column, operator, value });
                    i += 1; // Skip the value since it has been processed
                }
            }

            // Handle single condition or complex conditions with AND/OR logic
            if (whereClauseConditions.size() == 1) {
                String[] requirements = whereClauseConditions.get(0);
                List<String> keysToSelect = new ArrayList<>();

                if (requirements[1].equalsIgnoreCase("ID")) {
                    keysToSelect.addAll(t.returnKeysByRequirementsOnId(requirements[2], requirements[3]));
                } else {
                    keysToSelect.addAll(t.returnKeysByRequirementsOnIndex(requirements[1], requirements[2], requirements[3]));
                }

                // Retrieve rows
                for (String id : keysToSelect) {
                    resultRows.add(t.getRow(id));
                }
            } else {
                String logic = whereClauseConditions.get(1)[0]; // AND/OR
                String[] reqs1 = whereClauseConditions.get(0);
                String[] reqs2 = whereClauseConditions.get(2);

                // Get rows based on both conditions
                List<String> keys1 = new ArrayList<>();
                if (reqs1[1].equalsIgnoreCase("ID")) {
                    keys1.addAll(t.returnKeysByRequirementsOnId(reqs1[2], reqs1[3]));
                } else {
                    keys1.addAll(t.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]));
                }

                List<String> keys2 = new ArrayList<>();
                if (reqs2[1].equalsIgnoreCase("ID")) {
                    keys2.addAll(t.returnKeysByRequirementsOnId(reqs2[2], reqs2[3]));
                } else {
                    keys2.addAll(t.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]));
                }

                // Perform AND or OR on the keys
                Set<String> set1 = new HashSet<>(keys1);
                Set<String> set2 = new HashSet<>(keys2);

                if (logic.equalsIgnoreCase("OR")) {
                    set1.addAll(set2); // Union
                } else {
                    set1.retainAll(set2); // Intersection
                }

                // Retrieve rows
                for (String id : set1) {
                    resultRows.add(t.getRow(id));
                }
            }
        } else {
            // No WHERE clause, select all rows
            for (Row row : t.getData()) {
                resultRows.add(row);
            }
        }

        // Format the results for display
        return formatSelectResults(resultRows, t.getColumns());
    }

    public String update(String[] tokens) {
        // `UPDATE table_name SET update WHERE condition1 AND/OR condition2`
        // `UPDATE student SET age = 25 WHERE id = 1`

        String tableName = tokens[1].toLowerCase(); // Ensure table names are case-insensitive
        String setColumn = tokens[3].toLowerCase(); // Handle column name case-insensitively
        String newValue = tokens[5];

        // Initialize whereClauseConditions list
        List<String[]> whereClauseConditions = new ArrayList<>();

        // Parse WHERE clause conditions
        if (tokens.length > 6 && tokens[6].equalsIgnoreCase("WHERE")) {
            for (int i = 5; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("AND") || tokens[i].equalsIgnoreCase("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1].toLowerCase();  // Handle column name case-insensitively
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] { null, column, operator, value });
                    i += 1; // Skip the value since it has been processed
                }
            }
        }

        Table t = data.get(tableName);
        int rowCount = 0;
        if (whereClauseConditions.size() == 1) {
            String[] requirements = whereClauseConditions.get(0);
            List<String> keysToUpdate = t.returnKeysByRequirementsOnIndex(requirements[1], requirements[2], requirements[3]);

            for (String id : keysToUpdate) {
                Map<String, String> currData = t.getRow(id).getData();
                currData.put(setColumn, newValue);  // Handle column name case-insensitively
                t.updateRow(id, currData);
                rowCount++;
            }
        } else {
            String logic = whereClauseConditions.get(1)[0];
            String[] reqs1 = whereClauseConditions.get(0);
            String[] reqs2 = whereClauseConditions.get(2);

            List<String> keys1 = new ArrayList<>();

            if (reqs1[1].equalsIgnoreCase("ID")) {
                keys1.addAll(t.returnKeysByRequirementsOnId(reqs1[2], reqs1[3]));
            } else {
                keys1.addAll(t.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]));
            }

            List<String> keys2 = new ArrayList<>();

            if (reqs2[1].equalsIgnoreCase("ID")) {
                keys2.addAll(t.returnKeysByRequirementsOnId(reqs2[2], reqs2[3]));
            } else {
                keys2.addAll(t.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]));
            }

            Set<String> set1 = new HashSet<>(keys1);
            Set<String> set2 = new HashSet<>(keys2);

            if (logic.equalsIgnoreCase("OR")) {
                set1.addAll(set2);
            } else {
                set1.retainAll(set2);
            }

            for (String id : set1) {
                Map<String, String> currData = t.getRow(id).getData();
                currData.put(setColumn, newValue);  // Handle column name case-insensitively
                t.updateRow(id, currData);
                rowCount++;
            }
        }
        return rowCount + " rows updated successfully";
    }

    public String create(String[] tokens) {
        // example for reference CREATE TABLE student (id, name, age, gpa, deans_list)

        String[] colVals = queryBetweenParentheses(tokens, 3).split(" ");
        String name = tokens[2].toLowerCase();  // Ensure table names are case-insensitive
        Table created = new Table(name, Arrays.asList(colVals));
        data.put(name, created);
        return "Table " + name + " created successfully";
    }

    // Helper methods
    private String queryBetweenParentheses(String[] tokens, int startIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < tokens.length; i++) {
            result.append(tokens[i]).append(" ");
        }
        return result.toString().trim().replaceAll("\\(", "").replaceAll("\\)", "");
    }

    // Helper method to determine if a string is an operator
    private boolean isOperator(String token) {
        return token.equals("=") || token.equals(">") || token.equals("<") || token.equals(">=") || token.equals("<=");
    }

    // Helper method to format the results of a SELECT query
    private String formatSelectResults(List<Row> rows, List<String> columns) {
        StringBuilder sb = new StringBuilder();

        // Header (column names)
        sb.append(String.join("\t", columns)).append("\n");

        // Rows data
        for (Row row : rows) {
            Map<String, String> rowData = row.getData();
            for (String column : columns) {
                sb.append(rowData.get(column)).append("\t");
            }
            sb.append("\n");
        }

        return sb.toString().trim(); // Return the formatted string
    }
}
