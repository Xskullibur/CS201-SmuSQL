package edu.smu.smusql;

import java.util.*;

public class Table {
    String name; // Table name
    List<String> columns; // List of column names
    SkipList<Row> data; // SkipList to store rows based on the primary key
    Map<String, SkipList<String, Set<Row>>> secondaryIndices; // Map of secondary indices for columns

    public Table(String name, List<String> columns) {
        this.name = name;
        this.columns = columns;
        this.data = new SkipList<>();
        this.secondaryIndices = new HashMap<>();
    }

    // Create a secondary index for a specific column
    public void createSecondaryIndex(String column) {
        if (!columns.contains(column)) {
            throw new IllegalArgumentException("Column " + column + " does not exist in table");
        }
        secondaryIndices.put(column, new SkipList<>()); // Create a new SkipList for the column
    }

    // Insert a new row into the table
    public void insertRow(String id, Map<String, String> rowData) {
        // Validate that the row data matches the table schema
        if (!validateRow(rowData)) {
            throw new IllegalArgumentException("Row does not match table schema");
        }

        // Insert the new row into the primary skip list (indexed by ID)
        Row row = new Row(id, rowData);
        data.add(row); // Assuming SkipList has an add() method

        // Update all secondary indices
        updateSecondaryIndices(row, rowData, true);
    }

    // Update an existing row based on its ID
    public void updateRow(String id, Map<String, String> newData) {
        Row row = data.search(id); // Find the row by its ID (primary key)

        if (row != null) {
            // Update the row's data
            for (Map.Entry<String, String> entry : newData.entrySet()) {
                if (columns.contains(entry.getKey())) {
                    row.data.put(entry.getKey(), entry.getValue());
                }
            }

            // Update the secondary indices with the new data
            updateSecondaryIndices(row, newData, false);
        } else {
            throw new NoSuchElementException("Row with ID " + id + " not found");
        }
    }

    // Delete a row based on its ID
    public void deleteRow(String id) {
        Row row = data.search(id); // Find the row by its ID
        if (row != null) {
            // Remove the row from the primary skip list
            data.remove(row);

            // Remove the row from all secondary indices
            updateSecondaryIndices(row, row.data, false);
        } else {
            throw new NoSuchElementException("Row with ID " + id + " not found");
        }
    }

    // Retrieve a row based on its ID
    public Row getRow(String id) {
        return data.search(id); // Assuming SkipList has a search() method
    }

    // Retrieve all rows that match a certain value in an indexed column
    public List<Row> selectRowsByIndex(String column, String value) {
        if (!secondaryIndices.containsKey(column)) {
            throw new IllegalArgumentException("No index found for column " + column);
        }

        SkipList<String, Set<Row>> index = secondaryIndices.get(column);
        Set<Row> rows = index.search(value); // Search the secondary index for the given value

        return rows != null ? new ArrayList<>(rows) : Collections.emptyList();
    }

    // Update the secondary indices when a row is inserted, updated, or deleted
    private void updateSecondaryIndices(Row row, Map<String, String> rowData, boolean isInsert) {
        for (String column : secondaryIndices.keySet()) {
            String columnValue = rowData.get(column);
            SkipList<String, Set<Row>> index = secondaryIndices.get(column);

            if (isInsert) {
                // Insert into the secondary index
                index.computeIfAbsent(columnValue, k -> new HashSet<>()).add(row);
            } else {
                // Remove from the secondary index if it's an update or delete
                Set<Row> rows = index.search(columnValue);
                if (rows != null) {
                    rows.remove(row);
                    if (rows.isEmpty()) {
                        index.remove(columnValue); // Clean up empty index entries
                    }
                }
            }
        }
    }

    // Validate that the inserted row matches the table schema
    private boolean validateRow(Map<String, String> rowData) {
        return rowData.keySet().containsAll(columns); // Ensure all columns are present
    }

    // Print all rows for debugging
    public void printTable() {
        System.out.println("Table: " + name);
        for (Row row : data) {
            System.out.println(row);
        }
    }
}
