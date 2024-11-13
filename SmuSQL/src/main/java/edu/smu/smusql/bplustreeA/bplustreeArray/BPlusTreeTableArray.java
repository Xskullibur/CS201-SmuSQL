package edu.smu.smusql.bplustreeA.bplustreeArray;

import edu.smu.smusql.AbstractTable;
import edu.smu.smusql.Constants;
import edu.smu.smusql.bplustreeA.BPlusTree;
import java.util.List;

public class BPlusTreeTableArray extends AbstractTable<BPlusTree<Integer, Object[]>> {
    private final int columnCount;

    public BPlusTreeTableArray(List<String> columns) {
        super(columns);
        this.columnCount = columns.size();
        setRows(new BPlusTree<>(Constants.B_PLUS_TREE_ORDER));
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getColumnIndex(String columnName) {
        return getColumns().indexOf(columnName);
    }
}