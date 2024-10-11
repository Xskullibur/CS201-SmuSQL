package edu.smu.smusql;

import java.util.*;

public class Engine {

    Map<String, Table> data = new HashMap<>();

    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\\s+");
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

    public String insert(String[] tokens) {
        // `INSERT INTO table_name VALUES (value1, value2, ...)`

        String tableName = tokens[2]; // The name of the table to be inserted into.
        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.asList(valueList.split(",")); // These are the values in the row to be inserted.
        Table t = data.get(tableName);
        List<String> cols = t.getColumns();
        Map<String, String> colsVals = new HashMap<>();
        // id is handled seperately
        if (cols.size() != values.size() - 1) {
            return "invalid insert statement";
        }

        for (int i = 0; i < cols.size(); i++) {
            colsVals.put(cols.get(i), values.get(i + 1));
        }

        t.insertRow(tokens[3], colsVals);

        return "inserted into " + tableName + " successfully";
    }

    public String delete(String[] tokens) {

        // `DELETE FROM table_name WHERE condition1 AND/OR condition2`

        String tableName = tokens[2]; // The name of the table to be deleted from.

        List<String[]> whereClauseConditions = new ArrayList<>(); // Array for storing conditions from the where clause.

        // Parse WHERE clause conditions
        if (tokens.length > 3 && tokens[3].toUpperCase().equals("WHERE")) {
            for (int i = 4; i < tokens.length; i++) {
                if (tokens[i].toUpperCase().equals("AND") || tokens[i].toUpperCase().equals("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] { tokens[i].toUpperCase(), null, null, null });
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1];
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

            List<String> keys1 = t.returnKeysByRequirementsOnIndex(reqs1[1], reqs1[2], reqs1[3]);
            List<String> keys2 = t.returnKeysByRequirementsOnIndex(reqs2[1], reqs2[2], reqs2[3]);

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
        // `SELECT * FROM table_name`
        // `SELECT * FROM table_name WHERE condition1 AND/OR condition2`

        int rowCount = 0;
        // TODO
        return "Returned " + rowCount + " rows";
    }

    public String update(String[] tokens) {
        // `UPDATE table_name SET update WHERE condition1 AND/OR condition2`
        // `UPDATE student SET age = 25 WHERE id = 1`

        String tableName = tokens[1]; // name of the table to be updated

        String setColumn = tokens[3]; // column to be updated
        String newValue = tokens[5]; // new value for above column

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
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] { null, column, operator, value });
                    i += 1; // Skip the value since it has been processed
                }
            }
        }
        // TODO
        int rowCount = 0;
        return rowCount + " rows updated successfully";
    }

    public String create(String[] tokens) {
        // exmaple for reference CREATE TABLE student (id, name, age, gpa, deans_list)

        String[] colVals = queryBetweenParentheses(tokens, 3).split(" ");
        String name = tokens[2];
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

}
