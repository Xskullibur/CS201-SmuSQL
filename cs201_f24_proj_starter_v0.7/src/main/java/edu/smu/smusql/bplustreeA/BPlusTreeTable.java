package edu.smu.smusql.bplustreeA;

import java.util.List;
import java.util.Map;

import edu.smu.smusql.AbstractTable;
import edu.smu.smusql.Constants;

public class BPlusTreeTable extends AbstractTable<BPlusTree<Integer, Map<String, Object>>> {

    public BPlusTreeTable(List<String> columns) {
        super(columns);
        setRows(new BPlusTree<>(Constants.B_PLUS_TREE_ORDER));
    }

}
