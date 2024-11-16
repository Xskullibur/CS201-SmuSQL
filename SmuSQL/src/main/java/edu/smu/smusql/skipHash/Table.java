package edu.smu.smusql.skipHash;

import java.util.*;
import java.util.stream.Collectors;

public class Table {
    private String name; // Table name
    private List<ColumnSchema> columns = new ArrayList<>(); // List of column schemas
    private Map<String, Row> data; // HashMap of Rows indexed by ID
    private Map<String, SkipList<Indexing>> secondaryIndices; // Secondary indices: column name -> SkipList of Indexing

    public Table() {

    }

    public Table(String name, List<String> columns) {
        this.name = name;
        for (String columnName : columns) {
            this.columns.add(new ColumnSchema(columnName));
        }
        this.data = new HashMap<>();
        this.secondaryIndices = new HashMap<>();
        // Automatically create secondary indices for all columns except the primary key
        
    }

    public String getName() {
        return name;
    }

    public List<String> getColumnNames() {
        return columns.stream().map(x -> x.getName()).toList();
    }

    public Map<String, Row> getData() {
        return data;
    }

    private ColumnSchema getColumnSchema(String colName){
        for(ColumnSchema c : columns){
            if(c.getName().equals(colName)){
                return c;
            }
        }
        return null;
    }

    private void initializeSecondaryIndices() {
        // Create a secondary index for each column
        for (ColumnSchema column : columns) {
            String columnName = column.getName();
            // Create a new SkipList for the column
            SkipList<Indexing> skipList = new SkipList<>();
            secondaryIndices.put(columnName, skipList);
        }
    
        // Insert existing data into the secondary indices
        for (Row row : data.values()) {
            String id = row.getId();
            Map<String, Object> rowData = row.getData();
            for (ColumnSchema column : columns) {
                String columnName = column.getName();
                DataType dataType = column.getType();
                Object columnValue = rowData.get(columnName);
                if (columnValue != null) {
                    Indexing indexEntry = new Indexing(columnValue, id, dataType);
                    secondaryIndices.get(columnName).insert(indexEntry);
                }
            }
        }
    }
    

    public void setSecondaryIndices(HashMap<String, SkipList<Indexing>> indices) {
        this.secondaryIndices = indices;
    }

    private boolean validateDataTypes(Map<String, String> rowData) {
        for (ColumnSchema column : columns) {
            String columnName = column.getName();
            DataType dataType = column.getType();
            String valueStr = rowData.get(columnName);
            if (valueStr == null || valueStr.equalsIgnoreCase("null")) {
                continue; // Accept null values
            }
            try {
                parseValue(valueStr, dataType);
                return true;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid data type for column " + columnName + ": expected " + dataType);
            }
        }
        return false;
    }

    private boolean isDataTypesUnknown() {
        for (ColumnSchema column : columns) {
            if (column.getType() != DataType.UNKNOWN) {
                return false;
            }
        }
        return true;
    }

    private void inferDataTypes(Map<String, String> rowData) {
        for (ColumnSchema column : columns) {
            String columnName = column.getName();
            String valueStr = rowData.get(columnName);
            DataType inferredType = inferDataTypeFromValue(valueStr);
            column.setType(inferredType);
        }
    }

    private DataType inferDataTypeFromValue(String value) {
        if (value == null || value.equalsIgnoreCase("null")) {
            return DataType.STRING; // Default to STRING for null values
        }
        // Try to parse as INTEGER
        try {
            Integer.parseInt(value);
            return DataType.INTEGER;
        } catch (NumberFormatException ignored) {
        }
        // Try to parse as DOUBLE
        try {
            Double.parseDouble(value);
            return DataType.DOUBLE;
        } catch (NumberFormatException ignored) {
        }
        // Default to STRING
        return DataType.STRING;
    }

