package edu.smu.smusql;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Engine {

    Map<String, Table> data = new HashMap<>();

    public String executeSQL(String query) {

        String[] tokens = tokenize(query);
        String command = tokens[0];

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

    public void clearDatabase() {
        data.clear();
    }

    public String insert(String[] tokens) {
        // `INSERT INTO table_name VALUES (value1, value2, ...)`

        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive
        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.stream(valueList.split(","))
                .map(String::trim)
                .map(this::removeSurroundingQuotes) // Remove surrounding quotes from each value
                .toList();
        Table t = data.get(tableName);

        if (t == null) {
            return "Table " + tableName + " does not exist";
        }

        List<String> cols = t.getColumns();
        Map<String, String> colsVals = new HashMap<>();

        // Check for invalid column count
        if (cols.size() != values.size()) {
            return "Invalid insert statement: number of values does not match number of columns";
        }

        // Extract primary key (assuming the first column as primary key) and check for
        // duplicates
        String primaryKey = values.get(0);
        if (t.getRow(primaryKey) != null) { // Assuming Table has a method `rowExists` to check for primary key
                                            // existence
            return "0 row inserted, primary key already exists";
        }

        // Map columns to values
        for (int i = 0; i < cols.size(); i++) {
            colsVals.put(cols.get(i).toLowerCase(), values.get(i).trim());
        }

        // Insert row
        t.insertRow(primaryKey, colsVals);

        return "1 row inserted successfully";
    }

    // Helper method to remove surrounding quotes from a string
    private String removeSurroundingQuotes(String value) {
        if (value.startsWith("'") && value.endsWith("'") || value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1); // Remove the surrounding quotes
        }
        return value; // Return the value as-is if it has no surrounding quotes
    }

    public String delete(String[] tokens) {
        // `DELETE FROM table_name WHERE condition1 AND/OR condition2`

        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive
        Table t = data.get(tableName);
        if (t == null) {
            return "Table " + tableName + " does not exist";
        }

        List<String[]> whereClauseConditions = new ArrayList<>(); // Array for storing conditions from the where clause.

        // Parse WHERE clause conditions
        if (tokens.length > 3 && tokens[3].equalsIgnoreCase("WHERE")) {
            for (int i = 4; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("AND") || tokens[i].equalsIgnoreCase("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1].toLowerCase(); // Handle column name case-insensitively
                    String operator = tokens[i];
                    String value = removeSurroundingQuotes(tokens[i + 1]);
                    whereClauseConditions.add(new String[] { null, column, operator, value });
                    i += 1; // Skip the value since it has been processed
                }
            }
        }

        int rowCount = 0;
        List<String> keysToDelete;

        // Determine keys to delete based on conditions
        if (whereClauseConditions.size() == 1) {
            String[] requirements = whereClauseConditions.get(0);
            keysToDelete = requirements[1].equalsIgnoreCase("id")
                    ? t.returnKeysByRequirementsOnId(requirements[2], requirements[3])
                    : t.returnKeysByRequirementsOnIndex(requirements[1], requirements[2], requirements[3]);
        } else if (whereClauseConditions.size() > 1) {
            // Handling complex conditions with AND/OR
            String logic = whereClauseConditions.get(1)[0];
            String[] reqs1 = whereClauseConditions.get(0);
            String[] reqs2 = whereClauseConditions.get(2);

            List<String> keys1 = reqs1[1].equalsIgnoreCase("id")
                    ? t.returnKeysByRequirementsOnId(reqs1[2], reqs1[3])
                    : t.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]);

            List<String> keys2 = reqs2[1].equalsIgnoreCase("id")
                    ? t.returnKeysByRequirementsOnId(reqs2[2], reqs2[3])
                    : t.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]);

            Set<String> set1 = new HashSet<>(keys1);
            Set<String> set2 = new HashSet<>(keys2);

            if (logic.equals("OR")) {
                set1.addAll(set2);
            } else {
                set1.retainAll(set2);
            }
            keysToDelete = new ArrayList<>(set1);
        } else {
            // No WHERE clause (delete all rows)
            keysToDelete = t.getAllKeys();
        }

        // Perform deletion
        for (String id : keysToDelete) {
            if (t.deleteRow(id)) { // Assuming deleteRow returns true if a row is deleted
                rowCount++;
            }
        }

        // Return appropriate message
        if (rowCount == 1) {
            return "1 row(s) deleted successfully";
        } else if (rowCount > 1) {
            return rowCount + " row(s) deleted successfully";
        } else {
            return "0 row(s) deleted, not found";
        }
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
                    String column = tokens[i - 1].toLowerCase(); // Handle column name case-insensitively
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
                    keysToSelect.addAll(
                            t.returnKeysByRequirementsOnIndex(requirements[1], requirements[2], requirements[3]));
                }

                // Retrieve rows
                for (String id : keysToSelect) {
                    Row row = t.getRow(id);
                    if (row != null) {
                        resultRows.add(row);
                    }
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
            for (Row r : t.getData()) {
                if (r != null) {
                    resultRows.add(r);
                }
            }
        }

        // Format the results for display

        return formatSelectResults(resultRows, t.getColumns());
    }

    public String update(String[] tokens) {
        // `UPDATE table_name SET column1 = value1, column2 = value2, ... WHERE
        // condition1 AND/OR condition2`

        String tableName = tokens[1].toLowerCase(); // Ensure table names are case-insensitive
        Table t = data.get(tableName);
        if (t == null) {
            return "Table " + tableName + " does not exist";
        }

        Map<String, String> updates = new HashMap<>();
        int setIndex = 3;

        // Parse the SET clause to gather columns and values
        while (!tokens[setIndex].equalsIgnoreCase("WHERE") && setIndex < tokens.length) {
            String column = tokens[setIndex].toLowerCase(); // Handle column name case-insensitively
            String value = tokens[setIndex + 2];
            updates.put(column, removeSurroundingQuotes(value)); // Remove surrounding quotes
            setIndex += 3; // Move to the next column-value pair
        }

        // Initialize whereClauseConditions list
        List<String[]> whereClauseConditions = new ArrayList<>();

        // Parse WHERE clause conditions if present
        if (setIndex < tokens.length && tokens[setIndex].equalsIgnoreCase("WHERE")) {
            for (int i = setIndex + 1; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("AND") || tokens[i].equalsIgnoreCase("OR")) {
                    whereClauseConditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
                } else if (isOperator(tokens[i])) {
                    String column = tokens[i - 1].toLowerCase();
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] { null, column, operator, removeSurroundingQuotes(value) });
                    i += 1; // Skip the value since it has been processed
                }
            }
        }

        // Determine rows to update based on conditions
        int rowCount = 0;
        List<String> keysToUpdate;

        if (whereClauseConditions.size() == 1) {
            // Single condition case
            String[] condition = whereClauseConditions.get(0);
            keysToUpdate = condition[1].equalsIgnoreCase("id")
                    ? t.returnKeysByRequirementsOnId(condition[2], condition[3])
                    : t.returnKeysByRequirementsOnIndex(condition[1], condition[2], condition[3]);
        } else {
            // Complex conditions with AND/OR
            String logic = whereClauseConditions.get(1)[0];
            String[] condition1 = whereClauseConditions.get(0);
            String[] condition2 = whereClauseConditions.get(2);

            List<String> keys1 = condition1[1].equalsIgnoreCase("ID")
                    ? t.returnKeysByRequirementsOnId(condition1[2], condition1[3])
                    : t.returnKeysByRequirementsOnIndex(condition1[1], condition1[2], condition1[3]);

            List<String> keys2 = condition2[1].equalsIgnoreCase("ID")
                    ? t.returnKeysByRequirementsOnId(condition2[2], condition2[3])
                    : t.returnKeysByRequirementsOnIndex(condition2[1], condition2[2], condition2[3]);

            Set<String> set1 = new HashSet<>(keys1);
            Set<String> set2 = new HashSet<>(keys2);

            if (logic.equalsIgnoreCase("OR")) {
                set1.addAll(set2);
            } else {
                set1.retainAll(set2);
            }

            keysToUpdate = new ArrayList<>(set1);
        }

        // Apply the updates to each selected row
        for (String id : keysToUpdate) {
            Map<String, String> currData = t.getRow(id).getData();
            currData.putAll(updates); // Apply all updates to the row
            t.updateRow(id, currData); // Assuming this updates the row in the table
            rowCount++;
        }

        // Return the appropriate message
        if (rowCount == 1) {
            return "1 row(s) updated successfully";
        } else if (rowCount > 1) {
            return rowCount + " row(s) updated successfully";
        } else {
            return "0 row(s) updated, not found";
        }
    }

    public String create(String[] tokens) {
        // Example for reference CREATE TABLE student (id, name, age, gpa, deans_list)

        String name = tokens[2].toLowerCase().trim(); // Ensure table names are case-insensitive

        // Check if the table already exists in the `data` map
        if (data.containsKey(name)) {
            return "ERROR: Table " + name + " already exists.";
        }

        String[] colVals = Arrays.stream(queryBetweenParentheses(tokens, 3).toLowerCase().split(","))
                .map(x -> x.trim())
                .toArray(String[]::new);

        Table created = new Table(name, Arrays.asList(colVals));
        data.put(name, created);
        return "Table " + name + " created successfully";
    }

    // Helper methods


    public static String[] tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("'([^']*)'|\"([^\"]*)\"|(\\S+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                tokens.add("'" + m.group(1) + "'");
            } else if (m.group(2) != null) {
                tokens.add("\"" + m.group(2) + "\"");
            } else {
                tokens.add(m.group(3));
            }
        }
        return tokens.toArray(new String[0]);
    }
    private String queryBetweenParentheses(String[] tokens, int startIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < tokens.length; i++) {
            result.append(tokens[i]).append(" ");
        }
        return result.toString().trim().replaceAll("\\(", "").replaceAll("\\)", "");
    }

    // Helper method to determine if a string is an operator
    private boolean isOperator(String token) {
        return token.equals("=") || token.equals(">") || token.equals("<") || token.equals(">=") || token.equals("<=")
                || token.equals("!=");
    }

    // Helper method to format the results of a SELECT query
    private String formatSelectResults(List<Row> rows, List<String> columns) {
        StringBuilder sb = new StringBuilder();

        // Header (column names)
        sb.append(String.join("\t", columns)).append("\n");

        // Rows data
        for (Row row : rows) {
            if (row == null)
                continue; // Skip null rows
            Map<String, String> rowData = row.getData();
            for (String column : columns) {
                sb.append(rowData.get(column)).append("\t");
            }
            sb.append("\n");
        }

        return sb.toString().trim(); // Return the formatted string
    }
}
