package edu.smu.smusql;

import java.util.*;
import java.util.stream.Collectors;

public class Table {
    private String name; // Table name
    private List<String> columns; // List of column names
    private Map<String, Row> data; // HashMap of Rows indexed by ID
    private Map<String, SkipList<Indexing>> secondaryIndices; // Secondary indices: column name -> SkipList of Indexing

    private static final String STRING_INT_MAX = Integer.toString(Integer.MAX_VALUE);
    private static final String STRING_INT_MIN = Integer.toString(Integer.MIN_VALUE);

    public Table() {

    }

    public Table(String name, List<String> columns) {
        this.name = name;
        this.columns = columns;
        this.data = new HashMap<>();
        this.secondaryIndices = new HashMap<>();
        // Automatically create secondary indices for all columns except the primary key
        for (String column : columns) {
            createSecondaryIndex(column);
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public Map<String, Row> getData() {
        return data;
    }

    public void setSecondaryIndices(HashMap<String, SkipList<Indexing>> indices) {
        this.secondaryIndices = indices;
    }

    // Create a secondary index for a specific column
    public void createSecondaryIndex(String column) {
        if (!columns.contains(column)) {
            throw new IllegalArgumentException("Column " + column + " does not exist in the table");
        }
        secondaryIndices.put(column, new SkipList<>()); // Create a new SkipList for the column
    }

    // Return keys by requirements on a column using secondary indices
    public List<String> returnKeysByRequirementsOnIndex(String column, String operator, String value) {
        List<String> result = new ArrayList<>();

        // Check if the column has a secondary index
        if (!secondaryIndices.containsKey(column)) {
            throw new IllegalArgumentException("No secondary index found for column: " + column);
        }

        SkipList<Indexing> indexList = secondaryIndices.get(column);

        switch (operator) {
            case ">":
                result.addAll(indexList.getValuesGreater(new Indexing(value, STRING_INT_MAX)).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case ">=":
                result.addAll(indexList.getValuesGreaterOrEquals(new Indexing(value, STRING_INT_MIN)).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "<":
                result.addAll(indexList.getValuesLesser(new Indexing(value, STRING_INT_MIN)).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "<=":
                result.addAll(indexList.getValuesLesserOrEquals(new Indexing(value, STRING_INT_MAX)).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "=":
                result.addAll(indexList.getValuesEqual(new Indexing(value, STRING_INT_MIN)).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "!=":
                Set<String> allKeys = indexList.getAllValues().stream()
                        .map(Indexing::getPrimaryKey)
                        .collect(Collectors.toCollection(TreeSet::new)); // Collect into a sorted set

                List<String> equalKeys = indexList.getValuesEqual(new Indexing(value, STRING_INT_MIN)).stream()
                        .map(Indexing::getPrimaryKey)
                        .toList();

                allKeys.removeAll(equalKeys); // Remove matching keys

                result.addAll(allKeys); // Add the sorted keys to the result
                break;

            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }

        return result;
    }

    // Insert a new row into the table
    public void insertRow(String id, Map<String, String> rowData) {
        // Validate that the row data matches the table schema
        if (!validateRow(rowData)) {
            throw new IllegalArgumentException("Row does not match table schema");
        }

        // Check if the row ID already exists and remove it if it does (to replace)
        if (data.containsKey(id)) {
            throw new RuntimeException("0 row inserted, primary key already exists"); // Remove the old row before
                                                                                      // inserting the new one
        }

        // Insert the new row into the primary HashMap
        Row row = new Row(id, rowData);
        data.put(id, row);

        // Update all secondary indices (only for columns that have secondary indices)
        for (String column : columns) {
            String columnValue = rowData.get(column);
            if (columnValue != null) {
                Indexing indexEntry = new Indexing(columnValue, id);
                secondaryIndices.get(column).insert(indexEntry); // Add the index entry
            }
        }
    }

    // Update an existing row based on its ID
    public void updateRow(String id, Map<String, String> newData) {
        Row existingRow = data.get(id); // Find the row by its ID

        if (existingRow != null) {
            // Remove old values from secondary indices
            for (String column : secondaryIndices.keySet()) {
                String oldColumnValue = existingRow.getData().get(column);
                if (oldColumnValue != null) {
                    Indexing oldIndexEntry = new Indexing(oldColumnValue, id);
                    secondaryIndices.get(column).delete(oldIndexEntry); // Remove old index entry
                }
            }

            // Update the row's data
            for (Map.Entry<String, String> entry : newData.entrySet()) {
                if (columns.contains(entry.getKey())) {
                    existingRow.getData().put(entry.getKey(), entry.getValue());
                }
            }

            // Update the row in the HashMap
            data.put(id, existingRow);

            // Insert new values into secondary indices
            for (String column : columns) {
                String newColumnValue = newData.get(column);
                if (newColumnValue != null) {
                    Indexing newIndexEntry = new Indexing(newColumnValue, id);
                    secondaryIndices.get(column).insert(newIndexEntry); // Add new index entry
                }
            }
        } else {
            throw new NoSuchElementException("Row with ID " + id + " not found");
        }
    }

    // Delete a row based on its ID
    public void deleteRow(String id) {
        Row row = data.get(id); // Find the row by its ID
        if (row != null) {
            // Remove the row from the primary HashMap
            data.remove(id);

            // Remove the row from all secondary indices
            for (String column : secondaryIndices.keySet()) {
                String columnValue = row.getData().get(column);
                if (columnValue != null) {
                    Indexing indexEntry = new Indexing(columnValue, id);
                    secondaryIndices.get(column).delete(indexEntry); // Remove the index entry
                }
            }
        } else {
            throw new NoSuchElementException("Row with ID " + id + " not found");
        }
    }

    // Retrieve a row based on its ID
    public Row getRow(String id) {
        return data.get(id); // Retrieve directly from HashMap
    }

    // Validate that the inserted row matches the table schema
    private boolean validateRow(Map<String, String> rowData) {
        for (int i = 1; i < columns.size(); i++) {
            if (!rowData.keySet().contains(columns.get(i))) {
                return false;
            }
        }
        return true; // Ensure all columns are present
    }

    // Print all rows for debugging
    public void printTable() {
        System.out.println("Table: " + name);
        for (Row row : data.values()) { // Iterate over HashMap values
            System.out.println(row);
        }
    }
}
