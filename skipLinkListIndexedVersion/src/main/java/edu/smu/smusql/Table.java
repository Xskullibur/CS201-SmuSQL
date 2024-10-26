package edu.smu.smusql;

import java.util.*;

public class Table<K1, K2, V> {
    private String name;
    private Map<K1, Map<K2, V>> tableData; // Double-indexed HashMap for storing data
    private Set<K2> columns;

    public Table(String name, Set<K2> columns) {
        this.name = name;
        this.tableData = new HashMap<>();
        this.columns = columns;
    }

    // Retrieve the table name
    public String getName() {
        return name;
    }

    // Retrieve the list of columns
    public Set<K2> getColumns() {
        return columns;
    }

    // Retrieve the entire data structure
    public Map<K1, Map<K2, V>> getData() {
        return tableData;
    }

    // Create a secondary index on a specified column (example implementation, expand as needed)
    public void createSecondaryIndex(K2 column) {
        // Secondary indexing logic can be added here, depending on requirements
        System.out.println("Secondary index created on column: " + column);
    }

    // Insert a new row of data
    public void insertRow(K1 key1, Map<K2, V> rowData) {
        if (validateRow(rowData)) {
            tableData.put(key1, new HashMap<>(rowData));
        } else {
            System.out.println("Row validation failed.");
        }
    }

    // Update an existing row of data
    public void updateRow(K1 key1, Map<K2, V> rowData) {
        if (tableData.containsKey(key1) && validateRow(rowData)) {
            tableData.put(key1, new HashMap<>(rowData));
        } else {
            System.out.println("Row update failed. Either row does not exist or validation failed.");
        }
    }

    // Delete a row
    public void deleteRow(K1 key1) {
        if (tableData.remove(key1) == null) {
            System.out.println("Row not found for deletion.");
        }
    }

    // Retrieve a specific row
    public Map<K2, V> getRow(K1 key1) {
        return tableData.get(key1);
    }

    public List<K1> returnKeysByRequirementOnId(K2 columnKey, V value, String operator) {
        List<K1> result = new ArrayList<>();
    
        for (Map.Entry<K1, Map<K2, V>> row : tableData.entrySet()) {
            V cellValue = row.getValue().get(columnKey);
            if (cellValue != null && meetsCondition(cellValue, value, operator)) {
                result.add(row.getKey());
            }
        }
    
        return result;
    }
    
