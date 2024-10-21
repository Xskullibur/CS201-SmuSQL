package edu.smu.smusql;

import java.util.List;

public abstract class AbstractTable<T> {
    
    private List<String> columns;
    private T rows;

    public AbstractTable(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public T getRows() {
        return rows;
    }

    public void setRows(T rows) {
        this.rows = rows;
    }

}
