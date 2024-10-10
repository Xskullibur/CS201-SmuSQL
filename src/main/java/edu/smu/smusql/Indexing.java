package edu.smu.smusql;

public class Indexing implements Comparable<Indexing> {
    private String columnValue; // The value of the column being indexed
    private String primaryKey;  // The primary key (ID) of the original row

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

    // Overriding the compareTo method to compare Indexing objects based on columnValue, and then primaryKey
    @Override
    public int compareTo(Indexing other) {
        // First compare by column value
        int comparisonResult = this.columnValue.compareTo(other.columnValue);
        
        // If the column values are the same, compare by primary key (for tie-breaking)
        if (comparisonResult == 0) {
            return this.primaryKey.compareTo(other.primaryKey);
        }
        
        return comparisonResult;
    }

    // Overriding the equals method to ensure equality checks based on both columnValue and primaryKey
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Indexing indexing = (Indexing) obj;

        if (!columnValue.equals(indexing.columnValue)) return false;
        return primaryKey.equals(indexing.primaryKey);
    }

    // Overriding toString to print the Indexing object in a readable format
    @Override
    public String toString() {
        return "Indexing{columnValue='" + columnValue + "', primaryKey='" + primaryKey + "'}";
    }
}
