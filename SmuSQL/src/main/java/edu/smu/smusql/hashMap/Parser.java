
package edu.smu.smusql.hashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * This is a parser for smuSQL statements.
 * The parser extracts table names, column names, and values from SQL commands.
 * It also supports parsing WHERE conditions for SELECT, UPDATE, and DELETE.
 */
public class Parser {

    // Parse INSERT statement
    public void parseInsert(String[] tokens) {
        String tableName = tokens[2]; // The name of the table to be inserted into.
        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.asList(valueList.split(",")); // These are the values in the row to be inserted.
        values.replaceAll(String::trim); // Trim spaces around values
        System.out.println("Inserting into table: " + tableName);
        System.out.println("Values: " + values);
    }

    // Parse DELETE statement with WHERE clause
    public void parseDelete(String[] tokens) {
        String tableName = tokens[2]; // The name of the table to be deleted from.

        List<String[]> whereClauseConditions = new ArrayList<>(); // Array for storing conditions from the where clause.

        // Parse WHERE clause conditions
        if (tokens.length > 3 && tokens[3].toUpperCase().equals("WHERE")) {
            for (int i = 4; i < tokens.length; i++) {
                if (tokens[i].toUpperCase().equals("AND") || tokens[i].toUpperCase().equals("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] {tokens[i].toUpperCase(), null, null, null});
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] {null, column, operator, value});
                    i += 1; // Skip the value since it has been processed
                }
            }
        }

        System.out.println("Deleting from table: " + tableName);
        System.out.println("Conditions: " + whereClauseConditions);
    }

    // Parse UPDATE statement with WHERE clause
    public void parseUpdate(String[] tokens) {
        String tableName = tokens[1]; // The name of the table to be updated.
        String updateColumn = tokens[3]; // The column to be updated.
        String updateValue = tokens[5]; // The new value for the column.

        List<String[]> whereClauseConditions = new ArrayList<>(); // Array for storing conditions from the where clause.

        // Parse WHERE clause conditions
        if (tokens.length > 6 && tokens[6].toUpperCase().equals("WHERE")) {
            for (int i = 7; i < tokens.length; i++) {
                if (tokens[i].toUpperCase().equals("AND") || tokens[i].toUpperCase().equals("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] {tokens[i].toUpperCase(), null, null, null});
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] {null, column, operator, value});
                    i += 1; // Skip the value since it has been processed
                }
            }
        }

        System.out.println("Updating table: " + tableName);
        System.out.println("Setting " + updateColumn + " = " + updateValue);
        System.out.println("Conditions: " + whereClauseConditions);
    }

    // Parse SELECT statement with optional WHERE clause
    public void parseSelect(String[] tokens) {
        String tableName = tokens[3]; // The name of the table to be queried.

        List<String[]> whereClauseConditions = new ArrayList<>(); // Array for storing conditions from the where clause.

        // Parse WHERE clause conditions
        if (tokens.length > 4 && tokens[4].toUpperCase().equals("WHERE")) {
            for (int i = 5; i < tokens.length; i++) {
                if (tokens[i].toUpperCase().equals("AND") || tokens[i].toUpperCase().equals("OR")) {
                    // Add AND/OR conditions
                    whereClauseConditions.add(new String[] {tokens[i].toUpperCase(), null, null, null});
                } else if (isOperator(tokens[i])) {
                    // Add condition with operator (column, operator, value)
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[] {null, column, operator, value});
                    i += 1; // Skip the value since it has been processed
                }
            }
        }

        System.out.println("Selecting from table: " + tableName);
        System.out.println("Conditions: " + whereClauseConditions);
    }

    // Helper method to check if a token is an operator
    private boolean isOperator(String token) {
        return token.equals("=") || token.equals(">") || token.equals("<") || token.equals(">=") || token.equals("<=");
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
