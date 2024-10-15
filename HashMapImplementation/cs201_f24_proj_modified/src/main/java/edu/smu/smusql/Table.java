
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

    // Method to select rows based on a column value
    public List<Map<String, String>> select(String column, String value) {
        List<Map<String, String>> result = new ArrayList<>();
        Map<String, List<Integer>> columnIndex = indexMap.get(column);

        // Get row indices that match the given value
        if (columnIndex.containsKey(value)) {
            List<Integer> rowIndices = columnIndex.get(value);
            for (int rowIndex : rowIndices) {
                Map<String, String> row = dataList.get(rowIndex);
                if (row != null) {  // Skip null (deleted) rows
                    result.add(row);
                }
            }
        }
        return result;
    }

    // Updated method to ensure null checks before updating rows
    public int update(String column, String value, Map<String, String> updates) {
        int updatedCount = 0;
        Map<String, List<Integer>> columnIndex = indexMap.get(column);

        // Get row indices that match the condition
        if (columnIndex.containsKey(value)) {
            List<Integer> rowIndices = columnIndex.get(value);
            for (int rowIndex : rowIndices) {
                Map<String, String> row = dataList.get(rowIndex);
                if (row != null) {  // Skip null (deleted) rows
                    // Apply the updates
                    for (String updateColumn : updates.keySet()) {
                        row.put(updateColumn, updates.get(updateColumn));  // Safely update non-null rows
                    }
                    updatedCount++;
                }
            }
        }
        return updatedCount;
    }

    // Method to delete rows based on a condition
    public int delete(String column, String value) {
        int deletedCount = 0;
        Map<String, List<Integer>> columnIndex = indexMap.get(column);

        // Get row indices that match the condition
        if (columnIndex.containsKey(value)) {
            List<Integer> rowIndices = columnIndex.get(value);
            for (int rowIndex : rowIndices) {
                dataList.set(rowIndex, null); // Set the row to null (logical deletion)
                deletedCount++;
            }
            columnIndex.remove(value); // Remove the value from the index map
        }
        return deletedCount;
    }

    // Method to get the dataList
    public List<Map<String, String>> getDataList() {
        return dataList;
    }
}

