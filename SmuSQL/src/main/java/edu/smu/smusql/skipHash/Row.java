package edu.smu.smusql.skipHash;

import java.util.Map;
import java.util.Objects;


class Row implements Comparable<Row> {
    private String id;
    private Map<String, Object> data; // The actual row data mapped by column name (stored as String values)

    public Row(String id, Map<String, Object> data) {
        this.id = id;
        this.data = data;
    }

    public String getId(){
        return id;
    }
    
    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Self-check
        if (obj == null || getClass() != obj.getClass()) return false; // Type-check

        Row otherRow = (Row) obj;
        return Objects.equals(this.id, otherRow.id); // Compare by id
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // Generate hash code based on the ID field
    }

    @Override
    public int compareTo(Row other) {
        return compareAsNumbers(this.id, other.id);
    }

    private int compareAsNumbers(String value1, String value2) {
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


    @Override
    public String toString() {
        return "Row{id=" + id + ", data=" + data + "}";
    }
}
