package edu.smu.smusql.skipHash;



import edu.smu.smusql.IEngine;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.*;

public class SkipHashEngine implements IEngine {

    Map<String, Table> data = new HashMap<>();

    public void clearDatabase() {
        data.clear();
    }

    public String executeSQL(String query) {
        String[] tokens = tokenize(query);
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

        // Check if the table already exists
        if (data.containsKey(tableName)) {
            return "ERROR: Table " + tableName + " already exists.";
        }

        // Extract column names from the parentheses
        String columnList = queryBetweenParentheses(tokens, 3);
        List<String> columns = parseColumns(columnList.split(" "), 0);

        if (columns.isEmpty()) {
            return "ERROR: No columns specified for table " + tableName;
        }

        // Create a new Table instance and add it to the data map
        Table newTable = new Table(tableName, columns);
        data.put(tableName, newTable);
        return "Table " + tableName + " created successfully";
    }

    public String insert(String[] tokens) {
        // `INSERT INTO table_name VALUES (value1, value2, ...)`
        String tableName = tokens[2].toLowerCase(); // Ensure table names are case-insensitive
        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between
        Table table = data.get(tableName);

        if (table == null) {
            return "Table " + tableName + " does not exist";
        }

        List<String> columns = table.getColumns();
        List<String> colVals = parseColumns(valueList.split(" "), 0);

        if (columns.size() != colVals.size()) {
            return "Invalid insert statement: number of values does not match number of columns";
        }

        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            rowData.put(columns.get(i), colVals.get(i).trim());
        }

        try {
            table.insertRow(colVals.get(0).trim(), rowData); // Insert using the ID as the first value
            return "1 row inserted successfully";
        } catch (RuntimeException e) {
            return e.getMessage();
        }

    }

    public String update(String[] tokens) {
        // Example: UPDATE table_name SET column1 = value1, column2 = value2 WHERE
        // condition
        String tableName = tokens[1].toLowerCase(); // Ensure table names are case-insensitive
        Table table = data.get(tableName);

        if (table == null) {
            return "ERROR: Table " + tableName + " does not exist.";
        }

        // Parse the SET clause (extract column-value pairs)
        int setIndex = 3; // Position after `SET`
        List<String> setClauses = new ArrayList<>();
        for (int i = setIndex; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("WHERE")) {
                break;
            }
            setClauses.add(tokens[i]);
        }

        // Process SET clauses (e.g., "column1 = value1, column2 = value2")
        Map<String, String> updates = new HashMap<>();
        String setClauseString = String.join(" ", setClauses);
        String[] assignments = setClauseString.split(",");
        for (String assignment : assignments) {
            String[] parts = assignment.trim().split("=");
            if (parts.length == 2) {
                String column = parts[0].trim().toLowerCase();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                updates.put(column, value);
            } else {
                return "ERROR: Invalid SET clause format.";
            }
        }

        // Parse WHERE clause if present
        List<String[]> whereConditions = new ArrayList<>();
        if (setIndex + setClauses.size() < tokens.length) {
            whereConditions = parseWhereClause(tokens, setIndex + setClauses.size() + 1);
        }

        // Apply update logic
        int updatedCount = applyUpdateLogic(table, updates, whereConditions);

        return updatedCount > 0 ? updatedCount + " row(s) updated successfully" : "0 row(s) updated, not found";
    }

    public int applyUpdateLogic(Table table, Map<String, String> updates, List<String[]> whereConditions) {
        List<String> keysToUpdate = whereConditions.isEmpty()
                ? new ArrayList<>(table.getData().keySet()) // Update all rows if no WHERE clause
                : applySelectLogic(table, whereConditions).stream().map(Row::getId).collect(Collectors.toList());

        int updatedCount = 0;
        for (String id : keysToUpdate) {
            Row row = table.getRow(id);
            if (row != null) {
                updates.forEach(row.getData()::put);
                table.updateRow(id, row.getData()); // Update the row in the table
                updatedCount++;
            }
        }
        return updatedCount;
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
        return deletedCount != 0 ? deletedCount + " row(s) deleted successfully" : "0 row(s) deleted, not found";
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

        Collections.sort(selectedRows);
        return selectedRows;
    }

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
                String value = tokens[i + 1].replaceAll("^['\"]|['\"]$", "");
                conditions.add(new String[] { null, column, operator, value });
                i += 1; // Skip the value since it's already processed
            }
        }
        return conditions;
    }

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

    private List<String> parseColumns(String[] tokens, int startIndex) {
        String columnList = queryBetweenParentheses(tokens, startIndex);
        return Arrays.stream(columnList.split(","))
                .map(v -> v.trim().replaceAll("^['\"]|['\"]$", ""))
                // Trim and remove surrounding quotes and parentheses
                .collect(Collectors.toList());
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
        return token.equals("=") || token.equals(">") || token.equals("<") || token.equals(">=") || token.equals("<=")
                || token.equals("!=");
    }
}
