package edu.smu.smusql.bplustreeA;

import edu.smu.smusql.AbstractTable;
import edu.smu.smusql.Constants;
import java.util.List;

public class BPlusTreeTableArray extends AbstractTable<BPlusTree<Integer, Object[]>> {

    public BPlusTreeTableArray(List<String> columns) {
        super(columns);
        setRows(new BPlusTree<>(Constants.B_PLUS_TREE_ORDER));
    }

}