    private boolean meetsCondition(V cellValue, V targetValue, String operator) {
        if (!(cellValue instanceof Comparable && targetValue instanceof Comparable)) {
            throw new IllegalArgumentException("Values must be comparable for this operation.");
        }
    
        @SuppressWarnings("unchecked")
        Comparable<V> comparableCellValue = (Comparable<V>) cellValue;
    
        switch (operator) {
            case ">":
                return comparableCellValue.compareTo(targetValue) > 0;
            case ">=":
                return comparableCellValue.compareTo(targetValue) >= 0;
            case "<":
                return comparableCellValue.compareTo(targetValue) < 0;
            case "<=":
                return comparableCellValue.compareTo(targetValue) <= 0;
            case "=":
                return comparableCellValue.compareTo(targetValue) == 0;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
    
    

    public List<K1> returnKeysByRequirementsOnIndex(Map<K2, Map.Entry<V, String>> requirements) {
        List<K1> result = new ArrayList<>();
    
        outerLoop:
        for (Map.Entry<K1, Map<K2, V>> row : tableData.entrySet()) {
            for (Map.Entry<K2, Map.Entry<V, String>> condition : requirements.entrySet()) {
                K2 columnKey = condition.getKey();
                V targetValue = condition.getValue().getKey();
                String operator = condition.getValue().getValue();
    
                V cellValue = row.getValue().get(columnKey);
    
                // If cell value doesn't meet the condition, skip to the next row
                if (cellValue == null || !meetsCondition(cellValue, targetValue, operator)) {
                    continue outerLoop;
                }
            }
            result.add(row.getKey());  // Only add primary key if all conditions are met
        }
    
        return result;
    }
    

    // Validate row data to ensure it has all required columns
    public boolean validateRow(Map<K2, V> rowData) {
        return columns.equals(rowData.keySet());
    }

    // Print the table data
    public void printTable() {
        System.out.println("Table: " + name);
        for (Map.Entry<K1, Map<K2, V>> entry : tableData.entrySet()) {
            System.out.println("Row ID: " + entry.getKey() + " -> " + entry.getValue());
        }
    }
}

//public class Table {


    // private String name; // Table name
    // private List<String> columns; // List of column names
    // private int numCols;
    // private SkipList<Row> data; // Primary SkipList to store rows based on the primary key (id)
    // private Map<String, SkipList<Indexing>> secondaryIndices; // Secondary indices: column name -> SkipList of Indexing

    // private static final String STRING_INT_MAX = Integer.toString(Integer.MAX_VALUE);
    // private static final String STRING_INT_MIN = Integer.toString(Integer.MIN_VALUE);

    // public Table(String name, List<String> columns) {
    //     this.name = name;
    //     this.columns = columns;
    //     this.data = new SkipList<>();
    //     this.secondaryIndices = new HashMap<>();
    //     numCols = columns.size();
    //     // Automatically creates secondary index for all columns
    //     for(int i= 1; i < columns.size(); i++){
    //         createSecondaryIndex(columns.get(i));
    //     }
    // }

    // public String getName() {
    //     return name;
    // }

    // public List<String> getColumns() {
    //     return columns;
    // }

    // public SkipList<Row> getData() {
    //     return data;
    // }

    // // Create a secondary index for a specific column
    // public void createSecondaryIndex(String column) {
    //     if (!columns.contains(column)) {
    //         throw new IllegalArgumentException("Column " + column + " does not exist in the table");
    //     }
    //     secondaryIndices.put(column, new SkipList<>()); // Create a new SkipList for the column
    // }

    // // Insert a new row into the table
    // public void insertRow(String id, Map<String, String> rowData) {
    //     // Validate that the row data matches the table schema
    //     if (!validateRow(rowData)) {
    //         throw new IllegalArgumentException("Row does not match table schema");
    //     }

    //     // Insert the new row into the primary skip list (indexed by ID)
    //     Row row = new Row(id, rowData);
    //     data.insert(row); // Assuming SkipList has an insert() method

    //     // Update all secondary indices (only for columns that have secondary indices)
    //     for (String column : columns.subList(1, numCols)) {
    //         String columnValue = rowData.get(column);
    //         if (columnValue != null) {
    //             Indexing indexEntry = new Indexing(columnValue, id);
    //             secondaryIndices.get(column).insert(indexEntry); // Add the index entry
    //         }
    //     }
    // }

    // // Update an existing row based on its ID
    // public void updateRow(String id, Map<String, String> newData) {
    //     Row row = data.search(new Row(id, null)); // Find the row by its ID (primary key)

    //     if (row != null) {
    //         // Remove old values from secondary indices
    //         for (String column : secondaryIndices.keySet()) {
    //             String oldColumnValue = row.getData().get(column);
    //             if (oldColumnValue != null) {
    //                 Indexing oldIndexEntry = new Indexing(oldColumnValue, id);
    //                 secondaryIndices.get(column).delete(oldIndexEntry); // Remove old index entry
    //             }
    //         }

    //         // Update the row's data in the primary skip list
    //         for (Map.Entry<String, String> entry : newData.entrySet()) {
    //             if (columns.contains(entry.getKey())) {
    //                 Map<String, String> rowData = row.getData();
    //                 rowData.put(entry.getKey(), entry.getValue());
    //                 row.setData(rowData);
    //             }
    //         }

    //         // Insert new values into secondary indices
    //         for (String column : columns.subList(1, numCols)) {
    //             String newColumnValue = newData.get(column);
    //             if (newColumnValue != null) {
    //                 Indexing newIndexEntry = new Indexing(newColumnValue, id);
    //                 secondaryIndices.get(column).insert(newIndexEntry); // Add new index entry
    //             }
    //         }
    //     } else {
    //         throw new NoSuchElementException("Row with ID " + id + " not found");
    //     }
    // }

    // // Delete a row based on its ID
    // public void deleteRow(String id) {
    //     Row row = data.search(new Row(id, null)); // Find the row by its ID
    //     if (row != null) {
    //         // Remove the row from the primary skip list
    //         data.delete(row);

    //         // Remove the row from all secondary indices
    //         for (String column : secondaryIndices.keySet()) {
    //             String columnValue = row.getData().get(column);
    //             if (columnValue != null) {
    //                 Indexing indexEntry = new Indexing(columnValue, id);
    //                 secondaryIndices.get(column).delete(indexEntry); // Remove the index entry
    //             }
    //         }
    //     } else {
    //         throw new NoSuchElementException("Row with ID " + id + " not found");
    //     }
    // }

    // // Retrieve a row based on its ID
    // public Row getRow(String id) {
    //     return data.search(new Row(id, null)); // Assuming SkipList has a search() method
    // }



    // //in case anybody wants to do hilarous stuff like return > id
    // public List<String> returnKeysByRequirementsOnId(String operator, String id) {

    //     List<String> result = new ArrayList<>();
    //     if (operator.equals(">")) {
    //         result.addAll(data.getValuesGreater(new Row(id, null)).stream()
    //                 .map(x -> x.getId()).toList());
    //     } else if (operator.equals(">=")) {
    //         result.addAll(data.getValuesGreaterOrEquals(new Row(id, null)).stream()
    //         .map(x -> x.getId()).toList());

    //     } else if (operator.equals("<")) {
    //         result.addAll(data.getValuesLesser(new Row(id, null)).stream()
    //         .map(x -> x.getId()).toList());
    //     } else if (operator.equals("<=")) {
    //         result.addAll(data.getValuesLesserOrEquals(new Row(id, null)).stream()
    //         .map(x -> x.getId()).toList());
    //     } else if (operator.equals("=")) {
    //         result.addAll(data.getValuesEqual(new Row(id, null)).stream()
    //                 .map(x -> x.getId()).toList());
    //     }

    //     return result;
    // }

    // // Ask me if you don't understand this part
    // public List<String> returnKeysByRequirementsOnIndex(String column, String operator, String value) {
    //     List<String> result = new ArrayList<>();
    //     if (operator.equals(">")) {
    //         result.addAll(secondaryIndices.get(column).getValuesGreater(new Indexing(value, STRING_INT_MAX)).stream()
    //                 .map(x -> x.getPrimaryKey()).toList());
    //     } else if (operator.equals(">=")) {
    //         result.addAll(
    //                 secondaryIndices.get(column).getValuesGreaterOrEquals(new Indexing(value, STRING_INT_MIN)).stream()
    //                         .map(x -> x.getPrimaryKey()).toList());

    //     } else if (operator.equals("<")) {
    //         result.addAll(secondaryIndices.get(column).getValuesLesser(new Indexing(value, STRING_INT_MIN)).stream()
    //                 .map(x -> x.getPrimaryKey()).toList());
    //     } else if (operator.equals("<=")) {
    //         result.addAll(
    //                 secondaryIndices.get(column).getValuesLesserOrEquals(new Indexing(value, STRING_INT_MAX)).stream()
    //                         .map(x -> x.getPrimaryKey()).toList());
    //     } else if (operator.equals("=")) {
    //         result.addAll(secondaryIndices.get(column).getValuesEqual(new Indexing(value, STRING_INT_MIN)).stream()
    //                 .map(x -> x.getPrimaryKey()).toList());
    //     }

    //     return result;
    // }

    // // Validate that the inserted row matches the table schema
    // private boolean validateRow(Map<String, String> rowData) {
    //     for(int i = 1; i < columns.size(); i++){
    //         if(!rowData.keySet().contains(columns.get(i))){
    //             return false;
    //         }
    //     }
    //     return true; // Ensure all columns are present
    // }

    // // Print all rows for debugging
    // public void printTable() {
    //     System.out.println("Table: " + name);
    //     for (Row row : data) {
    //         System.out.println(row);
    //     }
    // }
//}
