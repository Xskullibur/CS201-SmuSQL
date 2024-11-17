package edu.smu.smusql.bplustreeA.bplustreeArray;

import edu.smu.smusql.AbstractTable;
import edu.smu.smusql.Constants;
import edu.smu.smusql.bplustreeA.BPlusTree;
import edu.smu.smusql.bplustreeA.BPlusTreeMultiRange;
import java.util.List;

public class BPlusTree_MultiRange_TableArray extends AbstractTable<BPlusTreeMultiRange<Integer, Object[]>> {
    private final int columnCount;

    public BPlusTree_MultiRange_TableArray(List<String> columns) {
        super(columns);
        this.columnCount = columns.size();
        setRows(new BPlusTreeMultiRange<>(Constants.B_PLUS_TREE_ORDER));
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getColumnIndex(String columnName) {
        return getColumns().indexOf(columnName);
    }
}