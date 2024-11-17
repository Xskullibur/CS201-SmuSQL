package edu.smu.smusql.skipHash;

class Indexing implements Comparable<Indexing> {
    private Object columnValue;
    private String primaryKey;
    private DataType dataType; // Store the data type

    public Indexing(Object columnValue, String primaryKey, DataType dataType) {
        this.columnValue = columnValue;
        this.primaryKey = primaryKey;
        this.dataType = dataType;
    }

    // Getter for columnValue
    public Object getColumnValue() {
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
        if (this.dataType != other.dataType) {
            throw new IllegalArgumentException("Cannot compare different data types");
        }

        if (this.columnValue == null && other.columnValue == null) {
            return 0;
        } else if (this.columnValue == null) {
            return -1; // Consider null less than any columnValue
        } else if (other.columnValue == null) {
            return 1;
        }

        switch (this.dataType) {
            case INTEGER:
                return Integer.compare((Integer) this.columnValue, (Integer) other.columnValue);
            case DOUBLE:
                return Double.compare((Double) this.columnValue, (Double) other.columnValue);
            case STRING:
                return ((String) this.columnValue).compareTo((String) other.columnValue);
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }

    // Overriding the equals method to ensure equality checks based on just
    // columnValue, is less strict to allow duplicates
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
