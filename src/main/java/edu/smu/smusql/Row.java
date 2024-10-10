package edu.smu.smusql;

import java.util.Map;
import java.util.Objects;


class Row implements Comparable<Row> {
    String id;
    private Map<String, String> data; // The actual row data mapped by column name (stored as String values)

    public Row(String id, Map<String, String> data) {
        this.id = id;
        this.data = data;
    }

    public String getId(){
        return id;
    }
    
    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
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
        return this.id.compareTo(other.id); // Compare by ID
    }

    @Override
    public String toString() {
        return "Row{id=" + id + ", data=" + data + "}";
    }
}
