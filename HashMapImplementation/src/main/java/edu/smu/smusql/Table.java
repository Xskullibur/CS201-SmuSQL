package edu.smu.smusql;

import java.util.*;

/*
 * An improved implementation of a database table for smuSQL using ArrayList and HashMaps.
 * Implements a double-level indexing system for faster query processing.
 */
public class Table {

    // Use ArrayList to store rows of data
    private ArrayList<Map<String, String>> dataList;

    // Create multiple HashMaps for indexing various column combinations
    private Map<String, Map<String, List<Integer>>> indexMap;

    private String name;
    private List<String> columns;

    // Constructor: Initialize the table with columns and the index map
    public Table(String name, List<String> columns) {
        dataList = new ArrayList<>();
        this.name = name;
        this.columns = columns;
        indexMap = new HashMap<>();
        // Initialize HashMaps for each column combination (for simplicity, we'll start with single-column indices)
        for (String column : columns) {
            indexMap.put(column, new HashMap<>());
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    // Method to add a new row and update the index maps
    public void addRow(Map<String, String> new_row) {
        // Add the new row to the data list
        dataList.add(new_row);
        int rowIndex = dataList.size() - 1; // Get the index of the newly inserted row

        // Update each column's index map with the new row data
        for (String column : columns) {
            String columnValue = new_row.get(column);
            Map<String, List<Integer>> columnIndex = indexMap.get(column);
            columnIndex.putIfAbsent(columnValue, new ArrayList<>());
            columnIndex.get(columnValue).add(rowIndex); // Add row index to the list of this column value
        }
    }

    // Method to select rows based on a condition (handles =, >, <, >=, <=)
    public List<Map<String, String>> selectWithCondition(String column, String operator, String value) {
        List<Map<String, String>> result = new ArrayList<>();

        for (Map<String, String> row : dataList) {
            if (row != null) {
                String columnValue = row.get(column);
                if (compare(columnValue, operator, value)) {
                    result.add(row);
                }
            }
        }
        return result;
    }

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
                case "!=":
                    return num1 != num2;
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
                case "!=":
                    return comparison != 0;
                default:
                    return false;
            }
        }
    }

    // Method to update rows based on a condition (handles =, >, <, >=, <=)
    public int updateWithCondition(String column, String operator, String value, Map<String, String> updates) {
        int updatedCount = 0;

        for (Map<String, String> row : dataList) {
            if (row != null) {
                String columnValue = row.get(column);
                if (compare(columnValue, operator, value)) {
                    for (String updateColumn : updates.keySet()) {
                        row.put(updateColumn, updates.get(updateColumn));
                    }
                    updatedCount++;
                }
            }
        }
        return updatedCount;
    }

    // Method to delete rows based on a condition (handles =, >, <, >=, <=)
    public int deleteWithCondition(String column, String operator, String value) {
        int deletedCount = 0;

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, String> row = dataList.get(i);
            if (row != null) {
                String columnValue = row.get(column);
                if (compare(columnValue, operator, value)) {
                    dataList.set(i, null);  // Logical deletion by setting the row to null
                    deletedCount++;
                }
            }
        }
        return deletedCount;
    }

    // Method to update a single row
    public void updateRow(Map<String, String> row, Map<String, String> updates) {
        for (String updateColumn : updates.keySet()) {
            row.put(updateColumn, updates.get(updateColumn));
        }
    }

    // Method to delete a single row
    public void deleteRow(Map<String, String> row) {
        dataList.remove(row);  // Logical deletion
    }

    // Method to get the dataList (for SELECT *)
    public List<Map<String, String>> getDataList() {
        return dataList;
    }
}
