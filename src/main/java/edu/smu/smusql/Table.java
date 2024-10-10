import java.util.*;

public class Table {
    String name; // Table name
    List<String> columns; // List of column names
    SkipList<Row> data; // Primary SkipList to store rows based on the primary key (id)
    Map<String, SkipList<Indexing>> secondaryIndices; // Secondary indices: column name -> SkipList of Indexing

    public Table(String name, List<String> columns) {
        this.name = name;
        this.columns = columns;
        this.data = new SkipList<>();
        this.secondaryIndices = new HashMap<>();

        //Automatically creates secondary index for all columns
        for(String col : columns){
            createSecondaryIndex(col);
        }
    }

    // Create a secondary index for a specific column
    public void createSecondaryIndex(String column) {
        if (!columns.contains(column)) {
            throw new IllegalArgumentException("Column " + column + " does not exist in the table");
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

        // Update all secondary indices (only for columns that have secondary indices)
        for (String column : columns) {
            String columnValue = rowData.get(column);
            if (columnValue != null) {
                Indexing indexEntry = new Indexing(columnValue, id);
                secondaryIndices.get(column).add(indexEntry); // Add the index entry
            }
        }
    }

    // Update an existing row based on its ID
    public void updateRow(String id, Map<String, String> newData) {
        Row row = data.search(id); // Find the row by its ID (primary key)

        if (row != null) {
            // Remove old values from secondary indices
            for (String column : secondaryIndices.keySet()) {
                String oldColumnValue = row.data.get(column);
                if (oldColumnValue != null) {
                    Indexing oldIndexEntry = new Indexing(oldColumnValue, id);
                    secondaryIndices.get(column).remove(oldIndexEntry); // Remove old index entry
                }
            }

            // Update the row's data in the primary skip list
            for (Map.Entry<String, String> entry : newData.entrySet()) {
                if (columns.contains(entry.getKey())) {
                    row.data.put(entry.getKey(), entry.getValue());
                }
            }

            // Insert new values into secondary indices
            for (String column : columns) {
                String newColumnValue = newData.get(column);
                if (newColumnValue != null) {
                    Indexing newIndexEntry = new Indexing(newColumnValue, id);
                    secondaryIndices.get(column).add(newIndexEntry); // Add new index entry
                }
            }
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
            for (String column : secondaryIndices.keySet()) {
                String columnValue = row.data.get(column);
                if (columnValue != null) {
                    Indexing indexEntry = new Indexing(columnValue, id);
                    secondaryIndices.get(column).remove(indexEntry); // Remove the index entry
                }
            }
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

        SkipList<Indexing> index = secondaryIndices.get(column);
        List<Row> result = new ArrayList<>();

        // Find all indexing entries for the given column value
        for (Indexing indexing : index) {
            if (indexing.getColumnValue().equals(value)) {
                Row row = data.search(indexing.getPrimaryKey());
                if (row != null) {
                    result.add(row);
                }
            }
        }

        return result;
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
