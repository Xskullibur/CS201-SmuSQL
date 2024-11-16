package edu.smu.smusql.skipHash;


class ColumnSchema {
    private String name;
    private DataType type; // An enum representing the data type

    public ColumnSchema(String name){
        this.name = name;
        this.type = DataType.UNKNOWN;
    }

    public ColumnSchema(String name, DataType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

}
