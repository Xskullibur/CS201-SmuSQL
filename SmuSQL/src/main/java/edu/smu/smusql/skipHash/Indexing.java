package edu.smu.smusql.skipHash;

public class Indexing implements Comparable<Indexing> {
    private String columnValue; // The value of the column being indexed
    private String primaryKey; // The primary key (ID) of the original row

    // Constructor to initialize the Indexing object
    public Indexing(String columnValue, String primaryKey) {
        this.columnValue = columnValue;
        this.primaryKey = primaryKey;
    }

    // Getter for columnValue
    public String getColumnValue() {
        return columnValue;
    }

    // Setter for columnValue
    public void setColumnValue(String columnValue) {
        this.columnValue = columnValue;
    }

    // Getter for primaryKey
    public String getPrimaryKey() {
        return primaryKey;
    }

    // Setter for primaryKey
    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    // Overriding the compareTo method to compare Indexing objects based on
    // columnValue, and then primaryKey
    @Override
    public int compareTo(Indexing other) {
        // Try to compare columnValue as numbers
        int comparisonResult = 0;
        if(this.columnValue.contains(".") || other.columnValue.contains(".")){
            comparisonResult = compareAsDoubles(this.columnValue, other.columnValue);
        } else {
            comparisonResult = compareAsInteger(columnValue, other.columnValue);
        }

        return comparisonResult;
    }

    // Helper method to compare two values as numbers, falling back to string
    // comparison, used for eevrything but getValuesEqual.

    private int compareAsInteger(String value1, String value2){
        try {
            // Try to parse both values as integers
            Integer num1 = Integer.parseInt(value1);
            Integer num2 = Integer.parseInt(value2);

            // Compare as numbers if both values are numeric
            return num1.compareTo(num2);
        } catch (NumberFormatException e){
            return value1.compareTo(value2);
        }
    }
    private int compareAsDoubles(String value1, String value2) {
        try {
            // Try to parse both values as integers
            Double num1 = Double.parseDouble(value1);
            Double num2 = Double.parseDouble(value2);

            // Compare as numbers if both values are numeric
            return num1.compareTo(num2);
        } catch (NumberFormatException e) {
            // If either value is not numeric, compare as strings
            return value1.compareTo(value2);
        }
    }

    // Overriding the equals method to ensure equality checks based on just columnValue, is less strict to allow duplicates
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Indexing indexing = (Indexing) obj;

        return columnValue.equals(indexing.columnValue);
    }

    // Overriding toString to print the Indexing object in a readable format
    @Override
    public String toString() {
        return "Indexing{columnValue='" + columnValue + "', primaryKey='" + primaryKey + "'}";
    }
}
