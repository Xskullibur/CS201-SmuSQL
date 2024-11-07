package edu.smu.smusql.bplustreeA.lruCache;

import edu.smu.smusql.bplustreeA.AstParser.nodes.ConditionNode;
import java.util.List;
import java.util.Objects;

public class CacheQueryKey {

    public final String tableName;
    private final ConditionNode whereClause;
    private final List<String> columns;

    public CacheQueryKey(String tableName, ConditionNode whereClause, List<String> columns) {
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.columns = columns;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, whereClause, columns);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CacheQueryKey queryKey = (CacheQueryKey) o;

        return Objects.equals(tableName, queryKey.tableName) && Objects.equals(whereClause,
            queryKey.whereClause) && Objects.equals(columns, queryKey.columns);
    }
}