    private Object parseValue(String value, DataType dataType) {
        switch (dataType) {
            case INTEGER:
                return Integer.parseInt(value);
            case DOUBLE:
                return Double.parseDouble(value);
            case STRING:
                return value;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }
   

    public List<String> returnKeysByRequirementsOnIndex(String column, String operator, String valueStr) {
        List<String> result = new ArrayList<>();

        // Check if the column has a secondary index
        if (!secondaryIndices.containsKey(column)) {
            throw new IllegalArgumentException("No secondary index found for column: " + column);
        }

        ColumnSchema columnSchema = getColumnSchema(column);
        DataType dataType = columnSchema.getType();
        Object value = parseValue(valueStr, dataType);

        SkipList<Indexing> indexList = secondaryIndices.get(column);

        Indexing indexValue = new Indexing(value, "", dataType);

        switch (operator) {
            case ">":
                result.addAll(indexList.getValuesGreater(indexValue).stream()
                        .map(Indexing::getPrimaryKey).collect(Collectors.toList()));
                break;
            case ">=":
                result.addAll(indexList.getValuesGreaterOrEquals(indexValue).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "<":
                result.addAll(indexList.getValuesLesser(indexValue).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "<=":
                result.addAll(indexList.getValuesLesserOrEquals(indexValue).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "=":
                result.addAll(indexList.getValuesEqual(indexValue).stream()
                        .map(Indexing::getPrimaryKey).toList());
                break;
            case "!=":
                List<String> lessThanKeys = indexList.getValuesLesser(indexValue).stream()
                        .map(Indexing::getPrimaryKey).collect(Collectors.toList());

                List<String> greaterThanKeys = indexList.getValuesGreater(indexValue).stream()
                        .map(Indexing::getPrimaryKey).collect(Collectors.toList());

                result.addAll(lessThanKeys);
                result.addAll(greaterThanKeys);
                break;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }

        return result;
    }

    // Insert a new row into the table
    public void insertRow(String id, Map<String, String> rowData) {
        // Validate that the row data matches the table schema
        if (isDataTypesUnknown()) {
            inferDataTypes(rowData);
            initializeSecondaryIndices(); // Initialize indices after data types are known

        }

        if (!validateDataTypes(rowData)) {
            throw new IllegalArgumentException("Row does not match table schema");
        }

        // Parse and store data with correct types
        Map<String, Object> parsedData = new HashMap<>();
        for (ColumnSchema column : columns) {
            String columnName = column.getName();
            DataType dataType = column.getType();
            String valueStr = rowData.get(columnName);
            Object value = parseValue(valueStr, dataType);
            parsedData.put(columnName, value);
        }

        // Insert the new row into the primary HashMap
        Row row = new Row(id, parsedData);
        data.put(id, row);

        // Update all secondary indices
        for (ColumnSchema column : columns) {
            String columnName = column.getName();
            Object columnValue = parsedData.get(columnName);
            if (columnValue != null) {
                Indexing indexEntry = new Indexing(columnValue, id, column.getType());
                secondaryIndices.get(columnName).insert(indexEntry);
            }
        }
    }

    public void updateRow(String id, Map<String, String> newData) {
        Row existingRow = data.get(id);
    
        if (existingRow != null) {
            // Remove old values from secondary indices
            for (ColumnSchema column : columns) {
                String columnName = column.getName();
                DataType dataType = column.getType();
                Object oldValue = existingRow.getData().get(columnName);
                if (oldValue != null) {
                    Indexing oldIndexEntry = new Indexing(oldValue, id, dataType);
                    secondaryIndices.get(columnName).delete(oldIndexEntry);
                }
            }
    
            // Parse and validate new data
            Map<String, Object> parsedData = new HashMap<>();
            for (ColumnSchema column : columns) {
                String columnName = column.getName();
                DataType dataType = column.getType();
                String valueStr = newData.get(columnName);
                Object value = parseValue(valueStr, dataType);
                parsedData.put(columnName, value);
            }
    
            // Update the row's data
            existingRow.setData(parsedData);
            data.put(id, existingRow);
    
            // Insert new values into secondary indices
            for (ColumnSchema column : columns) {
                String columnName = column.getName();
                DataType dataType = column.getType();
                Object newValue = parsedData.get(columnName);
                if (newValue != null) {
                    Indexing newIndexEntry = new Indexing(newValue, id, dataType);
                    secondaryIndices.get(columnName).insert(newIndexEntry);
                }
            }
        } else {
            throw new NoSuchElementException("Row with ID " + id + " not found");
        }
    }
    

    public void deleteRow(String id) {
        Row row = data.get(id);
        if (row != null) {
            // Remove from primary data
            data.remove(id);
    
            // Remove from secondary indices
            for (ColumnSchema column : columns) {
                String columnName = column.getName();
                DataType dataType = column.getType();
                Object columnValue = row.getData().get(columnName);
                if (columnValue != null) {
                    Indexing indexEntry = new Indexing(columnValue, id, dataType);
                    secondaryIndices.get(columnName).delete(indexEntry);
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


    // Print all rows for debugging
    public void printTable() {
        System.out.println("Table: " + name);
        for (Row row : data.values()) { // Iterate over HashMap values
            System.out.println(row);
        }
    }
}
